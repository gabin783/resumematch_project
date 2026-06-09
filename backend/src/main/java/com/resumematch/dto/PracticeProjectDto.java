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
public class PracticeProjectDto {
    private String title;
    private String goal;
    private List<String> requirements;
    private List<String> completionDefinition;
    private String resumeBullet;
}