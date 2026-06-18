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
import java.util.Locale;
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

    private static final List<String> KNOWN_IT_SKILLS = List.of(
            "GitHub Actions", "Google Workspace", "Spring Boot", "ASP.NET", "AWS EC2", "REST API",
            "JavaScript", "TypeScript", "Kubernetes", "Terraform", "Datadog", "ArgoCD",
            "QueryDSL", "PostgreSQL", "MongoDB", "MyBatis", "Next.js", "WebFlux",
            "Docker", "Oracle", "Python", "React", "MySQL", "Redis", "Kafka",
            "Kotlin", "GitHub", "Spring", "Figma", "Nginx", "Flask", ".NET",
            "Java", "JPA", "AWS", "SQL", "Git", "DB", "QA", "AI", "UI", "UX", "C", "R"
    );

    private static final Set<String> SHORT_SKILL_WHITELIST = Set.of(
            "hr", "qa", "ai", "db", "ui", "ux", "sql"
    );

    private static final Set<String> SKILL_WHITELIST = Set.of(
            "hr", "qa", "ai", "db", "ui", "ux", "sql",
            "specialist", "excel", "powerpoint", "google workspace",
            "java", "spring", "spring boot", "react", "mysql", "oracle", "python", "flask",
            "docker", "nginx", "aws", "typescript", "next.js", "jpa", "mybatis", "redis",
            "kubernetes", "git", "figma", "github", "github actions", "javascript", "webflux",
            "rest api", "querydsl", "postgresql", "mongodb", "jenkins", "ci/cd",
            "채용", "채용 프로세스 운영", "이력서 검토", "면접 일정 조율", "지원자 커뮤니케이션",
            "온보딩", "온보딩 운영", "인사 데이터 관리", "근태 관리", "교육 운영",
            "조직문화 프로그램 지원", "문서 작성 및 자료 정리", "채용 플랫폼 운영"
    );

    private static final Set<String> EMAIL_DOMAIN_TOKENS = Set.of(
            "com", ".com", "net", ".net", "org", ".org", "co", "kr", "co.kr", ".co.kr",
            "gmail", "naver", "daum", "kakao", "hanmail", "hotmail", "outlook", "icloud",
            "example", "mail", "email"
    );

    private static final Set<String> LOW_VALUE_STANDALONE_TOKENS = Set.of(
            "google", "workspace"
    );

    private static final Set<String> PERSONAL_NAME_TOKENS = Set.of(
            "kim", "lee", "park", "choi", "jung", "jeong", "kang", "jo", "cho", "yoon",
            "jang", "lim", "im", "han", "oh", "seo", "shin", "kwon", "hwang", "song",
            "hong", "yang", "ko", "go", "moon", "baek", "nam", "ryu", "yu", "yoo",
            "minseo", "minsu", "minho", "jiho", "jisu", "jisoo", "jiwon", "seoyeon",
            "seojun", "seohyun", "hyunwoo", "sujin", "yujin", "eunji", "john", "jane"
    );

    private static final Set<String> SKILL_SECTION_KEYWORDS = Set.of(
            "보유 역량", "핵심 역량", "보유 기술", "기술 스택", "스킬",
            "skills", "skill", "tech stack", "technical skills", "core competencies", "competencies"
    );

    private static final Set<String> TECHNICAL_SKILL_SECTION_KEYWORDS = Set.of(
            "보유 기술", "기술 스택", "스킬", "skills", "skill", "tech stack", "technical skills"
    );

    private static final Set<String> NON_SKILL_SECTION_KEYWORDS = Set.of(
            "경력", "경력사항", "프로젝트", "프로젝트 경험", "주요 업무", "담당 업무",
            "구현 내용", "기능", "설명", "결과", "성과", "학습 방향", "학력", "교육",
            "자격", "자격증", "수상", "어학", "자기소개", "소개", "연락처", "개인정보",
            "인적사항", "경험", "활동", "기타",
            "work experience", "experience", "projects", "education", "certification",
            "certifications", "awards", "contact", "profile", "summary"
    );

    private static final Set<String> LOW_VALUE_SKILL_PHRASES = Set.of(
            "프로젝트", "학습 방향", "오류 분석", "강점", "약점", "기능", "설명", "결과",
            "구현 내용", "주요 업무", "담당 업무"
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
            ResumeParseResponse response = parseLlmResponse(content, resumeText);

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
                    "Resume commercial LLM API request failed: status={}",
                    e.getStatusCode()
            );
            return fallbackAnalyze(resumeText, "commercial LLM API request failed");
        } catch (Exception e) {
            log.error("Resume LLM analyze failed: {}", e.getMessage());
            return fallbackAnalyze(resumeText, "resume LLM analyze failed");
        }
    }

    private String callCommercialLlm(String resumeText) throws Exception {
        log.info(
                "Calling resume commercial LLM API: model={}, baseUrl={}",
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
                너는 이력서 분석 전문가입니다.
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
                - Technology Stack, 기술 스택, Skills, Stack, 보유 역량, 핵심 역량 섹션이 있으면 해당 내용을 최우선으로 반영하세요.
                - 이력서에 명시된 구체 기술 스택과 업무 역량 구문은 누락하지 마세요.
                - 이력서에 근거가 없는 기술이나 역량은 과도하게 추측하지 마세요.

                skills 규칙:
                - skills는 화면에 표시할 핵심 보유 역량 또는 기술 스택 목록입니다.
                - 개발 직무는 구체적인 기술명 위주로, HR/운영/기획 직무는 이력서의 보유 역량 bullet에 적힌 업무 구문 위주로 넣으세요.
                - skills는 최소 8개, 최대 20개까지 추출하세요. 단, 이력서에 명시된 기술이 적으면 있는 만큼만 반환하세요.
                - skills에 넣으면 좋은 값: 프로그래밍 언어, 프레임워크, 라이브러리, 데이터베이스, 클라우드/DevOps 도구, 협업/개발 도구, 구체적인 개발 기술, 구체적인 HR/운영 업무 역량.
                - 예: Java, Spring Boot, JPA, QueryDSL, MySQL, PostgreSQL, Redis, MongoDB, React, TypeScript, Docker, Kubernetes, AWS, Jenkins, GitHub Actions, REST API, Git.
                - HR 예: 채용 프로세스 운영, 이력서 검토, 면접 일정 조율, 지원자 커뮤니케이션, 온보딩 운영, 인사 데이터 관리, 근태 관리, 교육 운영, 조직문화 프로그램 지원, 채용 플랫폼 운영.
                - skills에 넣으면 안 되는 값: 백엔드 개발, 프론트엔드 개발, 풀스택 개발, 서버 개발처럼 너무 넓은 직무명, 또는 협업, 문제 해결처럼 근거가 없는 추상어.

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

    private ResumeParseResponse parseLlmResponse(String content, String resumeText) throws Exception {
        String json = cleanJson(content);
        ResumeParseResponse parsed;
        try {
            parsed = objectMapper.readValue(json, ResumeParseResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse resume LLM JSON response");
            throw e;
        }

        List<String> sectionSkills = extractSectionSkillPhrases(resumeText);
        List<String> technicalSkills = sanitizeSkillList(parsed.getTechnicalSkills());
        List<String> skills = sanitizeSkillList(mergeSkillCandidates(sectionSkills, parsed.getSkills()));
        if (skills.isEmpty()) {
            skills = technicalSkills;
        }
        technicalSkills = sanitizeSkillList(mergeSkillCandidates(sectionSkills, technicalSkills));

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
            List<String> sectionSkills = extractSectionSkillPhrases(resumeText);
            List<String> cleanedKeywords = extractLocalKeywords(resumeText);
            List<String> aiCandidates = cleanedKeywords.stream()
                    .filter(word -> word.matches(".*[a-zA-Z]+.*"))
                    .collect(Collectors.toList());

            List<String> finalSkills = new ArrayList<>(sectionSkills);
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

            List<String> distinctSkills = sanitizeSkillList(finalSkills);
            if (distinctSkills.isEmpty()) {
                distinctSkills = sanitizeSkillList(cleanedKeywords.stream()
                        .filter(word -> word.matches(".*[a-zA-Z]+.*"))
                        .limit(12)
                        .collect(Collectors.toList()));
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
            log.error("Resume local fallback analyze failed: {}", e.getMessage());
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

    private List<String> extractSectionSkillPhrases(String resumeText) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return List.of();
        }

        List<String> candidates = new ArrayList<>();
        boolean inSkillSection = false;
        boolean technicalSkillSection = false;
        int blankLinesInSection = 0;

        for (String rawLine : resumeText.split("\\R+")) {
            String line = normalizeResumeLine(rawLine);
            if (line.isEmpty()) {
                if (inSkillSection && ++blankLinesInSection >= 2) {
                    inSkillSection = false;
                    technicalSkillSection = false;
                }
                continue;
            }
            blankLinesInSection = 0;

            String lineWithoutBullet = stripBulletMarker(line);
            if (isSkillSectionHeader(lineWithoutBullet)) {
                inSkillSection = true;
                technicalSkillSection = isTechnicalSkillSectionHeader(lineWithoutBullet);
                continue;
            }

            if (inSkillSection && isNonSkillSectionHeader(lineWithoutBullet)) {
                inSkillSection = false;
                technicalSkillSection = false;
                continue;
            }

            if (!inSkillSection) {
                continue;
            }

            candidates.addAll(splitSkillLine(lineWithoutBullet, technicalSkillSection));
        }

        return sanitizeSkillList(candidates);
    }

    private List<String> splitSkillLine(String line, boolean technicalSkillSection) {
        String cleaned = cleanSkillPhrase(line);
        if (cleaned.isEmpty() || cleaned.endsWith(":")) {
            return List.of();
        }

        List<String> knownSkills = extractKnownItSkills(cleaned);
        if (technicalSkillSection && !knownSkills.isEmpty()) {
            return knownSkills;
        }

        if (knownSkills.size() >= 2 && looksLikeTechnologyList(cleaned)) {
            return knownSkills;
        }

        if (technicalSkillSection) {
            return List.of();
        }

        if (cleaned.contains(",") || cleaned.contains("，")) {
            return Arrays.stream(cleaned.split("[,，]"))
                    .map(this::cleanSkillPhrase)
                    .filter(value -> !value.isEmpty())
                    .collect(Collectors.toList());
        }

        return List.of(cleaned);
    }

    private List<String> extractKnownItSkills(String line) {
        List<String> found = new ArrayList<>();
        String remaining = " " + line + " ";

        for (String skill : KNOWN_IT_SKILLS) {
            String pattern = "(?i)(?<![A-Za-z0-9+#.])" + skill.replace(".", "\\.").replace("+", "\\+").replace(" ", "\\s+") + "(?![A-Za-z0-9+#.])";
            if (remaining.matches(".*" + pattern + ".*")) {
                found.add(skill);
                remaining = remaining.replaceAll(pattern, " ");
            }
        }

        return found;
    }

    private boolean looksLikeTechnologyList(String line) {
        String normalized = line.replaceAll("[,/|·•]", " ").trim();
        return !normalized.matches(".*[가-힣].*")
                && normalized.split("\\s+").length >= 2
                && !normalized.matches(".*[.!?].*");
    }

    private String normalizeResumeLine(String value) {
        return value == null ? "" : value
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripBulletMarker(String value) {
        return value.replaceFirst("^\\s*(?:#{1,6}\\s*)?(?:[-*•·●○▪▫■□◆◇▶▷]|\\d+[.)]|[가-힣][.)])\\s+", "").trim();
    }

    private String cleanSkillPhrase(String value) {
        String cleaned = normalizeResumeLine(value)
                .replaceFirst("^[\\[【(]?[A-Za-z가-힣 ]{1,15}[\\]】)]?\\s*[:：]\\s*", "")
                .replaceAll("\\s*(사용|활용|경험)$", "")
                .trim();

        if (cleaned.equalsIgnoreCase("Google Workspace")) {
            return "Google Workspace";
        }

        return cleaned;
    }

    private boolean isSkillSectionHeader(String line) {
        String normalized = normalizeSectionTitle(line);
        return SKILL_SECTION_KEYWORDS.stream().anyMatch(keyword -> {
            String normalizedKeyword = normalizeToken(keyword);
            return normalized.equals(normalizedKeyword)
                    || normalized.startsWith(normalizedKeyword + " ")
                    || normalized.startsWith(normalizedKeyword + "-");
        });
    }

    private boolean isTechnicalSkillSectionHeader(String line) {
        String normalized = normalizeSectionTitle(line);
        return TECHNICAL_SKILL_SECTION_KEYWORDS.stream().anyMatch(keyword -> {
            String normalizedKeyword = normalizeToken(keyword);
            return normalized.equals(normalizedKeyword)
                    || normalized.startsWith(normalizedKeyword + " ")
                    || normalized.startsWith(normalizedKeyword + "-");
        });
    }

    private boolean isNonSkillSectionHeader(String line) {
        String normalized = normalizeSectionTitle(line);
        String compact = normalized.replace(" ", "");
        if (normalized.length() > 40) {
            return false;
        }

        return NON_SKILL_SECTION_KEYWORDS.stream().anyMatch(keyword -> {
            String normalizedKeyword = normalizeToken(keyword);
            return normalized.equals(normalizedKeyword) || compact.equals(normalizedKeyword.replace(" ", ""));
        });
    }

    private String normalizeSectionTitle(String line) {
        return normalizeToken(line)
                .replaceFirst("^#{1,6}\\s*", "")
                .replaceAll("^[\\[【(<{]+|[\\]】)>}]+$", "")
                .replaceAll("[:：]$", "")
                .trim();
    }

    private List<String> mergeSkillCandidates(List<String> preferred, List<String> secondary) {
        List<String> merged = new ArrayList<>();
        if (preferred != null) {
            merged.addAll(preferred);
        }
        if (secondary != null) {
            merged.addAll(secondary);
        }
        return merged;
    }

    private List<String> sanitizeSkillList(List<String> values) {
        return normalizeList(values).stream()
                .filter(this::isUsefulResumeSkill)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private boolean isUsefulResumeSkill(String value) {
        if (value == null) {
            return false;
        }

        String skill = value.trim();
        if (skill.isEmpty() || skill.length() > 60) {
            return false;
        }

        String normalized = normalizeToken(skill);
        if (skill.equals("C") || skill.equals("R") || skill.equals(".NET") || skill.equals("ASP.NET")) {
            return true;
        }

        if (SKILL_WHITELIST.contains(normalized)) {
            return true;
        }

        if (LOW_VALUE_SKILL_PHRASES.contains(normalized)) {
            return false;
        }

        if (skill.matches("(?i).*[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}.*")) {
            return false;
        }

        if (skill.matches("(?i).*(https?://|www\\.|[a-z0-9-]+\\.(com|net|org|co\\.kr|kr|io|dev)).*")) {
            return false;
        }

        if (skill.matches(".*\\d{2,4}[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}.*")) {
            return false;
        }

        if (skill.matches("^\\d+$") || skill.matches("^[\\p{Punct}\\s]+$")) {
            return false;
        }

        if (EMAIL_DOMAIN_TOKENS.contains(normalized)) {
            return false;
        }

        if (LOW_VALUE_STANDALONE_TOKENS.contains(normalized)) {
            return false;
        }

        String noEdgePunctuation = normalized.replaceAll("^[._-]+|[._-]+$", "");
        if (EMAIL_DOMAIN_TOKENS.contains(noEdgePunctuation)) {
            return false;
        }

        if (skill.matches(".*[.@].*") && !isKnownDottedSkill(normalized)) {
            return false;
        }

        if (extractKnownItSkills(skill).size() >= 2) {
            return false;
        }

        if (looksLikeFeatureDescription(skill)) {
            return false;
        }

        if (skill.matches("^[a-z]+$")) {
            if (skill.length() <= 2) {
                return SHORT_SKILL_WHITELIST.contains(normalized);
            }

            if (PERSONAL_NAME_TOKENS.contains(normalized)) {
                return false;
            }
        }

        if (skill.matches("^[A-Za-z]+$") && skill.length() == 1) {
            return false;
        }

        return true;
    }

    private boolean looksLikeFeatureDescription(String value) {
        String normalized = normalizeToken(value);
        boolean hasKnownTech = !extractKnownItSkills(value).isEmpty();

        if (hasKnownTech && normalized.matches(".*\\s(로|으로|에|에서)\\s.*")) {
            return true;
        }

        if (normalized.matches(".*(저장하고|비교하여|정리하고|활용해|활용하여|만드|구현하고|연동하고)$")) {
            return true;
        }

        if (normalized.matches(".*\\s(을|를)\\s+[^\\s]+$")) {
            return true;
        }

        if (normalized.matches(".*(프로젝트로|이력서에|결과물을|요구사항을|분석 결과|이력서 분석).*")) {
            return true;
        }

        return normalized.split("\\s+").length >= 7;
    }

    private boolean isKnownDottedSkill(String normalized) {
        return normalized.equals("next.js") || normalized.equals("node.js") || normalized.equals("vue.js");
    }

    private String normalizeToken(String value) {
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
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

}
