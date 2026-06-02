package com.resumematch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.dto.ResumeParseResponse;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResumeLlmAnalyzeService {
    private static final Logger log = LoggerFactory.getLogger(ResumeLlmAnalyzeService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> STOP_WORDS = Set.of(
            "이력서", "지원자", "분야", "성명", "이름", "회사", "직무", "학력", "학과", "목차",
            "생년월일", "전화", "메일", "주소", "성격", "소개", "내용", "주제", "일정", "정의",
            "개발", "환경", "기반", "효과", "기획", "용도", "목적", "관리", "운영", "경험",
            "사항", "고객", "역량", "사용", "불편", "고려", "데이터", "수집",
            "기술", "서비스", "파트", "스택", "과정", "프로젝트", "사이트", "구분", "작성"
    );

    private static final List<String> MUST_HAVE_STACKS = List.of(
            "Java", "Spring", "Spring Boot", "React", "MySQL", "Oracle", "Python", "Flask",
            "Docker", "Nginx", "AWS", "TypeScript", "Next.js", "JPA", "MyBatis", "Redis",
            "Kubernetes", "Git", "Figma", "GitHub", "GitHub Actions", "JavaScript", "SQL", "WebFlux"
    );

    private final ResumeAnalyzerService resumeAnalyzerService;
    private final OllamaAiService ollamaAiService;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    public ResumeLlmAnalyzeService(ResumeAnalyzerService resumeAnalyzerService, OllamaAiService ollamaAiService) {
        this.resumeAnalyzerService = resumeAnalyzerService;
        this.ollamaAiService = ollamaAiService;
    }

    public ResumeParseResponse analyze(String resumeText) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            log.warn("Resume LLM analyze skipped: resume text is empty");
            return fallbackAnalyze("", "resume text is empty");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Resume LLM analyze skipped: LLM_API_KEY is empty");
            return fallbackAnalyze(resumeText, "LLM_API_KEY is empty");
        }

        try {
            String content = callCommercialLlm(resumeText.trim());
            ResumeParseResponse response = parseLlmResponse(content);

            if (response.getSkills().isEmpty()) {
                log.warn("Resume LLM analyze returned no skills");
                return fallbackAnalyze(resumeText, "LLM returned no skills");
            }

            log.info(
                    "Resume LLM analyze success: skills={}, keywords={}",
                    response.getSkills().size(),
                    response.getKeywords().size()
            );
            return response;
        } catch (RestClientResponseException e) {
            log.error(
                    "Resume commercial LLM API request failed: status={}, body={}",
                    e.getStatusCode(),
                    truncate(e.getResponseBodyAsString(), 800),
                    e
            );
            return fallbackAnalyze(resumeText, "commercial LLM API request failed");
        } catch (Exception e) {
            log.error("Resume LLM analyze failed: {}", e.getMessage(), e);
            return fallbackAnalyze(resumeText, "resume LLM analyze failed");
        }
    }

    private String callCommercialLlm(String resumeText) throws Exception {
        log.info(
                "Calling resume commercial LLM API: model={}, baseUrl={}, apiKey={}",
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
                                "content", "You are a resume analysis expert. Return JSON only."
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(resumeText)
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

    private String buildPrompt(String resumeText) {
        return """
                너는 IT 이력서 분석 전문가입니다.
                다음 이력서 내용을 분석해서 JSON만 반환하세요.

                반환 형식:
                {
                  "skills": [],
                  "technicalSkills": [],
                  "softSkills": [],
                  "projects": [],
                  "experienceSummary": "",
                  "keywords": [],
                  "recommendedJobTypes": []
                }

                핵심 규칙:
                - JSON 외의 설명, 마크다운, 코드블록은 절대 포함하지 마세요.
                - Technology Stack, 기술 스택, Skills, Stack 섹션이 있으면 해당 내용을 최우선으로 반영하세요.
                - 이력서에 명시된 구체 기술 스택은 누락하지 마세요.
                - 이력서에 근거가 없는 기술은 과도하게 추측하지 마세요.

                skills 규칙:
                - skills는 화면에 표시할 핵심 기술 스택 목록입니다.
                - skills에는 직무명, 역할명, 추상 역량명이 아니라 구체적인 기술명만 넣으세요.
                - skills는 최소 8개, 최대 20개까지 추출하세요. 단, 이력서에 명시된 기술이 적으면 있는 만큼만 반환하세요.
                - skills에 넣으면 좋은 값: 프로그래밍 언어, 프레임워크, 라이브러리, 데이터베이스, 클라우드/DevOps 도구, 협업/개발 도구, 구체적인 개발 기술.
                - 예: Java, Spring Boot, JPA, QueryDSL, MySQL, PostgreSQL, Redis, MongoDB, React, TypeScript, Docker, Kubernetes, AWS, Jenkins, GitHub Actions, REST API, Git.
                - skills에 넣으면 안 되는 값: 백엔드 개발, 프론트엔드 개발, 풀스택 개발, 서버 개발, 데이터베이스 모델링, 시스템 설계, 협업, 문제 해결, 커뮤니케이션.

                필드별 규칙:
                - technicalSkills에는 skills와 유사하게 구체 기술명을 넣으세요.
                - softSkills에는 협업, 문제 해결, 커뮤니케이션 같은 소프트 스킬만 넣으세요.
                - projects에는 주요 프로젝트명을 3~5개 이내로 넣으세요.
                - keywords에는 REST API, 데이터베이스, CI/CD, 클라우드처럼 분석에 참고할 핵심 키워드를 넣으세요.
                - recommendedJobTypes에는 백엔드 개발자, 풀스택 개발자처럼 직무명을 넣으세요. 직무명은 skills에 넣지 마세요.
                - experienceSummary는 한 문장으로 작성하세요.

                표준화 규칙:
                - springboot는 Spring Boot로 표기하세요.
                - mysql은 MySQL로 표기하세요.
                - github actions는 GitHub Actions로 표기하세요.
                - javascript는 JavaScript로 표기하세요.
                - typescript는 TypeScript로 표기하세요.
                - rest api는 REST API로 표기하세요.

                이력서:
                %s
                """.formatted(resumeText);
    }

    private ResumeParseResponse parseLlmResponse(String content) throws Exception {
        String json = cleanJson(content);
        ResumeParseResponse parsed;
        try {
            parsed = objectMapper.readValue(json, ResumeParseResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse resume LLM JSON response. rawResponse={}", truncate(content, 1000), e);
            throw e;
        }

        List<String> technicalSkills = normalizeList(parsed.getTechnicalSkills());
        List<String> skills = normalizeList(parsed.getSkills());
        if (skills.isEmpty()) {
            skills = technicalSkills;
        }

        List<String> keywords = normalizeList(parsed.getKeywords());
        if (keywords.isEmpty()) {
            keywords = skills;
        }

        return ResumeParseResponse.builder()
                .status("success")
                .count(skills.size())
                .skills(skills)
                .keywords(keywords)
                .technicalSkills(technicalSkills.isEmpty() ? skills : technicalSkills)
                .softSkills(normalizeList(parsed.getSoftSkills()))
                .projects(normalizeList(parsed.getProjects()))
                .experienceSummary(defaultText(parsed.getExperienceSummary(), "이력서의 기술 경험을 분석했습니다."))
                .recommendedJobTypes(normalizeList(parsed.getRecommendedJobTypes()))
                .build();
    }

    private ResumeParseResponse fallbackAnalyze(String resumeText, String reason) {
        log.warn("Resume LLM analyze failed, fallback to local analyzer: reason={}", reason);

        try {
            List<String> cleanedKeywords = extractLocalKeywords(resumeText);
            List<String> aiCandidates = cleanedKeywords.stream()
                    .filter(word -> word.matches(".*[a-zA-Z]+.*"))
                    .collect(Collectors.toList());

            List<String> finalSkills = new ArrayList<>();
            if (!aiCandidates.isEmpty()) {
                String prompt = "[목록]: " + String.join(", ", aiCandidates) + "\n\n" +
                        "명령: 이 목록에서 프로그래밍 언어, 프레임워크, DB, 인프라 도구만 골라주세요.\n" +
                        "규칙 1: Blue, Green, List, Return 같은 일반 영어 단어는 제외하세요.\n" +
                        "규칙 2: 설명 없이 단어만 쉼표(,)로 나열하세요.\n" +
                        "결과: ";

                String filteredResult = ollamaAiService.callGemmaDirectly(prompt);
                finalSkills.addAll(Arrays.stream(filteredResult.replace("\n", ",").split(","))
                        .map(String::trim)
                        .filter(this::isLikelySkill)
                        .collect(Collectors.toList()));
            }

            for (String core : MUST_HAVE_STACKS) {
                boolean isInOriginal = cleanedKeywords.stream().anyMatch(k -> k.equalsIgnoreCase(core));
                boolean isAlreadyInResult = finalSkills.stream().anyMatch(s -> s.equalsIgnoreCase(core));
                if (isInOriginal && !isAlreadyInResult) {
                    finalSkills.add(core);
                }
            }

            List<String> distinctSkills = normalizeList(finalSkills);
            if (distinctSkills.isEmpty()) {
                distinctSkills = cleanedKeywords.stream()
                        .filter(word -> word.matches(".*[a-zA-Z]+.*"))
                        .limit(12)
                        .collect(Collectors.toList());
            }

            log.info("Resume local fallback analyze complete: skills={}, keywords={}", distinctSkills.size(), cleanedKeywords.size());
            return ResumeParseResponse.builder()
                    .status("success")
                    .count(distinctSkills.size())
                    .skills(distinctSkills)
                    .keywords(cleanedKeywords)
                    .technicalSkills(distinctSkills)
                    .softSkills(List.of())
                    .projects(List.of())
                    .experienceSummary("상용 LLM 분석에 실패하여 로컬 분석 결과를 표시합니다.")
                    .recommendedJobTypes(List.of())
                    .build();
        } catch (Exception e) {
            log.error("Resume local fallback analyze failed: {}", e.getMessage(), e);
            return ResumeParseResponse.builder()
                    .status("success")
                    .count(0)
                    .skills(List.of())
                    .keywords(List.of())
                    .technicalSkills(List.of())
                    .softSkills(List.of())
                    .projects(List.of())
                    .experienceSummary("이력서 분석 결과를 불러오지 못했습니다.")
                    .recommendedJobTypes(List.of())
                    .build();
        }
    }

    private List<String> extractLocalKeywords(String resumeText) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return List.of();
        }

        return resumeAnalyzerService.extractKeywords(resumeText).stream()
                .map(String::trim)
                .filter(word -> word.length() > 1)
                .filter(word -> !STOP_WORDS.contains(word))
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isLikelySkill(String value) {
        if (value == null || value.trim().isEmpty() || value.length() >= 30) {
            return false;
        }

        return value.matches("^[a-zA-Z0-9 .+#/_-]+$");
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

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
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
