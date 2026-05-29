package com.resumematch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRecommendationResponse {
    private Long jobPostingId;
    private String companyName;
    private String title;
    private String url;
    private String source;
    private int matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String reason;
}
