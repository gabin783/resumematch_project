package com.resumematch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.RoadmapRequestDto;
import com.resumematch.dto.RoadmapResponse;
import com.resumematch.dto.RoadmapWeekDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoadmapLlmGenerateService {
    private static final Logger log = LoggerFactory.getLogger(RoadmapLlmGenerateService.class);
    private static final int LOG_RESPONSE_MAX_LENGTH = 1000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    public RoadmapResponse generate(RoadmapRequestDto request) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("LLM_API_KEY is empty");
        }

        log.info(
                "Calling roadmap commercial LLM API: model={}, baseUrl={}, apiKey={}",
                model,
                baseUrl,
                maskApiKey(apiKey)
        );

        String content = callCommercialLlm(request);
        RoadmapResponse response = parseResponse(content);
        validateResponse(response);

        log.info(
                "Roadmap LLM generate success: targetJob={}, weeks={}",
                response.getTargetJob(),
                response.getWeeks().size()
        );

        return response;
    }

    private String callCommercialLlm(RoadmapRequestDto request) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are a career learning roadmap expert. Return JSON only."
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(request)
                        )
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");

        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText();
        }

        return response.getBody();
    }

    private String buildPrompt(RoadmapRequestDto request) throws Exception {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("targetJob", defaultText(request.getTargetJob()));
        input.put(
                "missingSkills",
                defaultList(request.getMissingSkills()).isEmpty()
                        ? defaultList(request.getKeywords())
                        : defaultList(request.getMissingSkills())
        );
        input.put("learningDirection", defaultText(request.getLearningDirection()));
        input.put("requiredSkills", defaultList(request.getRequiredSkills()));
        input.put("preferredSkills", defaultList(request.getPreferredSkills()));
        input.put("ownedSkills", defaultList(request.getOwnedSkills()));
        input.put("matchedSkills", defaultList(request.getMatchedSkills()));
        input.put("jobSummary", defaultText(request.getJobSummary()));
        input.put("analysis", defaultText(request.getAnalysis()));

        return """
                너는 개발자 취업 학습 로드맵 전문가입니다.
                아래 스킬 갭 분석 결과를 바탕으로 5주차 학습 로드맵 JSON만 반환하세요.

                이 로드맵의 핵심 목적은 단순 강의 링크 추천이 아니라,
                사용자가 채용공고의 부족 역량을 보완하고 이력서에 적을 수 있는 작은 산출물을 만드는 것입니다.

                사용자는 초급~주니어 개발자 취업 준비생입니다.
                공식문서나 검색 키워드만 던지는 방식은 피하고,
                이번 주에 무엇을 학습하고, 무엇을 만들고, 어디까지 끝내야 하는지 구체적으로 설계하세요.

                반환 형식:
                {
                  "targetJob": "백엔드 개발자",
                  "summary": "백엔드 개발자로 전환하기 위해 Java/Spring 기반 API 개발과 배포 경험을 우선 보완하는 5주 로드맵입니다.",
                  "weeks": [
                    {
                      "week": 1,
                      "title": "Java와 객체지향 기초 정리",
                      "goal": "Java 문법과 객체지향 개념을 익혀 Spring 학습 기반을 마련합니다.",
                      "focusSkills": ["Java", "OOP"],
                      "tasks": [
                        "Java 기본 문법과 컬렉션을 정리합니다.",
                        "클래스, 인터페이스, 상속, 다형성을 예제로 실습합니다.",
                        "간단한 콘솔 CRUD 프로그램을 만들어 GitHub에 정리합니다."
                      ],
                      "completionCriteria": [
                        "Java 기본 문법을 설명할 수 있습니다.",
                        "객체지향 개념을 코드 예제로 구현할 수 있습니다.",
                        "GitHub에 Java 실습 결과를 정리했습니다."
                      ],
                      "selfCheckItems": [
                        "List와 Map의 차이를 설명할 수 있나요?",
                        "인터페이스를 사용하는 이유를 설명할 수 있나요?",
                        "간단한 CRUD 로직을 Java로 작성할 수 있나요?"
                      ],
                      "learningSteps": [
                        {
                          "type": "concept",
                          "title": "Java 클래스와 객체 개념 이해",
                          "description": "클래스, 객체, 필드, 메서드의 차이를 예제 중심으로 정리합니다.",
                          "expectedOutput": "Book 클래스를 만들고 필드와 메서드를 정의합니다."
                        },
                        {
                          "type": "practice",
                          "title": "콘솔 기반 CRUD 흐름 구현",
                          "description": "List 또는 Map을 사용해 등록, 조회, 수정, 삭제 흐름을 구현합니다.",
                          "expectedOutput": "main 메서드에서 샘플 데이터를 넣고 CRUD 동작을 확인합니다."
                        },
                        {
                          "type": "resume",
                          "title": "이력서용 경험 문장 정리",
                          "description": "구현한 기능을 이력서 프로젝트 경험 문장으로 정리합니다.",
                          "expectedOutput": "Java 객체지향 구조를 활용한 CRUD 구현 경험 문장 1개를 작성합니다."
                        }
                      ],
                      "practiceProject": {
                        "title": "콘솔 기반 도서 대여 관리 프로그램",
                        "goal": "Java 객체지향 구조와 컬렉션을 사용해 작은 도메인의 CRUD 흐름을 구현합니다.",
                        "requirements": [
                          "Book, Member, Rental 클래스를 분리합니다.",
                          "도서 등록, 목록 조회, 대여 상태 변경 기능을 구현합니다.",
                          "List 또는 Map을 사용해 데이터를 관리합니다."
                        ],
                        "completionDefinition": [
                          "main 메서드에서 샘플 데이터로 기능을 실행할 수 있습니다.",
                          "클래스 분리 이유와 객체지향 구조를 설명할 수 있습니다."
                        ],
                        "resumeBullet": "Java 객체지향 구조를 적용해 도서 대여 관리 도메인의 CRUD 기능을 구현했습니다."
                      },
                      "recommendedSearchQueries": [
                        "Java 객체지향 클래스 객체 예제",
                        "Java List Map CRUD 콘솔 예제",
                        "Java 인터페이스 상속 다형성 예제"
                      ]
                    }
                  ]
                }

                규칙:
                - JSON 외의 설명, 마크다운, 코드블록은 절대 포함하지 마세요.
                - weeks는 정확히 5개입니다.
                - 각 week는 tasks를 정확히 3개 작성하세요.
                - 각 week는 completionCriteria를 2~3개 작성하세요.
                - 각 week는 selfCheckItems를 2~3개 작성하세요.
                - 각 week는 focusSkills를 1~3개 작성하세요.
                - 각 week는 learningSteps를 정확히 3개 작성하세요.
                - learningSteps의 type은 concept, practice, resume 중 하나를 사용하세요.
                - learningSteps는 concept → practice → resume 순서로 작성하세요.
                - 각 week는 practiceProject를 반드시 작성하세요.
                - practiceProject.title은 해당 주차에 실제로 만들 수 있는 작은 프로젝트명으로 작성하세요.
                - practiceProject.goal은 왜 이 과제를 하는지 설명하세요.
                - practiceProject.requirements는 정확히 3개 작성하세요.
                - practiceProject.completionDefinition은 2~3개 작성하세요.
                - practiceProject.resumeBullet은 이력서에 넣을 수 있는 한 문장으로 작성하세요.
                - 각 week는 recommendedSearchQueries를 2~3개 작성하세요.
                - 모든 문장은 한국어로 작성하세요.
                - 실제 학습 행동 중심으로 작성하세요.
                - 부족 스킬 우선순위와 learningDirection을 반영하세요.
                - 너무 추상적인 문장을 쓰지 마세요.
                - “공식문서를 보세요”, “강의를 찾아보세요”처럼 막연한 문장만 쓰지 마세요.
                - 검색어는 사용자가 바로 검색할 수 있을 만큼 구체적으로 작성하세요.
                - 사용자가 이번 주에 무엇을 만들고, 어디까지 끝내야 하는지 분명하게 작성하세요.
                - 1주차는 가장 기초가 되는 필수 기술, 5주차는 실전 프로젝트 또는 배포/운영 경험으로 마무리하세요.
                - 사용자가 보유한 기술이 있다면 그것을 활용해 부족 기술로 이어지도록 설계하세요.
                - 예: React 경험이 있고 Java/Spring이 부족하다면, 백엔드 API를 만들고 기존 프론트와 연결하는 식으로 설계하세요.
                - practiceProject는 너무 큰 프로젝트가 아니라 1주 안에 만들 수 있는 작은 산출물이어야 합니다.
                - 매주 GitHub에 정리하거나 이력서에 반영할 수 있는 결과물이 남도록 설계하세요.
                - 단순 개념 암기보다 작은 구현 경험을 만들도록 설계하세요.

                주차별 설계 가이드:
                - 1주차: 가장 기초가 되는 언어/개념/필수 선행 지식
                - 2주차: 핵심 프레임워크 또는 주요 기술의 기본 사용
                - 3주차: API, 데이터베이스, 도메인 모델링 등 실무 연결
                - 4주차: 배포, 운영, CI/CD, 성능, 예외 처리 등 우대 역량 연결
                - 5주차: 앞선 학습 내용을 합친 작은 실전 프로젝트와 이력서 정리

                입력 데이터:
                %s
                """.formatted(objectMapper.writeValueAsString(input));
    }

    private RoadmapResponse parseResponse(String content) throws Exception {
        String json = cleanJson(content);

        try {
            return objectMapper.readValue(json, RoadmapResponse.class);
        } catch (Exception e) {
            log.error(
                    "Failed to parse roadmap LLM JSON response. rawResponse={}",
                    truncate(content, LOG_RESPONSE_MAX_LENGTH),
                    e
            );
            throw e;
        }
    }

    private void validateResponse(RoadmapResponse response) {
        if (response == null || response.getWeeks() == null || response.getWeeks().size() != 5) {
            throw new IllegalStateException("Roadmap LLM response must contain exactly 5 weeks");
        }

        for (RoadmapWeekDto week : response.getWeeks()) {
            if (week.getTasks() == null || week.getTasks().size() != 3) {
                throw new IllegalStateException("Roadmap week tasks must contain exactly 3 items");
            }

            if (week.getLearningSteps() == null) {
                week.setLearningSteps(List.of());
            }

            if (week.getRecommendedSearchQueries() == null) {
                week.setRecommendedSearchQueries(List.of());
            }
        }
    }

    private String cleanJson(String content) {
        if (content == null) {
            return "{}";
        }

        return content
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String defaultText(String value) {
        return value == null || value.trim().isEmpty() ? "" : value;
    }

    private String maskApiKey(String value) {
        if (value == null || value.isBlank()) {
            return "(empty)";
        }

        String trimmed = value.trim();
        if (trimmed.length() <= 7) {
            return "****";
        }

        return trimmed.substring(0, 7) + "...****";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }
}