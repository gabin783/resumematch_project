// resumematch_demo/resumematch_demo/controller/MypageController.java 파일

package resumematch_demo.resumematch_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import resumematch_demo.resumematch_demo.dto.analysisResultsDto;
import resumematch_demo.resumematch_demo.entity.AnalysisResult;
import resumematch_demo.resumematch_demo.entity.Member;
import resumematch_demo.resumematch_demo.entity.Resume;
import resumematch_demo.resumematch_demo.entity.Roadmap; // ✨ 임포트
import resumematch_demo.resumematch_demo.repository.AnalysisResultRepository;
import resumematch_demo.resumematch_demo.repository.MemberRepository;
import resumematch_demo.resumematch_demo.repository.ResumeRepository;
import resumematch_demo.resumematch_demo.repository.RoadmapRepository; // ✨ 임포트

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MypageController {

    private final MemberRepository memberRepository;
    private final ResumeRepository resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final RoadmapRepository roadmapRepository; // ✨ 의존성 주입

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        Long memberId = 1L; // 임시 고정 회원 ID

        Member member = memberRepository.findById(memberId).orElse(null);

        List<Resume> resumes = resumeRepository.findTop5ByMemberIdOrderByCreatedAtDesc(memberId);

        List<AnalysisResult> analysisResults = analysisResultRepository.findAll().stream()
                .filter(a -> a.getMemberId().equals(memberId)) // (임시 필터링, 레포지토리에 findByMemberId 추가하셨다면 그걸 쓰시면 됩니다!)
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .toList();

        // ✨ DB에서 해당 회원의 로드맵 최신순으로 가져오기
        List<Roadmap> roadmaps = roadmapRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        return ResponseEntity.ok().body(analysisResultsDto.builder()
                .profile(member)
                .resumes(resumes)
                .analysisResults(analysisResults)
                .roadmaps(roadmaps) // ✨ DTO에 담아서 프론트엔드로 슝!
                .build());
    }
}