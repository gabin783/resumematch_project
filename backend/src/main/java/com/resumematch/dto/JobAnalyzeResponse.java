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
public class JobAnalyzeResponse {
    private String targetJob;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private List<String> mainTasks;
    private List<String> keywords;
    private String summary;
}
