package resumematch_demo.resumematch_demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import resumematch_demo.resumematch_demo.dto.JobMatchResponse;
import resumematch_demo.resumematch_demo.service.JobMatchingService;

@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "http://localhost:5173") // 리액트(5173 포트) 접근 허용
public class JobMatchingController {

    @Autowired
    private JobMatchingService jobMatchingService;

    // ✨ 1. [실전용] 리액트 화면에서 공고 매칭 버튼을 눌렀을 때 호출되는 API
    // 예: http://localhost:8080/api/match/5?memberId=1
    @PostMapping("/{jobId}")
    public JobMatchResponse matchJob(
            @PathVariable Long jobId,
            @RequestParam Long memberId) {

        return jobMatchingService.matchWithDbData(memberId, jobId);
    }

    // ✨ 2. [테스트용] 브라우저 주소창에 직접 스킬을 입력해서 확인해보는 API
    // 예: http://localhost:8080/api/match/test?my=Java,Spring&job=Java,Spring,AWS
    @GetMapping("/test")
    public JobMatchResponse testMatch(
            @RequestParam(value = "my") String mySkills,
            @RequestParam(value = "job") String jobSkills) {

        return jobMatchingService.analyzeHybridMatch(mySkills, jobSkills);
    }
}