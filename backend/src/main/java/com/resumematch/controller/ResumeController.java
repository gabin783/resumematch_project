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
import com.resumematch.service.OllamaAiService;
import com.resumematch.service.ResumeAnalyzerService;
import lombok.RequiredArgsConstructor;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeController {

    private final FileParsingService fileParsingService;
    private final ResumeAnalyzerService resumeAnalyzerService;
    private final OllamaAiService ollamaAiService;
    private final ResumeRepository resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "이력서", "지원", "분야", "성명", "이름", "회사", "직무", "학교", "학과", "목차",
            "생년월일", "전화", "메일", "주소", "성격", "소개", "내용", "주제", "선정", "정의",
            "개발", "환경", "기반", "효과", "기획", "용도", "목적", "관리", "운영", "경험",
            "사항", "고민", "능력", "사용", "불편", "고려", "일정", "데이터", "수집",
            "기술", "서비스", "파트", "스택", "과정", "프로젝트", "사이드", "구분", "작성"
    ));

    private static final List<String> MUST_HAVE_STACKS = Arrays.asList(
            "Java", "Spring", "Boot", "React", "MySQL", "Oracle", "Python", "Flask",
            "Docker", "Nginx", "AWS", "TypeScript", "Next", "JPA", "MyBatis", "Redis",
            "Kubernetes", "Git", "Figma", "GitHub", "Actions", "JavaScript", "SQL", "WebFlux"
    );

    @PostMapping("/parse-resume")
    public ResponseEntity<?> parseResume(@RequestParam("file") MultipartFile file) {
        try {
            String text = fileParsingService.extractText(file);
            System.out.println("================ [DEBUG: 1단계 원본 텍스트] ================");
            System.out.println(text.replaceAll("(?m)^\\s*\\r?\\n", ""));

            List<String> rawKeywords = resumeAnalyzerService.extractKeywords(text);
            List<String> cleanedKeywords = rawKeywords.stream()
                    .filter(word -> word.length() > 1)
                    .filter(word -> !STOP_WORDS.contains(word))
                    .distinct()
                    .collect(Collectors.toList());

            List<String> aiCandidates = cleanedKeywords.stream()
                    .filter(word -> word.matches(".*[a-zA-Z]+.*"))
                    .collect(Collectors.toList());

            String joinedKeywords = String.join(", ", aiCandidates);
            System.out.println("---------------- [DEBUG: 2.5단계 AI에게 줄 영어 키워드] ----------------");
            System.out.println(joinedKeywords);

            String prompt = "[목록]: " + joinedKeywords + "\n\n" +
                    "명령: 이 목록에서 프로그래밍 언어, 프레임워크, DB, 인프라 도구만 골라주세요.\n" +
                    "규칙 1: Blue, Green, List, Return 같은 일반 영어 단어는 제외하세요.\n" +
                    "규칙 2: 설명 없이 단어만 쉼표(,)로 나열하세요.\n" +
                    "결과: ";

            String filteredResult = ollamaAiService.callGemmaDirectly(prompt);

            List<String> finalSkills = Arrays.stream(filteredResult.replace("\n", ",").split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.length() < 20)
                    .collect(Collectors.toList());

            for (String core : MUST_HAVE_STACKS) {
                boolean isInOriginal = cleanedKeywords.stream().anyMatch(k -> k.equalsIgnoreCase(core));
                boolean isAlreadyInResult = finalSkills.stream().anyMatch(s -> s.equalsIgnoreCase(core));
                if (isInOriginal && !isAlreadyInResult) {
                    finalSkills.add(core);
                }
            }

            List<String> distinctSkills = finalSkills.stream()
                    .distinct()
                    .collect(Collectors.toList());

            System.out.println("---------------- [DEBUG: 3단계 최종 필터링 결과] ----------------");
            System.out.println(distinctSkills);
            System.out.println("==========================================================");

            Resume savedResume = Resume.builder()
                    .originalFileName(file.getOriginalFilename())
                    .skills(String.join(",", distinctSkills))
                    .build();

            resumeRepository.save(savedResume);
            System.out.println("DB 저장 완료! 저장된 이력서 ID: " + savedResume.getId());

            return ResponseEntity.ok().body(new ResumeParseResponse("success", distinctSkills.size(), distinctSkills));

        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("오류 발생: " + e.getMessage());
        }
    }

    @PostMapping("/gap-match")
    public ResponseEntity<?> analyzeGap(@RequestBody GapMatchRequest requestDto) {
        try {
            Resume latestResume = resumeRepository.findAll().stream()
                    .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                    .findFirst()
                    .orElse(null);

            if (latestResume == null) {
                return ResponseEntity.badRequest().body("이력서 정보가 없습니다. 먼저 이력서를 업로드해주세요.");
            }

            List<String> resumeSkillsList = Arrays.asList(latestResume.getSkills().split(","));

            String jsonResponse = ollamaAiService.analyzeGapWithGemma(
                    resumeSkillsList,
                    requestDto.getJdText(),
                    requestDto.getTargetJob()
            );

            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
            ObjectMapper objectMapper = new ObjectMapper();
            GapMatchResponse response = objectMapper.readValue(jsonResponse, GapMatchResponse.class);

            AnalysisResult resultEntity = AnalysisResult.builder()
                    .targetJob(requestDto.getTargetJob())
                    .jdText(requestDto.getJdText())
                    .analysis(response.getAnalysis())
                    .learningDirection(response.getLearningDirection())
                    .missingSkills(response.getMissingSkills() != null ? String.join(",", response.getMissingSkills()) : "")
                    .requiredSkills(toJson(objectMapper, requestDto.getRequiredSkills()))
                    .preferredSkills(toJson(objectMapper, requestDto.getPreferredSkills()))
                    .mainTasks(toJson(objectMapper, requestDto.getMainTasks()))
                    .jobKeywords(toJson(objectMapper, requestDto.getKeywords()))
                    .jobSummary(requestDto.getSummary())
                    .jobAnalyzedAt(hasJobAnalysis(requestDto) ? LocalDateTime.now() : null)
                    .build();

            analysisResultRepository.save(resultEntity);
            System.out.println("AI 스킬 갭 분석 결과 DB 저장 완료! ID: " + resultEntity.getId());

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("분석 오류: " + e.getMessage());
        }
    }

    private String toJson(ObjectMapper objectMapper, List<String> values) {
        if (values == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return String.join(",", values);
        }
    }

    private boolean hasJobAnalysis(GapMatchRequest requestDto) {
        return (requestDto.getRequiredSkills() != null && !requestDto.getRequiredSkills().isEmpty())
                || (requestDto.getPreferredSkills() != null && !requestDto.getPreferredSkills().isEmpty())
                || (requestDto.getMainTasks() != null && !requestDto.getMainTasks().isEmpty())
                || (requestDto.getKeywords() != null && !requestDto.getKeywords().isEmpty())
                || (requestDto.getSummary() != null && !requestDto.getSummary().trim().isEmpty());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable("id") Long id) {
        try {
            resumeRepository.deleteById(id);
            return ResponseEntity.ok().body("삭제 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("삭제 중 오류 발생");
        }
    }

    @DeleteMapping("/analysis/{id}")
    public ResponseEntity<?> deleteAnalysisResult(@PathVariable("id") Long id) {
        try {
            analysisResultRepository.deleteById(id);
            return ResponseEntity.ok().body("분석 기록 삭제 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("삭제 중 오류 발생");
        }
    }
}
