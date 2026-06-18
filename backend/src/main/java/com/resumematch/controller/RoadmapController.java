package com.resumematch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.RoadmapRequestDto;
import com.resumematch.dto.RoadmapResponse;
import com.resumematch.entity.Roadmap;
import com.resumematch.repository.RoadmapRepository;
import com.resumematch.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final RoadmapRepository roadmapRepository; // ✨ 추가: DB 저장소

    @PostMapping("/recommend")
    public ResponseEntity<?> recommendCourses(@RequestBody RoadmapRequestDto request) {
        if (request == null) {
            request = new RoadmapRequestDto();
        }

        if (request.getMemberId() == null || request.getMemberId() <= 0) {
            return ResponseEntity.badRequest().body("memberId가 필요합니다.");
        }

        RoadmapResponse roadmap = roadmapService.generateRoadmap(request);

        try {
            Long currentMemberId = request.getMemberId();

            String jobTitle = (roadmap.getTargetJob() != null && !roadmap.getTargetJob().isEmpty())
                    ? roadmap.getTargetJob()
                    : "맞춤형 AI 학습 로드맵";

            ObjectMapper objectMapper = new ObjectMapper();
            String contentJson = objectMapper.writeValueAsString(roadmap);

            Roadmap roadmapEntity = Roadmap.builder()
                    .memberId(currentMemberId)
                    .targetJob(jobTitle)
                    .content(contentJson)
                    .build();

            roadmapRepository.save(roadmapEntity);
            roadmap.setId(roadmapEntity.getId());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("로드맵 DB 저장 중 오류 발생");
        }

        return ResponseEntity.ok(roadmap);
    }

    // ✨ 3. 마이페이지에서 사용할 로드맵 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoadmap(
            @PathVariable("id") Long id,
            @RequestParam(value = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            return ResponseEntity.badRequest().body("memberId가 필요합니다.");
        }

        try {
            Roadmap roadmap = roadmapRepository.findById(id).orElse(null);
            if (roadmap == null) {
                return ResponseEntity.status(404).body("삭제할 로드맵을 찾을 수 없습니다.");
            }

            if (!Objects.equals(roadmap.getMemberId(), memberId)) {
                return ResponseEntity.status(403).body("삭제 권한이 없습니다.");
            }

            roadmapRepository.delete(roadmap);
            return ResponseEntity.ok().body("로드맵 삭제 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("삭제 중 오류 발생");
        }
    }
}
