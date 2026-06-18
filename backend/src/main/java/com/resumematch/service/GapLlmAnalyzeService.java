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
                    "Gap commercial LLM API request failed: status={}",
                    e.getStatusCode()
            );
            return fallbackAnalyze(resumeSkills, request, "commercial LLM API request failed");
        } catch (Exception e) {
            log.error("Gap LLM analyze failed: {}", e.getMessage());
            return fallbackAnalyze(resumeSkills, request, "gap LLM analyze failed");
        }
    }

    private String callCommercialLlm(List<String> resumeSkills, GapMatchRequest request) throws Exception {
        log.info(
                "Calling gap commercial LLM API: model={}, baseUrl={}",
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
                  "learningDirection": "Java와 Spring Boot 기본 문법을 먼저 학습하세요.\\nREST API CRUD 프로젝트를 만들어 이력서 근거를 추가하세요.\\nDocker와 Jenkins를 활용한 배포 흐름을 실습하세요.",
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
                - resumeSkills에 Docker가 있으면 Docker는 missingSkills가 아니라 matchedSkills 또는 partialSkills입니다.
                - resumeSkills에 AWS EC2, S3, Lambda 등 AWS 세부 기술이 있으면 AWS는 missingSkills가 아니라 matchedSkills 또는 partialSkills입니다.
                - resumeSkills에 GitHub Actions가 있으면 GitHub Actions는 missingSkills가 아니라 matchedSkills 또는 partialSkills입니다.
                - JD 요구사항에 있고 resumeSkills에도 있으면 matchedSkills 또는 partialSkills로 분류하세요.
                - JD 요구사항과 resumeSkills가 직접 일치하고 프로젝트/경력 근거가 명확하면 matchedSkills로 분류하세요.
                - JD 요구사항에 있고 resumeSkills에는 없지만 관련 기술, 유사 경험, 같은 계열의 프레임워크/도구 경험이 있으면 partialSkills로 분류하세요.
                - JD 요구사항에 있고 resumeSkills 및 관련 경험이 모두 없을 때만 missingSkills로 분류하세요.
                - 기술 스택에 명시되어 있지만 경험 깊이가 부족한 경우는 missingSkills가 아니라 partialSkills로 분류하세요.
                - 실무 경험의 깊이가 부족한 경우에는 missingSkills가 아니라 partialSkills로 분류하고 score를 50~70 사이로 주세요.
                - missingSkills에는 정말 이력서에서 확인되지 않는 기술만 넣으세요.
                - 비슷한 경험이나 관련 기술은 partialSkills에 넣으세요.
                - preferredSkills는 matchScore에 보조적으로만 반영하세요.
                - 이력서에 근거가 없는 기술을 ownedSkills로 판단하지 마세요.
                - ownedSkills에는 이력서 보유 기술 중 핵심 기술을 최소 5개, 최대 8개 넣으세요.
                - ownedSkills는 구체적인 기술명만 사용하세요. 예: Java, Spring Boot, JPA, MySQL, REST API, Git, Docker, AWS.
                - ownedSkills에는 백엔드 개발, 풀스택 개발, 협업, 문제 해결 같은 직무명/추상 역량을 넣지 마세요.

                ownedSkills 점수 기준:
                - ownedSkills score는 보유도 기준입니다. 모두 같은 값이나 전부 80으로 쓰지 마세요.
                - 이력서 기술 스택에 명시되어 있고 프로젝트/경력에서도 사용 근거가 있으면 85~95를 주세요.
                - 기술 스택에 명시되어 있지만 구체 사용 근거가 약하면 70~84를 주세요.
                - 관련 경험은 있으나 직접 기술명이 약하면 55~69를 주세요.
                - 단순 언급 수준이면 40~54를 주세요.
                - 예: Java 90, Spring Boot 88, REST API 82, JPA 75, Git 70처럼 자연스럽게 분산하세요.

                matchedSkills 기준:
                - matchedSkills는 JD 요구 기술과 resumeSkills가 직접 일치하거나 이력서 프로젝트/경력에서 해당 기술 사용 근거가 명확할 때만 사용하세요.
                - matchedSkills score는 75~95 사이로 주세요.
                - 핵심 필수 기술일수록 높은 점수를 주세요.

                partialSkills 기준:
                - partialSkills는 이력서에 관련 기술은 있으나 JD 요구 기술과 정확히 일치하지 않을 때 사용하세요.
                - resumeSkills에 기술명은 있지만 프로젝트/경력 근거가 약할 때 사용하세요.
                - 유사 기술 경험은 있으나 직접 경험이 부족할 때 사용하세요.
                - partialSkills score는 50~75 사이로, missingSkills보다 높고 matchedSkills보다 낮게 설정하세요.

                missingSkills 점수 기준:
                - missingSkills score는 부족도가 아니라 보유도 기준입니다. 낮을수록 더 부족합니다.
                - missingSkills score는 모두 같은 값으로 쓰지 마세요.
                - 이력서에 전혀 없고 관련 경험도 없으면 10~25를 주세요.
                - 관련 개념은 있으나 해당 기술명/경험이 없으면 26~45를 주세요.
                - 유사 기술 경험은 있으나 JD 요구 기술과 직접 일치하지 않으면 46~60을 주세요.
                - missingSkills에는 70점 이상을 주지 마세요.
                - missingSkills priority가 high이면 낮은 score를, low이면 상대적으로 높은 score를 부여하세요.

                matchScore 기준:
                - matchScore는 0~100 사이 정수이며 너무 후하게 주지 마세요.
                - 필수 스킬 직접 일치는 1점, 부분 일치는 0.5점, 미보유는 0점 기준으로 판단하고 우대 스킬 일치는 보너스로만 반영하세요.
                - requiredSkills 직접 일치가 가장 큰 영향을 줍니다.
                - partialSkills는 절반 정도만 반영하세요.
                - preferredSkills는 보너스 정도로만 반영하세요.
                - missingSkills가 많으면 점수를 낮게 주세요.
                - requiredSkills와 preferredSkills 중 resumeSkills와 직접 일치하는 기술이 하나도 없으면 matchScore는 10점 이하로 주세요.
                - 직접 일치하는 개발 기술이 없고 공통 업무 역량만 관련되어 있으면 matchScore는 20점 이하로 주세요.
                - HR/운영/기획 이력서로 개발자 JD를 분석하는 등 직무 불일치가 크면 analysis에 직무 연관성이 낮다고 명시하세요.
                - 프론트엔드 이력서로 백엔드 JD를 분석하면 30~50점대가 자연스럽습니다.
                - 백엔드 이력서로 백엔드 JD를 분석하면 65~85점대가 자연스럽습니다.

                reason/evidence 작성 기준:
                - 각 스킬 항목의 reason, evidence 문장을 반복적으로 만들지 마세요.
                - ownedSkills reason에는 왜 JD 요구사항과 잘 맞는지 작성하세요.
                - ownedSkills evidence에는 이력서의 기술 스택, 프로젝트, 경력 중 어떤 부분이 근거인지 1~2문장으로 구체적으로 작성하세요.
                - ownedSkills evidence가 두 문장이라면 문장 사이에 줄바꿈 문자 \\n을 넣어 읽기 쉽게 작성하세요.
                - ownedSkills evidence가 너무 짧은 단어 나열로 끝나지 않게 하세요.
                - ownedSkills에는 recommendation 필드가 필요하지 않습니다. reason/evidence에 판단 근거를 충분히 담으세요.
                - missingSkills reason에는 JD에서 왜 필요한지 작성하세요.
                - missingSkills evidence에는 이력서에서 부족하다고 판단한 근거를 1~2문장으로 구체적으로 작성하세요.
                - missingSkills evidence가 두 문장이라면 문장 사이에 줄바꿈 문자 \\n을 넣어 읽기 쉽게 작성하세요.
                - missingSkills evidence를 "경험이 없습니다" 같은 짧은 문장 하나로 끝내지 마세요.
                - recommendation이라는 필드는 만들지 마세요. 학습/강화 방향은 reason에 한 문장으로 자연스럽게 포함하세요.

                learningDirection 작성 기준:
                - learningDirection은 반드시 3개의 학습 단계로 작성하세요.
                - learningDirection은 배열이 아니라 하나의 문자열입니다.
                - 각 단계는 줄바꿈 문자 \\n으로 구분하세요.
                - 1줄: 가장 우선 보완해야 할 필수 기술 학습을 행동 중심 문장으로 작성하세요.
                - 2줄: 작은 실습 프로젝트로 이력서 근거를 만드는 행동 중심 문장을 작성하세요.
                - 3줄: 우대 기술 또는 배포/운영 역량을 보완하는 행동 중심 문장을 작성하세요.
                - 한 문장 안에 모든 학습 방향을 몰아서 쓰지 마세요.
                - 예: Java와 Spring Boot 기본 문법을 먼저 학습하세요.\\nREST API CRUD 프로젝트를 만들어 이력서 근거를 추가하세요.\\nDocker와 Jenkins를 활용한 배포 흐름을 실습하세요.

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
            log.error("Failed to parse gap LLM JSON response");
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
            log.error("Gap fallback analyze failed: {}", e.getMessage());
            return normalizeResponse(
                    GapMatchResponse.builder()
                            .matchScore(50)
                            .targetJob(defaultText(request.getTargetJob(), "분석된 직무"))
                            .analysis("스킬 갭 분석 결과를 불러오지 못했습니다.")
                            .learningDirection(buildDefaultLearningDirection(request, List.of()))
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
        List<String> sourceResumeSkills = mergeSourceResumeSkills(resumeSkills, request);
        List<SkillScoreDto> ownedSkills = buildDeterministicOwnedSkills(sourceResumeSkills);
        List<String> requiredSkills = preferredRequestSkills(request.getRequiredSkills(), response.getRequiredSkills());
        List<String> preferredSkills = preferredRequestSkills(request.getPreferredSkills(), response.getPreferredSkills());
        List<SkillScoreDto> matchedSkills = buildDeterministicMatchedSkills(sourceResumeSkills, requiredSkills, preferredSkills);
        List<SkillScoreDto> partialSkills = buildDeterministicPartialSkills(sourceResumeSkills, requiredSkills, preferredSkills, matchedSkills);
        List<SkillScoreDto> missingSkills = buildDeterministicMissingSkills(sourceResumeSkills, requiredSkills, preferredSkills, matchedSkills, partialSkills);
        int recalculatedScore = calculateFinalMatchScore(matchedSkills, partialSkills, missingSkills, requiredSkills, preferredSkills);
        int adjustedScore = adjustScoreForNoDirectMatches(recalculatedScore, matchedSkills, partialSkills, requiredSkills, preferredSkills);
        String finalAnalysis = buildFinalAnalysisSummary(adjustedScore, matchedSkills, partialSkills, missingSkills);
        String finalLearningDirection = buildFinalLearningDirection(matchedSkills, partialSkills, missingSkills, requiredSkills);

        log.info(
                "Gap deterministic result: resumeSkills={}, requiredSkills={}, preferredSkills={}, matchedSkills={}, partialSkills={}, missingSkills={}, matchScore={}",
                sourceResumeSkills.size(),
                requiredSkills.size(),
                preferredSkills.size(),
                matchedSkills.size(),
                partialSkills.size(),
                missingSkills.size(),
                adjustedScore
        );

        return GapMatchResponse.builder()
                .matchScore(adjustedScore)
                .targetJob(defaultText(response.getTargetJob(), defaultText(request.getTargetJob(), "분석된 직무")))
                .analysis(defaultText(finalAnalysis, defaultText(response.getAnalysis(), "이력서와 채용공고를 비교한 스킬 갭 분석 결과입니다.")))
                .learningDirection(finalLearningDirection)
                .ownedSkills(ownedSkills)
                .matchedSkills(matchedSkills)
                .partialSkills(partialSkills)
                .missingSkills(missingSkills)
                .requiredSkills(requiredSkills)
                .preferredSkills(preferredSkills)
                .mainTasks(defaultList(response.getMainTasks()).isEmpty() ? defaultList(request.getMainTasks()) : defaultList(response.getMainTasks()))
                .jobKeywords(defaultList(response.getJobKeywords()).isEmpty() ? defaultList(request.getKeywords()) : defaultList(response.getJobKeywords()))
                .jobSummary(defaultText(response.getJobSummary(), defaultText(request.getSummary(), "")))
                .build();
    }

    private List<SkillScoreDto> buildDeterministicOwnedSkills(List<String> resumeSkills) {
        return defaultList(resumeSkills).stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .map(skill -> skillScore(
                        skill.trim(),
                        80,
                        "이력서에 명시된 보유 기술입니다.",
                        "원본 이력서 스킬 목록에서 확인되었습니다.",
                        "low"
                ))
                .toList();
    }

    private List<SkillScoreDto> buildDeterministicMatchedSkills(
            List<String> resumeSkills,
            List<String> requiredSkills,
            List<String> preferredSkills
    ) {
        Map<String, String> resumeSkillNames = skillNameMap(resumeSkills);
        Set<String> requiredKeys = defaultList(requiredSkills).stream()
                .map(this::normalizeSkillKey)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());
        Map<String, SkillScoreDto> matched = new LinkedHashMap<>();

        List<String> jobSkills = new java.util.ArrayList<>();
        jobSkills.addAll(defaultList(requiredSkills));
        jobSkills.addAll(defaultList(preferredSkills));

        for (String jobSkill : jobSkills) {
            String key = normalizeSkillKey(jobSkill);
            String resumeSkill = resumeSkillNames.get(key);
            if (key.isBlank() || resumeSkill == null || matched.containsKey(key)) {
                continue;
            }

            boolean required = requiredKeys.contains(key);
            matched.put(key, skillScore(
                    resumeSkill,
                    required ? 90 : 85,
                    "이력서와 채용공고에 모두 명시된 기술입니다.",
                    "원본 이력서 스킬과 JD 요구사항이 직접 일치합니다.",
                    required ? "high" : "medium"
            ));
        }

        return List.copyOf(matched.values());
    }

    private List<SkillScoreDto> buildDeterministicPartialSkills(
            List<String> resumeSkills,
            List<String> requiredSkills,
            List<String> preferredSkills,
            List<SkillScoreDto> matchedSkills
    ) {
        Map<String, String> resumeSkillNames = skillNameMap(resumeSkills);
        Set<String> matchedKeys = skillKeySet(matchedSkills);
        Map<String, SkillScoreDto> partial = new LinkedHashMap<>();

        List<String> jobSkills = new java.util.ArrayList<>();
        jobSkills.addAll(defaultList(requiredSkills));
        jobSkills.addAll(defaultList(preferredSkills));

        for (String jobSkill : jobSkills) {
            String key = normalizeSkillKey(jobSkill);
            if (key.isBlank() || matchedKeys.contains(key) || partial.containsKey(key)) {
                continue;
            }

            String relatedResumeSkill = findRelatedResumeSkill(key, resumeSkillNames);
            if (relatedResumeSkill == null) {
                continue;
            }

            partial.put(key, skillScore(
                    jobSkill.trim(),
                    65,
                    "이력서에 JD 요구 기술과 관련된 세부 기술이 확인됩니다.",
                    relatedResumeSkill + " 보유 스킬이 " + jobSkill.trim() + " 요구사항과 관련됩니다.",
                    "medium"
            ));
        }

        return List.copyOf(partial.values());
    }

    private List<SkillScoreDto> buildDeterministicMissingSkills(
            List<String> resumeSkills,
            List<String> requiredSkills,
            List<String> preferredSkills,
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> partialSkills
    ) {
        Set<String> resumeKeys = skillNameMap(resumeSkills).keySet();
        Set<String> matchedKeys = skillKeySet(matchedSkills);
        Set<String> partialKeys = skillKeySet(partialSkills);
        Map<String, SkillScoreDto> missing = new LinkedHashMap<>();

        for (String skill : defaultList(requiredSkills)) {
            String key = normalizeSkillKey(skill);
            if (isResolvedSkill(key, resumeKeys, matchedKeys, partialKeys)) {
                continue;
            }
            missing.putIfAbsent(key, skillScore(
                    skill.trim(),
                    30,
                    "채용공고의 필수 기술이지만 이력서 원본 스킬에서 확인되지 않습니다.",
                    "원본 이력서 스킬 목록과 직접 일치하거나 관련된 보유 기술이 확인되지 않았습니다.",
                    "high"
            ));
        }

        for (String skill : defaultList(preferredSkills)) {
            String key = normalizeSkillKey(skill);
            if (isResolvedSkill(key, resumeKeys, matchedKeys, partialKeys)) {
                continue;
            }
            missing.putIfAbsent(key, skillScore(
                    skill.trim(),
                    45,
                    "채용공고의 우대 기술이지만 이력서 원본 스킬에서 확인되지 않습니다.",
                    "원본 이력서 스킬 목록과 직접 일치하거나 관련된 보유 기술이 확인되지 않았습니다.",
                    "medium"
            ));
        }

        return List.copyOf(missing.values());
    }

    private boolean isResolvedSkill(String key, Set<String> resumeKeys, Set<String> matchedKeys, Set<String> partialKeys) {
        return key.isBlank() || resumeKeys.contains(key) || matchedKeys.contains(key) || partialKeys.contains(key);
    }

    private Map<String, String> skillNameMap(List<String> skills) {
        Map<String, String> skillNames = new LinkedHashMap<>();
        for (String skillName : defaultList(skills)) {
            putSkillName(skillNames, skillName);
        }
        return skillNames;
    }

    private Set<String> skillKeySet(List<SkillScoreDto> skills) {
        return defaultList(skills).stream()
                .map(skill -> normalizeSkillKey(skill.getName()))
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());
    }

    private List<SkillScoreDto> supplementDirectMatchedSkills(
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> ownedSkills,
            List<String> resumeSkills,
            List<String> requiredSkills,
            List<String> preferredSkills
    ) {
        Map<String, String> resumeSkillNames = new LinkedHashMap<>();
        for (String skillName : defaultList(resumeSkills)) {
            putSkillName(resumeSkillNames, skillName);
        }
        for (SkillScoreDto skill : defaultList(ownedSkills)) {
            putSkillName(resumeSkillNames, skill.getName());
        }

        Map<String, SkillScoreDto> merged = new LinkedHashMap<>();
        for (SkillScoreDto skill : defaultList(matchedSkills)) {
            merged.put(normalizeSkillKey(skill.getName()), skill);
        }

        Set<String> requiredKeys = defaultList(requiredSkills).stream()
                .map(this::normalizeSkillKey)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());

        List<String> jobSkills = new java.util.ArrayList<>();
        jobSkills.addAll(defaultList(requiredSkills));
        jobSkills.addAll(defaultList(preferredSkills));

        for (String jobSkill : jobSkills) {
            String key = normalizeSkillKey(jobSkill);
            String resumeSkill = resumeSkillNames.get(key);
            if (key.isBlank() || resumeSkill == null || merged.containsKey(key)) {
                continue;
            }

            boolean required = requiredKeys.contains(key);
            merged.put(key, skillScore(
                    resumeSkill,
                    required ? 90 : 85,
                    "이력서와 채용공고에 모두 명시된 기술입니다.",
                    "이력서 보유 스킬과 JD 요구사항이 직접 일치합니다.",
                    required ? "high" : "medium"
            ));
        }

        return List.copyOf(merged.values());
    }

    private List<SkillScoreDto> supplementRelatedPartialSkills(
            List<SkillScoreDto> partialSkills,
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> ownedSkills,
            List<String> resumeSkills,
            List<String> requiredSkills,
            List<String> preferredSkills
    ) {
        Map<String, String> resumeSkillNames = new LinkedHashMap<>();
        for (String skillName : defaultList(resumeSkills)) {
            putSkillName(resumeSkillNames, skillName);
        }
        for (SkillScoreDto skill : defaultList(ownedSkills)) {
            putSkillName(resumeSkillNames, skill.getName());
        }

        Set<String> matchedKeys = defaultList(matchedSkills).stream()
                .map(skill -> normalizeSkillKey(skill.getName()))
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());

        Map<String, SkillScoreDto> merged = new LinkedHashMap<>();
        for (SkillScoreDto skill : defaultList(partialSkills)) {
            merged.put(normalizeSkillKey(skill.getName()), skill);
        }

        List<String> jobSkills = new java.util.ArrayList<>();
        jobSkills.addAll(defaultList(requiredSkills));
        jobSkills.addAll(defaultList(preferredSkills));

        for (String jobSkill : jobSkills) {
            String jobKey = normalizeSkillKey(jobSkill);
            if (jobKey.isBlank() || matchedKeys.contains(jobKey) || merged.containsKey(jobKey)) {
                continue;
            }

            String relatedResumeSkill = findRelatedResumeSkill(jobKey, resumeSkillNames);
            if (relatedResumeSkill == null) {
                continue;
            }

            merged.put(jobKey, skillScore(
                    jobSkill.trim(),
                    65,
                    "이력서에 JD 요구 기술과 관련된 세부 기술이 확인됩니다.",
                    relatedResumeSkill + " 보유 스킬이 " + jobSkill.trim() + " 요구사항과 관련됩니다.",
                    "medium"
            ));
        }

        return List.copyOf(merged.values());
    }

    private void putSkillName(Map<String, String> skillNames, String skillName) {
        String key = normalizeSkillKey(skillName);
        if (!key.isBlank()) {
            skillNames.putIfAbsent(key, skillName.trim());
        }
    }

    private List<SkillScoreDto> removeMatchedItems(List<SkillScoreDto> values, List<SkillScoreDto> matchedSkills) {
        Set<String> matchedKeys = defaultList(matchedSkills).stream()
                .map(skill -> normalizeSkillKey(skill.getName()))
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());

        if (matchedKeys.isEmpty()) {
            return defaultList(values);
        }

        return defaultList(values).stream()
                .filter(skill -> !matchedKeys.contains(normalizeSkillKey(skill.getName())))
                .toList();
    }

    private List<SkillScoreDto> removeResolvedItems(
            List<SkillScoreDto> values,
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> partialSkills,
            List<String> resumeSkills
    ) {
        List<SkillScoreDto> resolved = new java.util.ArrayList<>();
        resolved.addAll(defaultList(matchedSkills));
        resolved.addAll(defaultList(partialSkills));
        for (String skillName : defaultList(resumeSkills)) {
            if (skillName != null && !skillName.isBlank()) {
                resolved.add(skillScore(skillName, 75, "이력서에 명시된 보유 기술입니다.", "resumeSkills", "low"));
            }
        }
        return removeMatchedItems(values, resolved);
    }

    private int calculateFinalMatchScore(
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> partialSkills,
            List<SkillScoreDto> missingSkills,
            List<String> requiredSkills,
            List<String> preferredSkills
    ) {
        Set<String> matchedKeys = defaultList(matchedSkills).stream()
                .map(skill -> normalizeSkillKey(skill.getName()))
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());
        Set<String> partialKeys = defaultList(partialSkills).stream()
                .map(skill -> normalizeSkillKey(skill.getName()))
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());

        double requiredScore = scoreSkillGroup(requiredSkills, matchedKeys, partialKeys, 75);
        double preferredScore = scoreSkillGroup(preferredSkills, matchedKeys, partialKeys, 25);
        int estimatedScore = estimateScore(GapMatchResponse.builder()
                .matchedSkills(matchedSkills)
                .partialSkills(partialSkills)
                .missingSkills(missingSkills)
                .build());

        return clampScore((int) Math.round(Math.max(estimatedScore, requiredScore + preferredScore)));
    }

    private double scoreSkillGroup(List<String> jobSkills, Set<String> matchedKeys, Set<String> partialKeys, int weight) {
        List<String> normalized = defaultList(jobSkills).stream()
                .map(this::normalizeSkillKey)
                .filter(key -> !key.isBlank())
                .toList();

        if (normalized.isEmpty()) {
            return 0;
        }

        double score = 0;
        for (String key : normalized) {
            if (matchedKeys.contains(key)) {
                score += 1;
            } else if (partialKeys.contains(key)) {
                score += 0.5;
            }
        }

        return (score / normalized.size()) * weight;
    }

    private String buildFinalAnalysisSummary(
            int matchScore,
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> partialSkills,
            List<SkillScoreDto> missingSkills
    ) {
        StringBuilder summary = new StringBuilder();
        List<String> matchedNames = skillNames(matchedSkills, 5);
        List<String> missingNames = skillNames(missingSkills, 5);
        List<String> partialNames = skillNames(partialSkills, 3);

        if (matchedNames.isEmpty()) {
            summary.append("공고의 핵심 요구 기술과 직접 일치하는 보유 기술은 확인되지 않습니다. ");
            summary.append("이력서에는 다른 직무 역량이 확인되지만, 해당 채용공고의 핵심 기술과의 직접 연관성은 낮습니다.");
        } else {
            summary.append(String.join(", ", matchedNames));
            summary.append(" 등 공고 요구 기술과 일치하는 경험이 확인됩니다. ");

            if (matchScore >= 70) {
                summary.append("일부 보완이 필요하지만 전반적인 직무 적합도는 보통 이상입니다.");
            } else if (matchScore >= 40) {
                summary.append("일부 기술은 일치하지만 핵심 기술 보완이 필요합니다.");
            } else {
                summary.append("직접 일치하는 기술이 적어 직무 연관성이 낮습니다.");
            }
        }

        if (!missingNames.isEmpty()) {
            summary.append(" 다만 ");
            summary.append(String.join(", ", missingNames));
            summary.append(" 등은 추가 보완이 필요합니다.");
        } else if (!partialNames.isEmpty()) {
            summary.append(" ");
            summary.append(String.join(", ", partialNames));
            summary.append(" 등은 관련 경험을 더 구체적인 성과나 프로젝트 근거로 보강하면 좋습니다.");
        }

        return summary.toString();
    }

    private List<String> skillNames(List<SkillScoreDto> skills, int limit) {
        return defaultList(skills).stream()
                .map(SkillScoreDto::getName)
                .filter(name -> name != null && !name.isBlank())
                .limit(limit)
                .toList();
    }

    private String buildFinalLearningDirection(
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> partialSkills,
            List<SkillScoreDto> missingSkills,
            List<String> requiredSkills
    ) {
        Set<String> matchedKeys = defaultList(matchedSkills).stream()
                .map(skill -> normalizeSkillKey(skill.getName()))
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());
        List<String> missingNames = skillNamesExcluding(missingSkills, matchedKeys, 10);
        List<String> partialNames = skillNamesExcluding(partialSkills, matchedKeys, 5);
        List<String> lines = new java.util.ArrayList<>();

        if (matchedKeys.isEmpty()) {
            return buildFoundationLearningDirection(missingNames, requiredSkills);
        }

        if (hasSkillKey(missingNames, "Kotlin")) {
            addLearningLine(lines, "Kotlin 기본 문법과 Java와의 차이를 학습하세요.");
        }

        if (hasAnySkillKey(missingNames, List.of("Redis", "Kafka"))) {
            addLearningLine(lines, "Redis 또는 Kafka를 활용한 캐싱/메시징 실습을 진행하세요.");
        }

        if (hasAnySkillKey(missingNames, List.of("Kubernetes", "ArgoCD", "GitHub Actions", "Terraform"))) {
            addLearningLine(lines, "Kubernetes, ArgoCD, GitHub Actions 등 배포 자동화 흐름을 간단한 예제로 실습하세요.");
        }

        for (String skill : missingNames) {
            if (lines.size() >= 3) {
                break;
            }
            if (hasAnySkillKey(List.of(skill), List.of("Kotlin", "Redis", "Kafka", "Kubernetes", "ArgoCD", "GitHub Actions", "Terraform"))) {
                continue;
            }
            addLearningLine(lines, skill + "의 핵심 개념과 기본 사용법을 작은 예제로 학습하세요.");
        }

        for (String skill : partialNames) {
            if (lines.size() >= 3) {
                break;
            }
            addLearningLine(lines, skill + " 관련 경험을 프로젝트 성과나 운영 시나리오로 더 구체화하세요.");
        }

        while (lines.size() < 3) {
            addLearningLine(lines, "부족한 JD 요구 기술을 하나씩 선택해 작은 실습 결과물로 정리하세요.");
        }

        return String.join("\n", lines.subList(0, 3));
    }

    private String buildFoundationLearningDirection(List<String> missingNames, List<String> requiredSkills) {
        List<String> candidates = missingNames.isEmpty() ? defaultList(requiredSkills) : missingNames;
        List<String> lines = new java.util.ArrayList<>();

        if (hasAnySkillKey(candidates, List.of("Java", "Spring Boot"))) {
            addLearningLine(lines, "Java와 Spring Boot 기본 문법을 먼저 학습하세요.");
        }
        if (hasAnySkillKey(candidates, List.of("React", "TypeScript"))) {
            addLearningLine(lines, "React와 TypeScript 기반 화면 구현 기초를 실습하세요.");
        }
        if (hasAnySkillKey(candidates, List.of("MySQL", "DB", "SQL", "REST API"))) {
            addLearningLine(lines, "MySQL과 REST API를 연결한 간단한 CRUD 프로젝트를 만들어 보세요.");
        }

        for (String skill : candidates) {
            if (lines.size() >= 3) {
                break;
            }
            addLearningLine(lines, skill + "의 기본 개념을 학습하고 간단한 실습 예제를 완성하세요.");
        }

        while (lines.size() < 3) {
            addLearningLine(lines, "개발 직무의 핵심 기술을 기초 문법, 웹 API, 데이터베이스 순서로 학습하세요.");
        }

        return String.join("\n", lines.subList(0, 3));
    }

    private List<String> skillNamesExcluding(List<SkillScoreDto> skills, Set<String> excludedKeys, int limit) {
        return defaultList(skills).stream()
                .map(SkillScoreDto::getName)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !excludedKeys.contains(normalizeSkillKey(name)))
                .limit(limit)
                .toList();
    }

    private void addLearningLine(List<String> lines, String line) {
        if (!lines.contains(line)) {
            lines.add(line);
        }
    }

    private boolean hasSkillKey(List<String> skills, String target) {
        String targetKey = normalizeSkillKey(target);
        return defaultList(skills).stream()
                .map(this::normalizeSkillKey)
                .anyMatch(targetKey::equals);
    }

    private boolean hasAnySkillKey(List<String> skills, List<String> targets) {
        return defaultList(targets).stream().anyMatch(target -> hasSkillKey(skills, target));
    }

    private int adjustScoreForNoDirectMatches(
            int rawScore,
            List<SkillScoreDto> matchedSkills,
            List<SkillScoreDto> partialSkills,
            List<String> requiredSkills,
            List<String> preferredSkills
    ) {
        int score = clampScore(rawScore);
        if (!defaultList(matchedSkills).isEmpty()) {
            return score;
        }

        boolean hasJobSkillRequirements = !defaultList(requiredSkills).isEmpty() || !defaultList(preferredSkills).isEmpty();
        if (!hasJobSkillRequirements) {
            return Math.min(score, 20);
        }

        if (defaultList(partialSkills).isEmpty()) {
            return Math.min(score, 10);
        }

        return Math.min(score, 20);
    }

    private String normalizeLearningDirection(String value, GapMatchRequest request, List<SkillScoreDto> missingSkills) {
        String normalized = defaultText(value, "").replace("\\n", "\n").trim();
        List<String> lines = java.util.Arrays.stream(normalized.split("\\R+"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        if (lines.size() >= 3) {
            return String.join("\n", lines.subList(0, 3));
        }

        return buildDefaultLearningDirection(request, missingSkills);
    }

    private String buildDefaultLearningDirection(GapMatchRequest request, List<SkillScoreDto> missingSkills) {
        String primarySkill = defaultList(missingSkills).stream()
                .map(SkillScoreDto::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElseGet(() -> defaultList(request.getRequiredSkills()).stream()
                        .filter(skill -> skill != null && !skill.isBlank())
                        .findFirst()
                        .orElse("필수 기술"));

        String preferredSkill = defaultList(request.getPreferredSkills()).stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .findFirst()
                .orElse("Docker와 Jenkins");

        return primarySkill + " 기본 개념과 문법을 먼저 학습하세요.\n"
                + "REST API CRUD 프로젝트를 만들어 이력서 근거를 추가하세요.\n"
                + preferredSkill + "를 활용한 배포/운영 흐름을 실습하세요.";
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
                                item -> normalizeSkillKey(item.getName()),
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

    private List<String> mergeSourceResumeSkills(List<String> resumeSkills, GapMatchRequest request) {
        java.util.LinkedHashMap<String, String> merged = new java.util.LinkedHashMap<>();

        for (String skillName : defaultList(resumeSkills)) {
            putSkillName(merged, skillName);
        }
        for (String skillName : defaultList(request.getResumeSkills())) {
            putSkillName(merged, skillName);
        }
        for (String skillName : defaultList(request.getTechnicalSkills())) {
            putSkillName(merged, skillName);
        }

        return List.copyOf(merged.values());
    }

    private List<SkillScoreDto> mergeOwnedSkills(List<SkillScoreDto> ownedSkills, List<String> resumeSkills) {
        java.util.LinkedHashMap<String, SkillScoreDto> merged = new java.util.LinkedHashMap<>();

        for (String skillName : defaultList(resumeSkills)) {
            if (skillName == null || skillName.isBlank()) {
                continue;
            }

            merged.putIfAbsent(
                    normalizeSkillKey(skillName),
                    skillScore(skillName, 75, "이력서에서 확인된 기술 스택입니다.", "resumeSkills", "low")
            );

        }

        for (SkillScoreDto skill : ownedSkills) {
            String key = normalizeSkillKey(skill.getName());
            if (!key.isBlank()) {
                merged.putIfAbsent(key, skill);
            }
        }

        return List.copyOf(merged.values());
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

    private List<String> preferredRequestSkills(List<String> requestSkills, List<String> responseSkills) {
        return defaultList(requestSkills).isEmpty() ? defaultList(responseSkills) : defaultList(requestSkills);
    }

    private String normalizeSkillKey(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String findRelatedResumeSkill(String jobKey, Map<String, String> resumeSkillNames) {
        for (Map.Entry<String, String> entry : resumeSkillNames.entrySet()) {
            if (isRelatedSkill(jobKey, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private boolean isRelatedSkill(String jobKey, String resumeKey) {
        if (jobKey.isBlank() || resumeKey.isBlank() || jobKey.equals(resumeKey)) {
            return false;
        }

        return resumeKey.startsWith(jobKey + " ") || jobKey.startsWith(resumeKey + " ");
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

}
