package com.resumematch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapMatchResponse {
    private int matchScore;
    private String targetJob;
    private String analysis;
    private String learningDirection;
    private List<SkillScoreDto> ownedSkills;
    private List<SkillScoreDto> matchedSkills;
    private List<SkillScoreDto> partialSkills;
    private List<SkillScoreDto> missingSkills;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private List<String> mainTasks;
    private List<String> jobKeywords;
    private String jobSummary;
}
