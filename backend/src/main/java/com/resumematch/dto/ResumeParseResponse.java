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
public class ResumeParseResponse {
    private String status;
    private int count;
    private List<String> skills;
    private List<String> keywords;
    private List<String> technicalSkills;
    private List<String> softSkills;
    private List<String> projects;
    private String experienceSummary;
    private List<String> recommendedJobTypes;

    public ResumeParseResponse(String status, int count, List<String> skills) {
        this.status = status;
        this.count = count;
        this.skills = skills;
        this.keywords = List.of();
        this.technicalSkills = skills;
        this.softSkills = List.of();
        this.projects = List.of();
        this.experienceSummary = "";
        this.recommendedJobTypes = List.of();
    }
}
