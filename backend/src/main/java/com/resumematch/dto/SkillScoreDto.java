package com.resumematch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillScoreDto {
    private String name;
    private int score;
    private String reason;
    private String evidence;
    private String priority;
}
