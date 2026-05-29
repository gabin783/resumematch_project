package com.resumematch.service;

import com.resumematch.dto.JobRecommendationResponse;
import com.resumematch.entity.JobPosting;
import com.resumematch.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobRecommendationService {
    private final JobPostingRepository jobPostingRepository;

    public List<JobRecommendationResponse> getRecommendations() {
        return jobPostingRepository.findAll().stream()
                .map(this::toRecommendation)
                .toList();
    }

    private JobRecommendationResponse toRecommendation(JobPosting jobPosting) {
        String title = valueOrEmpty(jobPosting.getTitle());
        String normalizedTitle = title.toLowerCase();
        String source = jobPosting.getSource();
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>(List.of("JPA", "AWS", "Docker"));
        int matchScore = dummyScore(jobPosting.getId(), false);

        if (source == null && valueOrEmpty(jobPosting.getUrl()).contains("wanted.co.kr")) {
            source = "WANTED";
        }

        // TODO: Replace this dummy scoring with real resume analysis and job description matching.
        if (containsAny(normalizedTitle, "백엔드", "back-end", "backend", "server", "서버")) {
            matchedSkills.addAll(List.of("Java", "Spring Boot"));
            missingSkills = new ArrayList<>(List.of("JPA", "AWS"));
            matchScore = dummyScore(jobPosting.getId(), true);
        } else if (containsAny(normalizedTitle, "프론트", "frontend", "front-end")) {
            matchedSkills.addAll(List.of("React", "TypeScript"));
            missingSkills = new ArrayList<>(List.of("Next.js", "UI 테스트"));
            matchScore = dummyScore(jobPosting.getId(), true);
        } else {
            matchedSkills.addAll(List.of("문제해결", "협업"));
        }

        String reason = matchedSkills.contains("Java")
                ? "이력서의 Java/Spring 경험이 공고 요구사항과 일부 일치합니다."
                : "이력서의 프로젝트 경험과 협업 역량이 공고와 일부 연결됩니다.";

        return JobRecommendationResponse.builder()
                .jobPostingId(jobPosting.getId())
                .companyName(jobPosting.getCompanyName())
                .title(jobPosting.getTitle())
                .url(jobPosting.getUrl())
                .source(source != null ? source : "LOCAL")
                .matchScore(matchScore)
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .reason(reason)
                .build();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private int dummyScore(Long id, boolean isRoleMatched) {
        int[] matchedScores = {86, 81, 74, 68};
        int[] generalScores = {74, 68, 58};
        int index = Math.floorMod(id == null ? 0 : id.intValue(), isRoleMatched ? matchedScores.length : generalScores.length);
        return isRoleMatched ? matchedScores[index] : generalScores[index];
    }
}
