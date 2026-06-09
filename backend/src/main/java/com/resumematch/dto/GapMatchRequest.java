package com.resumematch.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GapMatchRequest {
    private Long memberId;
    private List<String> resumeSkills;
    private List<String> technicalSkills;
    private List<String> resumeKeywords;
    private String experienceSummary;
    private List<String> recommendedJobTypes;
    private String jdText;
    private String targetJob;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private List<String> mainTasks;
    private List<String> keywords;
    private String summary;
}
