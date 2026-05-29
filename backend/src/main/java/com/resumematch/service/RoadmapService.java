package com.resumematch.service;

import com.resumematch.dto.CourseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";

    public List<CourseDto> generateRoadmap(List<String> keywords) {
        List<CourseDto> roadmap = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        long idCounter = 1;
        int stepCount = 1;

        for (String keyword : keywords) {
            try {
                // 1. 유튜브 API 호출 주소 조립 (검색어 + API 키)
                String url = String.format("%s?part=snippet&q=%s+강의&key=%s&maxResults=1&type=video",
                        YOUTUBE_API_URL, keyword, apiKey);

                // 2. API 호출 및 응답 받기
                String response = restTemplate.getForObject(url, String.class);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray items = jsonResponse.getJSONArray("items");

                if (items.length() > 0) {
                    JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
                    String videoId = items.getJSONObject(0).getJSONObject("id").getString("videoId");
                    String videoTitle = snippet.getString("title");

                    // 3. 응답받은 진짜 데이터를 CourseDto로 변환
                    roadmap.add(CourseDto.builder()
                            .id(idCounter++)
                            .step("Step " + stepCount++ + ". " + keyword + " 마스터")
                            .title(videoTitle)
                            .provider("YouTube")
                            .url("https://www.youtube.com/watch?v=" + videoId)
                            .time("영상 길이에 따름")
                            .tags(List.of(keyword, "AI 추천"))
                            .build());
                }
            } catch (Exception e) {
                System.err.println(keyword + " 검색 중 오류 발생: " + e.getMessage());
            }
        }

        return roadmap;
    }
}