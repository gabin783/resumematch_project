package com.resumematch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.JobAnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class JobAnalyzeService {
    private static final Logger log = LoggerFactory.getLogger(JobAnalyzeService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    public JobAnalyzeResponse analyze(String jobDescription) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            log.warn("Job analyze skipped: jobDescription is empty");
            return fallbackResponse("jobDescription is empty");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Job analyze skipped: LLM_API_KEY is empty");
            return fallbackResponse("LLM_API_KEY is empty");
        }

        try {
            String content = callCommercialLlm(jobDescription.trim());
            JobAnalyzeResponse response = parseResponse(content);
            log.info(
                    "Job analyze success: targetJob={}, requiredSkills={}, preferredSkills={}",
                    response.getTargetJob(),
                    response.getRequiredSkills().size(),
                    response.getPreferredSkills().size()
            );
            return response;
        } catch (RestClientResponseException e) {
            log.error(
                    "Commercial LLM API request failed: status={}",
                    e.getStatusCode()
            );
            return fallbackResponse("commercial LLM API request failed");
        } catch (Exception e) {
            log.error("Job analyze failed: {}", e.getMessage());
            return fallbackResponse("job analyze failed");
        }
    }

    private String callCommercialLlm(String jobDescription) throws Exception {
        log.info(
                "Calling commercial LLM API: model={}, baseUrl={}",
                model,
                baseUrl
        );

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0.0,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are a job posting analysis expert. Return JSON only."
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(jobDescription)
                        )
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }

        return response.getBody();
    }

    private String buildPrompt(String jobDescription) {
        return """
                너는 채용공고 분석 전문가입니다.
                다음 채용공고 내용을 분석해서 JSON만 반환하세요.

                반환 형식:
                {
                  "targetJob": "",
                  "requiredSkills": [],
                  "preferredSkills": [],
                  "mainTasks": [],
                  "keywords": [],
                  "summary": ""
                }

                규칙:
                - JSON 외의 설명은 절대 포함하지 마세요.
                - requiredSkills에는 필수 기술만 넣으세요.
                - preferredSkills에는 우대사항 또는 있으면 좋은 기술만 넣으세요.
                - mainTasks에는 주요 업무를 3~5개로 요약하세요.
                - keywords에는 분석에 중요한 핵심 키워드를 넣으세요.
                - summary는 한 문장으로 작성하세요.
                - 스킬명은 가능한 한 표준 기술명으로 정리하세요. 예: SpringBoot는 Spring Boot, mysql은 MySQL.

                채용공고:
                %s
                """.formatted(jobDescription);
    }

    private JobAnalyzeResponse parseResponse(String content) throws Exception {
        String json = cleanJson(content);
        JobAnalyzeResponse response;
        try {
            response = objectMapper.readValue(json, JobAnalyzeResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response");
            throw e;
        }

        return JobAnalyzeResponse.builder()
                .targetJob(defaultText(response.getTargetJob(), "분석된 직무"))
                .requiredSkills(defaultList(response.getRequiredSkills()))
                .preferredSkills(defaultList(response.getPreferredSkills()))
                .mainTasks(defaultList(response.getMainTasks()))
                .keywords(defaultList(response.getKeywords()))
                .summary(defaultText(response.getSummary(), "채용공고 분석 결과입니다."))
                .build();
    }

    private String cleanJson(String content) {
        if (content == null) {
            return "{}";
        }

        String cleaned = content
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private JobAnalyzeResponse fallbackResponse(String reason) {
        log.warn("Returning fallback job analysis response: reason={}", reason);
        // TODO: Replace this fallback with existing local AI analysis if commercial API is unavailable.
        return JobAnalyzeResponse.builder()
                .targetJob("분석된 직무")
                .requiredSkills(List.of("Java", "Spring Boot"))
                .preferredSkills(List.of("AWS"))
                .mainTasks(List.of("채용공고 분석 결과를 불러오지 못했습니다."))
                .keywords(List.of("Java", "Spring Boot", "AWS"))
                .summary("상용 API 분석에 실패하여 기본 분석 결과를 표시합니다.")
                .build();
    }
}
