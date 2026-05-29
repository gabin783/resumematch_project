package com.resumematch.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class RoadmapRequestDto {

    private List<String> keywords; // 기존에 있던 키워드 리스트

    // ✨ 목표 직무 이름을 받기 위해 이 줄을 추가해 주세요!
    private String targetJob;
}