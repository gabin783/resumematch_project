package com.resumematch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.GapMatchRequest;
import com.resumematch.dto.GapMatchResponse;
import com.resumematch.dto.ResumeParseResponse;
import com.resumematch.entity.AnalysisResult;
import com.resumematch.entity.Resume;
import com.resumematch.repository.AnalysisResultRepository;
import com.resumematch.repository.ResumeRepository;
import com.resumematch.service.FileParsingService;
import com.resumematch.service.GapLlmAnalyzeService;
import com.resumematch.service.ResumeLlmAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeController {
    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final FileParsingService fileParsingService;
    private final ResumeLlmAnalyzeService resumeLlmAnalyzeService;
    private final GapLlmAnalyzeService gapLlmAnalyzeService;
    private final ResumeRepository resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;

    @PostMapping("/parse-resume")
    public ResponseEntity<?> parseResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            return ResponseEntity.badRequest().body("memberId가 필요합니다.");
        }

        try {
            String text = fileParsingService.extractText(file);
            ResumeParseResponse parseResponse = resumeLlmAnalyzeService.analyze(text);

            Resume savedResume = Resume.builder()
                    .memberId(memberId)
                    .originalFileName(file.getOriginalFilename())
                    .skills(String.join(",", parseResponse.getSkills()))
                    .build();

            resumeRepository.save(savedResume);
            return ResponseEntity.ok().body(parseResponse);
        } catch (Throwable e) {
            log.error("Resume parse failed", e);
            return ResponseEntity.internalServerError().body("이력서 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/gap-match")
    public ResponseEntity<?> analyzeGap(@RequestBody GapMatchRequest requestDto) {
        if (requestDto.getMemberId() == null || requestDto.getMemberId() <= 0) {
            return ResponseEntity.badRequest().body("memberId가 필요합니다.");
        }

        try {
            Long memberId = requestDto.getMemberId();
            Resume latestResume = resumeRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId);

            if (latestResume == null) {
                return ResponseEntity.badRequest().body("이력서 정보가 없습니다. 먼저 이력서를 업로드해주세요.");
            }

            List<String> resumeSkillsList = Arrays.asList(latestResume.getSkills().split(","));

            ObjectMapper objectMapper = new ObjectMapper();
            GapMatchResponse response = gapLlmAnalyzeService.analyze(resumeSkillsList, requestDto);

            AnalysisResult resultEntity = AnalysisResult.builder()
                    .memberId(memberId)
                    .targetJob(response.getTargetJob())
                    .jdText(requestDto.getJdText())
                    .analysis(toJson(objectMapper, response))
                    .learningDirection(response.getLearningDirection())
                    .missingSkills(joinSkillNames(response.getMissingSkills()))
                    .requiredSkills(toJson(objectMapper, response.getRequiredSkills()))
                    .preferredSkills(toJson(objectMapper, response.getPreferredSkills()))
                    .mainTasks(toJson(objectMapper, response.getMainTasks()))
                    .jobKeywords(toJson(objectMapper, response.getJobKeywords()))
                    .jobSummary(response.getJobSummary())
                    .jobAnalyzedAt(hasJobAnalysis(requestDto) ? LocalDateTime.now() : null)
                    .build();

            analysisResultRepository.save(resultEntity);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Gap match failed", e);
            return ResponseEntity.internalServerError().body("스킬 갭 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String toJson(ObjectMapper objectMapper, List<String> values) {
        if (values == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            log.warn("Failed to serialize list to JSON. Fallback to comma-separated text.", e);
            return String.join(",", values);
        }
    }

    private String toJson(ObjectMapper objectMapper, GapMatchResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Failed to serialize gap response to JSON. Fallback to analysis text.", e);
            return response.getAnalysis();
        }
    }

    private String joinSkillNames(List<com.resumematch.dto.SkillScoreDto> values) {
        if (values == null) {
            return "";
        }

        return values.stream()
                .map(com.resumematch.dto.SkillScoreDto::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.trim().isEmpty())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private boolean hasJobAnalysis(GapMatchRequest requestDto) {
        return (requestDto.getRequiredSkills() != null && !requestDto.getRequiredSkills().isEmpty())
                || (requestDto.getPreferredSkills() != null && !requestDto.getPreferredSkills().isEmpty())
                || (requestDto.getMainTasks() != null && !requestDto.getMainTasks().isEmpty())
                || (requestDto.getKeywords() != null && !requestDto.getKeywords().isEmpty())
                || (requestDto.getSummary() != null && !requestDto.getSummary().trim().isEmpty());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable Long id) {
        try {
            resumeRepository.deleteById(id);
            return ResponseEntity.ok().body("삭제 완료");
        } catch (Exception e) {
            log.error("Resume delete failed: id={}", id, e);
            return ResponseEntity.internalServerError().body("삭제 중 오류가 발생했습니다.");
        }
    }

    @DeleteMapping("/analysis/{id}")
    public ResponseEntity<?> deleteAnalysisResult(@PathVariable Long id) {
        try {
            analysisResultRepository.deleteById(id);
            return ResponseEntity.ok().body("분석 기록 삭제 완료");
        } catch (Exception e) {
            log.error("Analysis result delete failed: id={}", id, e);
            return ResponseEntity.internalServerError().body("삭제 중 오류가 발생했습니다.");
        }
    }
}
