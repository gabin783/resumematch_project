package com.resumematch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ResumeParseResponse {
    private String status;
    private int count;
    private List<String> skills;
}