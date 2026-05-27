package resumematch_demo.resumematch_demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import resumematch_demo.resumematch_demo.dto.CourseDto;
import resumematch_demo.resumematch_demo.dto.RoadmapRequestDto;
import resumematch_demo.resumematch_demo.entity.Roadmap;
import resumematch_demo.resumematch_demo.repository.RoadmapRepository;
import resumematch_demo.resumematch_demo.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final RoadmapRepository roadmapRepository; // ✨ 추가: DB 저장소

    @PostMapping("/recommend")
    public ResponseEntity<List<CourseDto>> recommendCourses(@RequestBody RoadmapRequestDto request) {
        // 1. 기존처럼 유튜브 API 등을 통해 로드맵 리스트 생성
        List<CourseDto> recommendedCourses = roadmapService.generateRoadmap(request.getKeywords());

        // 2. ✨ 생성된 로드맵을 DB에 저장하는 로직 추가
        try {
            Long currentMemberId = 1L; // 임시 고정 회원 ID

            // 프론트엔드에서 targetJob을 안 보냈을 경우를 대비한 기본값 처리
            String jobTitle = (request.getTargetJob() != null && !request.getTargetJob().isEmpty())
                    ? request.getTargetJob()
                    : "맞춤형 AI 학습 로드맵";

            // List<CourseDto> 객체를 통째로 JSON 문자열로 변환 (DB의 TEXT 컬럼에 넣기 위함)
            ObjectMapper objectMapper = new ObjectMapper();
            String contentJson = objectMapper.writeValueAsString(recommendedCourses);

            Roadmap roadmapEntity = Roadmap.builder()
                    .memberId(currentMemberId)
                    .targetJob(jobTitle)
                    .content(contentJson)
                    .build();

            roadmapRepository.save(roadmapEntity);
            System.out.println("✅ 학습 로드맵 DB 저장 완료! ID: " + roadmapEntity.getId());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("로드맵 DB 저장 중 오류 발생");
        }

        // 3. 프론트엔드에는 정상적으로 생성된 로드맵 리스트 반환
        return ResponseEntity.ok(recommendedCourses);
    }

    // ✨ 3. 마이페이지에서 사용할 로드맵 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoadmap(@PathVariable("id") Long id) {
        try {
            roadmapRepository.deleteById(id);
            return ResponseEntity.ok().body("로드맵 삭제 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("삭제 중 오류 발생");
        }
    }
}