package com.resumematch.controller;

import com.resumematch.dto.JobRecommendationResponse;
import com.resumematch.entity.JobPosting;
import com.resumematch.repository.JobPostingRepository;
import com.resumematch.service.JobRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:5173") // 리액트(5173 포트)에서 접근할 수 있도록 허용!
public class JobPostingController {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobRecommendationService jobRecommendationService;

    // 리액트에서 GET 요청을 보내면 DB의 모든 공고를 List로 반환합니다.
    @GetMapping
    public List<JobPosting> getAllJobs() {
        return jobPostingRepository.findAll();
    }

    @GetMapping("/recommendations")
    public List<JobRecommendationResponse> getRecommendations() {
        return jobRecommendationService.getRecommendations();
    }
}
