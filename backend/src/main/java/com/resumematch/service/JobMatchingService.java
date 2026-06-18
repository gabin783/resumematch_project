package com.resumematch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.resumematch.dto.JobMatchResponse;
import com.resumematch.entity.JobPosting;
import com.resumematch.repository.JobPostingRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobMatchingService {

    @Autowired
    private OllamaAiService ollamaAiService;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    // ✨ 1. 리액트에서 요청이 오면 DB 데이터를 조회해서 매칭을 준비하는 메서드
    public JobMatchResponse matchWithDbData(Long memberId, Long jobId) {

        // ① DB에서 채용 공고 정보 꺼내오기
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채용공고를 찾을 수 없습니다."));

        // ② 내 이력서 스킬 가져오기 (🚨 현재는 테스트용 임시 스킬. 나중에 실제 DB 데이터로 교체하세요!)
        String mySkills = "Java, Spring, React";

        // ③ 채용 공고 스킬 가져오기 (🚨 현재는 테스트용 임시 스킬)
        String jobSkills = "Java, Spring, AWS, MySQL";


        // ④ 하이브리드 엔진(Java + AI) 호출!
        return analyzeHybridMatch(mySkills, jobSkills);
    }

    // ✨ 2. 실제 매칭 연산과 AI 피드백 생성을 담당하는 하이브리드 엔진 메서드
    public JobMatchResponse analyzeHybridMatch(String myResumeSkills, String jobPostingSkills) {

        List<String> mySkills = Arrays.stream(myResumeSkills.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        List<String> requiredSkills = Arrays.stream(jobPostingSkills.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        List<String> matchedSkills = mySkills.stream()
                .filter(requiredSkills::contains)
                .collect(Collectors.toList());

        List<String> missingSkills = requiredSkills.stream()
                .filter(skill -> !mySkills.contains(skill))
                .collect(Collectors.toList());

        int matchRate = 0;
        if (!requiredSkills.isEmpty()) {
            matchRate = (int) (((double) matchedSkills.size() / requiredSkills.size()) * 100);
        }

        // OllamaAiService에서 임시(Mock) 응답을 주도록 설정해둔 상태입니다!
        String aiFeedback = ollamaAiService.generateHybridFeedback(matchRate, matchedSkills, missingSkills);

        // 연산된 모든 데이터를 DTO에 예쁘게 포장해서 컨트롤러로 전달
        return new JobMatchResponse(matchRate, matchedSkills, missingSkills, aiFeedback);
    }
}
