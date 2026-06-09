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
public class CourseDto {
    private Long id;
    private String step;
    private String title;
    private String provider;
    private String url;
    private String time;
    private String level;
    private String keyword;
    private List<String> tags;
}
