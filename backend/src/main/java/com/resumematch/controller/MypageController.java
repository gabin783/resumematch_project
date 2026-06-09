package com.resumematch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.resumematch.dto.analysisResultsDto;
import com.resumematch.entity.AnalysisResult;
import com.resumematch.entity.Member;
import com.resumematch.entity.Resume;
import com.resumematch.entity.Roadmap; // ✨ 임포트
import com.resumematch.repository.AnalysisResultRepository;
import com.resumematch.repository.MemberRepository;
import com.resumematch.repository.ResumeRepository;
import com.resumematch.repository.RoadmapRepository; // ✨ 임포트

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
    public ResponseEntity<?> getDashboardData(@RequestParam(required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            return ResponseEntity.badRequest().body("memberId가 필요합니다.");
        }

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 회원입니다.");
        }

        List<Resume> resumes = resumeRepository.findTop5ByMemberIdOrderByCreatedAtDesc(memberId);

        List<AnalysisResult> analysisResults = analysisResultRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

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
