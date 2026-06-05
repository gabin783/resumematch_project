package com.resumematch.controller;

import com.resumematch.dto.JobAnalyzeRequest;
import com.resumematch.dto.JobAnalyzeResponse;
import com.resumematch.dto.JobUrlExtractRequest;
import com.resumematch.dto.JobUrlExtractResponse;
import com.resumematch.service.JobAnalyzeService;
import com.resumematch.service.JobUrlExtractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class JobAnalyzeController {
    private final JobAnalyzeService jobAnalyzeService;
    private final JobUrlExtractService jobUrlExtractService;

    @PostMapping("/analyze")
    public ResponseEntity<JobAnalyzeResponse> analyze(@RequestBody(required = false) JobAnalyzeRequest request) {
        String jobDescription = request == null ? null : request.getJobDescription();
        return ResponseEntity.ok(jobAnalyzeService.analyze(jobDescription));
    }

    @PostMapping("/extract-url")
    public ResponseEntity<JobUrlExtractResponse> extractUrl(@RequestBody(required = false) JobUrlExtractRequest request) {
        String url = request == null ? null : request.getUrl();
        return ResponseEntity.ok(jobUrlExtractService.extract(url));
    }
}
