package com.resumematch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.GapMatchRequest;
import com.resumematch.dto.GapMatchResponse;
import com.resumematch.dto.SkillScoreDto;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GapLlmAnalyzeService {
    private static final Logger log = LoggerFactory.getLogger(GapLlmAnalyzeService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OllamaAiService ollamaAiService;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    public GapLlmAnalyzeService(OllamaAiService ollamaAiService) {
        this.ollamaAiService = ollamaAiService;
    }

    public GapMatchResponse analyze(List<String> resumeSkills, GapMatchRequest request) {
        if (resumeSkills == null || resumeSkills.isEmpty()) {
            log.warn("Gap LLM analyze skipped: resumeSkills is empty");
            return fallbackAnalyze(List.of(), request, "resumeSkills is empty");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gap LLM analyze skipped: LLM_API_KEY is empty");
            return fallbackAnalyze(resumeSkills, request, "LLM_API_KEY is empty");
        }

        try {
            String content = callCommercialLlm(resumeSkills, request);
            GapMatchResponse response = parseLlmResponse(content, resumeSkills, request);
            log.info(
                    "Gap LLM analyze success: matchScore={}, matchedSkills={}, missingSkills={}",
                    response.getMatchScore(),
                    safeSize(response.getMatchedSkills()),
                    safeSize(response.getMissingSkills())
            );
            return response;
        } catch (RestClientResponseException e) {
            log.error(
                    "Gap commercial LLM API request failed: status={}, body={}",
                    e.getStatusCode(),
                    truncate(e.getResponseBodyAsString(), 800),
                    e
            );
            return fallbackAnalyze(resumeSkills, request, "commercial LLM API request failed");
        } catch (Exception e) {
            log.error("Gap LLM analyze failed: {}", e.getMessage(), e);
            return fallbackAnalyze(resumeSkills, request, "gap LLM analyze failed");
        }
    }

    private String callCommercialLlm(List<String> resumeSkills, GapMatchRequest request) throws Exception {
        log.info(
                "Calling gap commercial LLM API: model={}, baseUrl={}, apiKey={}",
                model,
                baseUrl,
                maskApiKey(apiKey)
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
                                "content", "You are a technical recruiter and skill gap analyst. Return JSON only."
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(resumeSkills, request)
                        )
                )
        );

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, httpRequest, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }

        return response.getBody();
    }

    private String buildPrompt(List<String> resumeSkills, GapMatchRequest request) throws Exception {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("resumeSkills", defaultList(resumeSkills));
        input.put("technicalSkills", defaultList(request.getTechnicalSkills()));
        input.put("resumeKeywords", defaultList(request.getResumeKeywords()));
        input.put("experienceSummary", defaultText(request.getExperienceSummary(), ""));
        input.put("recommendedJobTypes", defaultList(request.getRecommendedJobTypes()));
        input.put("targetJob", defaultText(request.getTargetJob(), ""));
        input.put("jobDescription", defaultText(request.getJdText(), ""));
        input.put("requiredSkills", defaultList(request.getRequiredSkills()));
        input.put("preferredSkills", defaultList(request.getPreferredSkills()));
        input.put("mainTasks", defaultList(request.getMainTasks()));
        input.put("jobKeywords", defaultList(request.getKeywords()));
        input.put("jobSummary", defaultText(request.getSummary(), ""));

        return """
                너는 채용 담당자이자 스킬 갭 분석 전문가입니다.
                아래 이력서 분석 결과와 채용공고 분석 결과를 비교해서 JSON만 반환하세요.

                반환 형식:
                {
                  "matchScore": 72,
                  "targetJob": "",
                  "analysis": "",
                  "learningDirection": "",
                  "ownedSkills": [
                    { "name": "", "score": 80, "reason": "", "evidence": "", "priority": "medium" }
                  ],
                  "matchedSkills": [
                    { "name": "", "score": 90, "reason": "", "evidence": "", "priority": "low" }
                  ],
                  "partialSkills": [
                    { "name": "", "score": 60, "reason": "", "evidence": "", "priority": "medium" }
                  ],
                  "missingSkills": [
                    { "name": "", "score": 30, "reason": "", "evidence": "", "priority": "high" }
                  ],
                  "requiredSkills": [],
                  "preferredSkills": [],
                  "mainTasks": [],
                  "jobKeywords": [],
                  "jobSummary": ""
                }

                규칙:
                - JSON 외의 설명, 마크다운, 코드블록은 절대 포함하지 마세요.
                - requiredSkills를 가장 중요하게 평가하세요.
                - resumeSkills에 명시된 기술은 절대 missingSkills로 분류하지 마세요.
                - JD 요구사항에 있고 resumeSkills에도 있으면 matchedSkills로 분류하세요.
                - JD 요구사항에 있고 resumeSkills에는 없지만 관련 기술, 유사 경험, 같은 계열의 프레임워크/도구 경험이 있으면 partialSkills로 분류하세요.
                - JD 요구사항에 있고 resumeSkills 및 관련 경험이 모두 없을 때만 missingSkills로 분류하세요.
                - 실무 경험의 깊이가 부족한 경우에는 missingSkills가 아니라 partialSkills로 분류하고 score를 50~70 사이로 주세요.
                - missingSkills에는 정말 이력서에서 확인되지 않는 기술만 넣으세요.
                - 비슷한 경험이나 관련 기술은 partialSkills에 넣으세요.
                - preferredSkills는 matchScore에 보조적으로만 반영하세요.
                - 이력서에 근거가 없는 기술을 ownedSkills로 판단하지 마세요.
                - ownedSkills에는 이력서 보유 기술 중 핵심 기술을 최소 5개, 최대 8개 넣으세요.
                - ownedSkills는 구체적인 기술명만 사용하세요. 예: Java, Spring Boot, JPA, MySQL, REST API, Git, Docker, AWS.
                - ownedSkills에는 백엔드 개발, 풀스택 개발, 협업, 문제 해결 같은 직무명/추상 역량을 넣지 마세요.
                - missingSkills의 score는 모두 같은 값으로 쓰지 마세요.
                - missingSkills score는 부족 정도와 우선순위에 따라 20~60 사이에서 다르게 작성하세요.
                - 중요하고 근거가 거의 없는 기술은 20~35, 일부 관련 경험이 있는 기술은 40~60으로 작성하세요.
                - missingSkills priority가 high이면 낮은 score를, low이면 상대적으로 높은 score를 부여하세요.
                - matchScore는 0~100 사이 정수이며 너무 후하게 주지 마세요.
                - 필수 스킬 직접 일치는 1점, 부분 일치는 0.5점, 미보유는 0점 기준으로 판단하고 우대 스킬 일치는 보너스로만 반영하세요.
                - ownedSkills, matchedSkills, partialSkills, missingSkills의 score도 0~100 사이 정수로 작성하세요.
                - priority는 high, medium, low 중 하나로 작성하세요.
                - requiredSkills, preferredSkills, mainTasks, jobKeywords, jobSummary는 입력된 채용공고 분석 값을 유지하거나 정리해서 반환하세요.

                입력 데이터:
                %s
                """.formatted(objectMapper.writeValueAsString(input));
    }

    private GapMatchResponse parseLlmResponse(String content, List<String> resumeSkills, GapMatchRequest request) throws Exception {
        String json = cleanJson(content);
        GapMatchResponse parsed;
        try {
            parsed = objectMapper.readValue(json, GapMatchResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse gap LLM JSON response. rawResponse={}", truncate(content, 1000), e);
            throw e;
        }

        GapMatchResponse normalized = normalizeResponse(parsed, resumeSkills, request);
        if (normalized.getAnalysis().isBlank() || normalized.getLearningDirection().isBlank()) {
            throw new IllegalStateException("Gap LLM response lacks required text fields");
        }

        return normalized;
    }

    private GapMatchResponse fallbackAnalyze(List<String> resumeSkills, GapMatchRequest request, String reason) {
        log.warn("Gap LLM analyze failed, fallback to Ollama/Gemma: reason={}", reason);

        try {
            String jsonResponse = ollamaAiService.analyzeGapWithGemma(
                    resumeSkills,
                    request.getJdText(),
                    request.getTargetJob()
            );
            GapMatchResponse parsed = parseFallbackResponse(jsonResponse);
            return normalizeResponse(parsed, resumeSkills, request);
        } catch (Exception e) {
            log.error("Gap fallback analyze failed: {}", e.getMessage(), e);
            return normalizeResponse(
                    GapMatchResponse.builder()
                            .matchScore(50)
                            .targetJob(defaultText(request.getTargetJob(), "분석된 직무"))
                            .analysis("스킬 갭 분석 결과를 불러오지 못했습니다.")
                            .learningDirection("채용공고의 필수 기술을 기준으로 부족한 역량을 우선 보완하세요.")
                            .missingSkills(defaultList(request.getRequiredSkills()).stream()
                                    .limit(3)
                                    .map(skill -> skillScore(skill, 30, "채용공고 필수 기술입니다.", "fallback", "high"))
                                    .toList())
                            .build(),
                    resumeSkills,
                    request
            );
        }
    }

    private GapMatchResponse normalizeResponse(GapMatchResponse response, List<String> resumeSkills, GapMatchRequest request) {
        List<SkillScoreDto> ownedSkills = normalizeSkillScores(response.getOwnedSkills());
        if (ownedSkills.size() < 5) {
            ownedSkills = mergeOwnedSkills(ownedSkills, resumeSkills);
        }

        List<SkillScoreDto> missingSkills = normalizeMissingSkills(response.getMissingSkills());

        return GapMatchResponse.builder()
                .matchScore(clampScore(response.getMatchScore() > 0 ? response.getMatchScore() : estimateScore(response)))
                .targetJob(defaultText(response.getTargetJob(), defaultText(request.getTargetJob(), "분석된 직무")))
                .analysis(defaultText(response.getAnalysis(), "이력서와 채용공고를 비교한 스킬 갭 분석 결과입니다."))
                .learningDirection(defaultText(response.getLearningDirection(), "부족한 핵심 스킬을 우선순위에 따라 학습하세요."))
                .ownedSkills(ownedSkills)
                .matchedSkills(normalizeSkillScores(response.getMatchedSkills()))
                .partialSkills(normalizeSkillScores(response.getPartialSkills()))
                .missingSkills(missingSkills)
                .requiredSkills(defaultList(response.getRequiredSkills()).isEmpty() ? defaultList(request.getRequiredSkills()) : defaultList(response.getRequiredSkills()))
                .preferredSkills(defaultList(response.getPreferredSkills()).isEmpty() ? defaultList(request.getPreferredSkills()) : defaultList(response.getPreferredSkills()))
                .mainTasks(defaultList(response.getMainTasks()).isEmpty() ? defaultList(request.getMainTasks()) : defaultList(response.getMainTasks()))
                .jobKeywords(defaultList(response.getJobKeywords()).isEmpty() ? defaultList(request.getKeywords()) : defaultList(response.getJobKeywords()))
                .jobSummary(defaultText(response.getJobSummary(), defaultText(request.getSummary(), "")))
                .build();
    }

    private GapMatchResponse parseFallbackResponse(String content) throws Exception {
        JsonNode root = objectMapper.readTree(cleanJson(content));
        return GapMatchResponse.builder()
                .matchScore(root.path("matchScore").asInt(0))
                .targetJob(root.path("targetJob").asText(""))
                .analysis(root.path("analysis").asText(""))
                .learningDirection(root.path("learningDirection").asText(""))
                .ownedSkills(readSkillScoreArray(root.path("ownedSkills"), 75, "이력서에서 확인된 기술입니다.", "low"))
                .matchedSkills(readSkillScoreArray(root.path("matchedSkills"), 90, "이력서와 채용공고에 모두 포함된 기술입니다.", "low"))
                .partialSkills(readSkillScoreArray(root.path("partialSkills"), 60, "관련 경험이 일부 확인된 기술입니다.", "medium"))
                .missingSkills(readSkillScoreArray(root.path("missingSkills"), 30, "채용공고에는 있으나 이력서 근거가 부족한 기술입니다.", "high"))
                .requiredSkills(readStringArray(root.path("requiredSkills")))
                .preferredSkills(readStringArray(root.path("preferredSkills")))
                .mainTasks(readStringArray(root.path("mainTasks")))
                .jobKeywords(readStringArray(root.path("jobKeywords")))
                .jobSummary(root.path("jobSummary").asText(""))
                .build();
    }

    private List<SkillScoreDto> readSkillScoreArray(JsonNode node, int defaultScore, String defaultReason, String defaultPriority) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(item -> {
                    if (item.isTextual()) {
                        return skillScore(item.asText(), defaultScore, defaultReason, "fallback", defaultPriority);
                    }

                    return skillScore(
                            item.path("name").asText(item.path("skill").asText("")),
                            item.path("score").asInt(defaultScore),
                            item.path("reason").asText(defaultReason),
                            item.path("evidence").asText("fallback"),
                            item.path("priority").asText(defaultPriority)
                    );
                })
                .filter(item -> item.getName() != null && !item.getName().isBlank())
                .toList();
    }

    private List<String> readStringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private int estimateScore(GapMatchResponse response) {
        int matched = safeSize(response.getMatchedSkills());
        int partial = safeSize(response.getPartialSkills());
        int missing = safeSize(response.getMissingSkills());
        int denominator = Math.max(matched + partial + missing, 1);
        return Math.round(((matched + partial * 0.5f) / denominator) * 100);
    }

    private List<SkillScoreDto> normalizeSkillScores(List<SkillScoreDto> values) {
        return defaultList(values).stream()
                .filter(item -> item.getName() != null && !item.getName().trim().isEmpty())
                .map(item -> skillScore(
                        item.getName().trim(),
                        clampScore(item.getScore()),
                        defaultText(item.getReason(), ""),
                        defaultText(item.getEvidence(), ""),
                        normalizePriority(item.getPriority())
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                item -> item.getName().toLowerCase(),
                                item -> item,
                                (first, ignored) -> first,
                                java.util.LinkedHashMap::new
                        ),
                        map -> List.copyOf(map.values())
                ));
    }

    private List<SkillScoreDto> normalizeMissingSkills(List<SkillScoreDto> values) {
        List<SkillScoreDto> normalized = normalizeSkillScores(values);
        if (normalized.isEmpty()) {
            return normalized;
        }

        boolean allSameScore = normalized.stream()
                .map(SkillScoreDto::getScore)
                .distinct()
                .count() == 1;

        if (!allSameScore) {
            return normalized.stream()
                    .map(item -> skillScore(
                            item.getName(),
                            clampMissingScore(item.getScore()),
                            item.getReason(),
                            item.getEvidence(),
                            item.getPriority()
                    ))
                    .toList();
        }

        int[] fallbackScores = {30, 35, 40, 45, 50, 55, 60};
        return java.util.stream.IntStream.range(0, normalized.size())
                .mapToObj(index -> {
                    SkillScoreDto item = normalized.get(index);
                    return skillScore(
                            item.getName(),
                            fallbackScores[Math.min(index, fallbackScores.length - 1)],
                            item.getReason(),
                            item.getEvidence(),
                            item.getPriority()
                    );
                })
                .toList();
    }

    private List<SkillScoreDto> mergeOwnedSkills(List<SkillScoreDto> ownedSkills, List<String> resumeSkills) {
        java.util.LinkedHashMap<String, SkillScoreDto> merged = new java.util.LinkedHashMap<>();

        for (SkillScoreDto skill : ownedSkills) {
            merged.put(skill.getName().toLowerCase(), skill);
        }

        for (String skillName : defaultList(resumeSkills)) {
            if (skillName == null || skillName.isBlank()) {
                continue;
            }

            merged.putIfAbsent(
                    skillName.toLowerCase(),
                    skillScore(skillName, 75, "이력서에서 확인된 기술 스택입니다.", "resumeSkills", "low")
            );

            if (merged.size() >= 5) {
                break;
            }
        }

        return merged.values().stream().limit(8).toList();
    }

    private int clampMissingScore(int score) {
        return Math.max(20, Math.min(score, 60));
    }

    private SkillScoreDto skillScore(String name, int score, String reason, String evidence, String priority) {
        return SkillScoreDto.builder()
                .name(name)
                .score(clampScore(score))
                .reason(reason)
                .evidence(evidence)
                .priority(normalizePriority(priority))
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

    private int clampScore(int score) {
        return Math.max(0, Math.min(score, 100));
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String normalizePriority(String priority) {
        Set<String> allowed = Set.of("high", "medium", "low");
        if (priority == null || !allowed.contains(priority.toLowerCase())) {
            return "medium";
        }
        return priority.toLowerCase();
    }

    private String maskApiKey(String value) {
        if (value == null || value.isBlank()) {
            return "(empty)";
        }

        String trimmed = value.trim();
        int visibleLength = Math.min(7, trimmed.length());
        return trimmed.substring(0, visibleLength) + "...****";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...(truncated)";
    }
}
