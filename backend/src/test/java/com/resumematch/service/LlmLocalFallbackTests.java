package com.resumematch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.GapMatchRequest;
import com.resumematch.dto.GapMatchResponse;
import com.resumematch.dto.ResumeParseResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmLocalFallbackTests {

    @Test
    void resumeAnalysisWithoutApiKeyReturnsLocalSkills() {
        ResumeAnalyzerService resumeAnalyzerService = mock(ResumeAnalyzerService.class);
        when(resumeAnalyzerService.extractKeywords("Java Spring Boot MySQL 경력"))
                .thenReturn(List.of("Java", "Spring Boot", "MySQL", "경력"));

        ResumeLlmAnalyzeService service = new ResumeLlmAnalyzeService(resumeAnalyzerService);

        ResumeParseResponse response = service.analyze("Java Spring Boot MySQL 경력");

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getSkills()).contains("Java", "Spring Boot", "MySQL");
        assertThat(response.getKeywords()).contains("Java", "Spring Boot", "MySQL");
        assertThat(response.getTechnicalSkills()).isEqualTo(response.getSkills());
        assertThat(response.getSoftSkills()).isNotNull();
        assertThat(response.getProjects()).isNotNull();
        assertThat(response.getRecommendedJobTypes()).isNotNull();
    }

    @Test
    void gapAnalysisWithoutApiKeyReturnsNormalizedLocalResponse() {
        GapLlmAnalyzeService service = new GapLlmAnalyzeService();
        GapMatchRequest request = new GapMatchRequest();
        request.setTargetJob("백엔드 개발자");
        request.setJdText("Java, Spring Boot, MySQL, Docker");
        request.setRequiredSkills(List.of("Java", "Spring Boot", "MySQL"));
        request.setPreferredSkills(List.of("Docker"));
        request.setMainTasks(List.of("REST API 개발"));
        request.setKeywords(List.of("Java", "Spring Boot", "Docker"));
        request.setSummary("Spring Boot 기반 백엔드 개발");

        GapMatchResponse response = service.analyze(
                List.of("Java", "Spring Boot"),
                request
        );

        assertThat(response.getMatchScore()).isBetween(0, 100);
        assertThat(response.getTargetJob()).isEqualTo("백엔드 개발자");
        assertThat(response.getAnalysis()).isNotBlank();
        assertThat(response.getLearningDirection()).isNotBlank();
        assertThat(response.getOwnedSkills()).isNotNull();
        assertThat(response.getMatchedSkills()).isNotNull();
        assertThat(response.getPartialSkills()).isNotNull();
        assertThat(response.getMissingSkills()).isNotNull();
        assertThat(response.getRequiredSkills()).containsExactly("Java", "Spring Boot", "MySQL");
        assertThat(response.getPreferredSkills()).containsExactly("Docker");
        assertThat(response.getMainTasks()).containsExactly("REST API 개발");
        assertThat(response.getJobKeywords()).containsExactly("Java", "Spring Boot", "Docker");
        assertThat(response.getJobSummary()).isEqualTo("Spring Boot 기반 백엔드 개발");
    }

    @Test
    void resumeAnalysisWithApiKeyUsesOpenAiResponse() throws Exception {
        String content = """
                {
                  "skills": ["Java", "Spring Boot"],
                  "technicalSkills": ["Java", "Spring Boot"],
                  "softSkills": ["협업"],
                  "projects": ["OpenAI 분석 프로젝트"],
                  "experienceSummary": "OpenAI 분석 결과",
                  "keywords": ["Java"],
                  "recommendedJobTypes": ["백엔드 개발자"]
                }
                """;
        HttpServer server = startOpenAiServer(content);

        try {
            ResumeAnalyzerService resumeAnalyzerService = mock(ResumeAnalyzerService.class);
            when(resumeAnalyzerService.extractKeywords("Java Spring Boot 경력"))
                    .thenReturn(List.of("Java", "Spring Boot"));
            ResumeLlmAnalyzeService service = new ResumeLlmAnalyzeService(resumeAnalyzerService);
            ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
            ReflectionTestUtils.setField(service, "model", "test-model");
            ReflectionTestUtils.setField(service, "baseUrl", serverUrl(server));

            ResumeParseResponse response = service.analyze("Java Spring Boot 경력");

            assertThat(response.getProjects()).containsExactly("OpenAI 분석 프로젝트");
            assertThat(response.getExperienceSummary()).isEqualTo("OpenAI 분석 결과");
            assertThat(response.getRecommendedJobTypes()).containsExactly("백엔드 개발자");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void gapAnalysisWithApiKeyUsesOpenAiResponse() throws Exception {
        String content = """
                {
                  "matchScore": 80,
                  "targetJob": "백엔드 개발자",
                  "analysis": "OpenAI 갭 분석",
                  "learningDirection": "Java 심화 학습\\nREST API 프로젝트\\nDocker 배포 실습",
                  "ownedSkills": [],
                  "matchedSkills": [],
                  "partialSkills": [],
                  "missingSkills": [],
                  "requiredSkills": ["Java", "Spring Boot"],
                  "preferredSkills": ["Docker"],
                  "mainTasks": ["REST API 개발"],
                  "jobKeywords": ["Java", "Docker"],
                  "jobSummary": "OpenAI 공고 분석"
                }
                """;
        HttpServer server = startOpenAiServer(content);

        try {
            GapLlmAnalyzeService service = new GapLlmAnalyzeService();
            ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
            ReflectionTestUtils.setField(service, "model", "test-model");
            ReflectionTestUtils.setField(service, "baseUrl", serverUrl(server));
            GapMatchRequest request = new GapMatchRequest();
            request.setTargetJob("백엔드 개발자");
            request.setJdText("Java와 Spring Boot 개발자");

            GapMatchResponse response = service.analyze(List.of("Java"), request);

            assertThat(response.getRequiredSkills()).containsExactly("Java", "Spring Boot");
            assertThat(response.getPreferredSkills()).containsExactly("Docker");
            assertThat(response.getMainTasks()).containsExactly("REST API 개발");
            assertThat(response.getJobKeywords()).containsExactly("Java", "Docker");
            assertThat(response.getJobSummary()).isEqualTo("OpenAI 공고 분석");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startOpenAiServer(String content) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", content)
                ))
        ));
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(command -> {
            Thread thread = new Thread(command, "mock-openai-server");
            thread.setDaemon(true);
            thread.start();
        });
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private String serverUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
    }
}
