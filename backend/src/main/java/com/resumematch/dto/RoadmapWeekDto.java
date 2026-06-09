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
public class RoadmapWeekDto {
    private int week;
    private String title;
    private String goal;
    private List<String> focusSkills;
    private List<String> tasks;
    private List<String> completionCriteria;
    private List<String> selfCheckItems;
    private List<CourseDto> recommendedCourses;
    private List<LearningStepDto> learningSteps;
    private PracticeProjectDto practiceProject;
    private List<String> recommendedSearchQueries;
}
