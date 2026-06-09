package com.resumematch.service;

import com.resumematch.dto.CourseDto;
import com.resumematch.dto.RoadmapRequestDto;
import com.resumematch.dto.RoadmapResponse;
import com.resumematch.dto.RoadmapWeekDto;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RoadmapService {
    private static final Logger log = LoggerFactory.getLogger(RoadmapService.class);

    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final int YOUTUBE_SEARCH_LIMIT = 10;
    private static final int RESOURCE_LIMIT_PER_WEEK = 3;
    private static final String DEFAULT_TARGET_JOB = "맞춤형 AI 학습 로드맵";

    @Value("${youtube.api.key:}")
    private String apiKey;

    private final RoadmapLlmGenerateService roadmapLlmGenerateService;

    public RoadmapResponse generateRoadmap(RoadmapRequestDto request) {
        RoadmapResponse roadmap;

        try {
            roadmap = roadmapLlmGenerateService.generate(request);
        } catch (Exception e) {
            log.warn("Roadmap LLM generate failed, using fallback roadmap: {}", e.getMessage());
            roadmap = buildFallbackRoadmap(request);
        }

        attachRecommendedResources(roadmap);
        return roadmap;
    }

    private void attachRecommendedResources(RoadmapResponse roadmap) {
        if (roadmap == null || roadmap.getWeeks() == null) {
            return;
        }

        long idCounter = 1;
        for (RoadmapWeekDto week : roadmap.getWeeks()) {
            List<CourseDto> resources = buildRecommendedResources(week, idCounter);
            idCounter += resources.size();
            week.setRecommendedCourses(resources);
        }
    }

    private List<CourseDto> buildRecommendedResources(RoadmapWeekDto week, long startId) {
        List<CourseDto> resources = new ArrayList<>();
        String primarySkill = firstOrDefault(week.getFocusSkills(), week.getTitle());

        List<CourseDto> youtubeCourses = searchYoutubeCourses(primarySkill, startId);
        resources.addAll(youtubeCourses);

        long nextId = startId + resources.size();

        if (resources.size() < RESOURCE_LIMIT_PER_WEEK) {
            resources.add(buildSearchResource(nextId++, primarySkill, week));
        }

        if (resources.size() < RESOURCE_LIMIT_PER_WEEK) {
            resources.add(buildPracticeResource(nextId++, primarySkill, week));
        }

        if (resources.size() < RESOURCE_LIMIT_PER_WEEK) {
            resources.add(buildReferenceResource(nextId, primarySkill));
        }

        return resources.stream()
                .limit(RESOURCE_LIMIT_PER_WEEK)
                .toList();
    }

    private List<CourseDto> searchYoutubeCourses(String keyword, long startId) {
        if (apiKey == null || apiKey.isBlank() || keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String query = URLEncoder.encode(buildYoutubeSearchKeyword(keyword), StandardCharsets.UTF_8);
            String url = String.format(
                    "%s?part=snippet&q=%s&key=%s&maxResults=%d&type=video&regionCode=KR&relevanceLanguage=ko&safeSearch=moderate&order=relevance",
                    YOUTUBE_API_URL,
                    query,
                    apiKey,
                    YOUTUBE_SEARCH_LIMIT
            );

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) {
                return List.of();
            }

            JSONObject jsonResponse = new JSONObject(response);
            JSONArray items = jsonResponse.optJSONArray("items");
            if (items == null || items.isEmpty()) {
                return List.of();
            }

            List<CourseDto> courses = new ArrayList<>();

            for (int index = 0; index < items.length(); index++) {
                if (!courses.isEmpty()) {
                    break;
                }

                JSONObject item = items.optJSONObject(index);
                if (item == null) {
                    continue;
                }

                JSONObject id = item.optJSONObject("id");
                JSONObject snippet = item.optJSONObject("snippet");
                if (id == null || snippet == null) {
                    continue;
                }

                String videoId = id.optString("videoId", "");
                String title = cleanYoutubeTitle(snippet.optString("title", ""));

                if (videoId.isBlank() || title.isBlank()) {
                    continue;
                }

                if (isBadYoutubeCourse(title)) {
                    log.info("Filtered YouTube course: keyword={}, title={}", keyword, title);
                    continue;
                }

                if (!isRelevantYoutubeCourse(keyword, title)) {
                    log.info("Filtered unrelated YouTube course: keyword={}, title={}", keyword, title);
                    continue;
                }

                courses.add(CourseDto.builder()
                        .id(startId)
                        .step("추천 강의")
                        .title(title)
                        .provider("YouTube")
                        .url("https://www.youtube.com/watch?v=" + videoId)
                        .time("영상 길이에 따름")
                        .level("입문")
                        .keyword(keyword)
                        .tags(List.of(keyword, "강의"))
                        .build());
            }

            return courses;
        } catch (Exception e) {
            log.warn("YouTube course search failed: keyword={}, reason={}", keyword, e.getMessage());
            return List.of();
        }
    }

    private CourseDto buildSearchResource(long id, String keyword, RoadmapWeekDto week) {
        String searchKeyword = buildLearningSearchKeyword(keyword, week);
        String encodedQuery = URLEncoder.encode(searchKeyword, StandardCharsets.UTF_8);

        return CourseDto.builder()
                .id(id)
                .step("추천 검색")
                .title(searchKeyword)
                .provider("검색 키워드")
                .url("https://www.google.com/search?q=" + encodedQuery)
                .time("필요한 자료 선택")
                .level("입문")
                .keyword(keyword)
                .tags(List.of(keyword, "검색"))
                .build();
    }

    private CourseDto buildPracticeResource(long id, String keyword, RoadmapWeekDto week) {
        String title = buildPracticeTitle(keyword, week);

        return CourseDto.builder()
                .id(id)
                .step("실습 과제")
                .title(title)
                .provider("실습 과제")
                .url("#")
                .time("1~2시간")
                .level("실습")
                .keyword(keyword)
                .tags(List.of(keyword, "실습"))
                .build();
    }

    private CourseDto buildReferenceResource(long id, String keyword) {
        String title = buildReferenceTitle(keyword);
        String url = buildReferenceUrl(keyword);

        return CourseDto.builder()
                .id(id)
                .step("참고 자료")
                .title(title)
                .provider("공식 문서/참고 자료")
                .url(url)
                .time("필요한 부분 확인")
                .level("참고")
                .keyword(keyword)
                .tags(List.of(keyword, "문서"))
                .build();
    }

    private String buildYoutubeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "프로그래밍 입문 강의 한국어";
        }

        String normalized = keyword.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("spring")) {
            return "Spring Boot 백엔드 REST API 실습 강의 한국어";
        }

        if (lower.contains("java") || lower.contains("oop") || lower.contains("객체지향")) {
            return "Java 객체지향 입문 강의 한국어";
        }

        if (lower.contains("docker")) {
            return "Docker 배포 실습 강의 한국어";
        }

        if (lower.contains("jpa") || lower.contains("hibernate")) {
            return "Spring Boot JPA 실습 강의 한국어";
        }

        if (lower.contains("kubernetes") || lower.contains("k8s")) {
            return "Kubernetes 기초 강의 한국어";
        }

        if (lower.contains("mysql") || lower.contains("maria") || lower.contains("database") || lower.contains("db")) {
            return "MySQL 데이터베이스 기초 강의 한국어";
        }

        if (lower.contains("rest")) {
            return "REST API 개발 실습 강의 한국어";
        }

        if (lower.contains("ci") || lower.contains("cd") || lower.contains("jenkins")) {
            return "Jenkins CI CD 배포 자동화 강의 한국어";
        }

        return normalized + " 개발 입문 강의 한국어";
    }

    private String buildLearningSearchKeyword(String keyword, RoadmapWeekDto week) {
        String normalized = keyword == null || keyword.isBlank() ? firstOrDefault(week.getFocusSkills(), week.getTitle()) : keyword.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("java") || lower.contains("oop") || lower.contains("객체지향")) {
            return "Java 객체지향 입문 강의 한국어 클래스 인터페이스 상속";
        }

        if (lower.contains("spring")) {
            return "Spring Boot REST API CRUD 실습 강의 한국어";
        }

        if (lower.contains("jpa") || lower.contains("hibernate")) {
            return "Spring Boot JPA 게시판 CRUD 실습 강의 한국어";
        }

        if (lower.contains("docker")) {
            return "Docker Spring Boot 배포 실습 강의 한국어";
        }

        if (lower.contains("kubernetes") || lower.contains("k8s")) {
            return "Kubernetes 기초 배포 실습 강의 한국어";
        }

        if (lower.contains("mysql") || lower.contains("maria") || lower.contains("database") || lower.contains("db")) {
            return "MySQL 데이터베이스 설계 SQL 기초 강의 한국어";
        }

        if (lower.contains("rest")) {
            return "REST API 설계 CRUD 실습 강의 한국어";
        }

        if (lower.contains("ci") || lower.contains("cd") || lower.contains("jenkins")) {
            return "Jenkins CI CD Spring Boot 배포 자동화 강의 한국어";
        }

        return normalized + " 개발 실습 강의 한국어";
    }

    private String buildPracticeTitle(String keyword, RoadmapWeekDto week) {
        String normalized = keyword == null || keyword.isBlank() ? week.getTitle() : keyword.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("java") || lower.contains("oop") || lower.contains("객체지향")) {
            return "Java 콘솔 CRUD 프로그램을 만들고 객체지향 구조로 정리하기";
        }

        if (lower.contains("spring")) {
            return "Spring Boot로 간단한 REST API를 만들고 GitHub에 정리하기";
        }

        if (lower.contains("jpa") || lower.contains("hibernate")) {
            return "JPA 엔티티 관계를 설계하고 게시글 CRUD를 구현하기";
        }

        if (lower.contains("docker")) {
            return "Spring Boot 프로젝트를 Docker 이미지로 빌드하고 실행하기";
        }

        if (lower.contains("kubernetes") || lower.contains("k8s")) {
            return "간단한 애플리케이션을 Kubernetes 로컬 환경에 배포해보기";
        }

        if (lower.contains("mysql") || lower.contains("maria") || lower.contains("database") || lower.contains("db")) {
            return "MySQL 테이블을 설계하고 기본 CRUD 쿼리를 작성하기";
        }

        if (lower.contains("rest")) {
            return "REST API 요청/응답 구조를 설계하고 CRUD 엔드포인트 만들기";
        }

        if (lower.contains("ci") || lower.contains("cd") || lower.contains("jenkins")) {
            return "GitHub Actions 또는 Jenkins로 간단한 빌드 자동화 구성하기";
        }

        return normalized + "를 활용한 작은 실습 결과물 만들기";
    }

    private String buildReferenceTitle(String keyword) {
        String normalized = keyword == null || keyword.isBlank() ? "개발 학습" : keyword.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("java") || lower.contains("oop") || lower.contains("객체지향")) {
            return "Java 공식 튜토리얼과 객체지향 개념 참고";
        }

        if (lower.contains("spring")) {
            return "Spring 공식 가이드에서 REST API 예제 확인";
        }

        if (lower.contains("jpa") || lower.contains("hibernate")) {
            return "Spring Data JPA 공식 문서와 예제 확인";
        }

        if (lower.contains("docker")) {
            return "Docker 공식 Getting Started 문서 확인";
        }

        if (lower.contains("kubernetes") || lower.contains("k8s")) {
            return "Kubernetes 공식 기초 문서 확인";
        }

        if (lower.contains("mysql") || lower.contains("maria") || lower.contains("database") || lower.contains("db")) {
            return "MySQL 공식 문서와 SQL 기본 문법 확인";
        }

        if (lower.contains("rest")) {
            return "REST API 설계 원칙과 예제 확인";
        }

        if (lower.contains("ci") || lower.contains("cd") || lower.contains("jenkins")) {
            return "CI/CD 기본 개념과 Jenkins 문서 확인";
        }

        return normalized + " 공식 문서 또는 신뢰 가능한 튜토리얼 확인";
    }

    private String buildReferenceUrl(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "https://www.google.com/search?q=programming+tutorial";
        }

        String lower = keyword.toLowerCase(Locale.ROOT);

        if (lower.contains("java") || lower.contains("oop") || lower.contains("객체지향")) {
            return "https://docs.oracle.com/javase/tutorial/";
        }

        if (lower.contains("spring")) {
            return "https://spring.io/guides";
        }

        if (lower.contains("jpa") || lower.contains("hibernate")) {
            return "https://spring.io/projects/spring-data-jpa";
        }

        if (lower.contains("docker")) {
            return "https://docs.docker.com/get-started/";
        }

        if (lower.contains("kubernetes") || lower.contains("k8s")) {
            return "https://kubernetes.io/docs/tutorials/";
        }

        if (lower.contains("mysql") || lower.contains("maria") || lower.contains("database") || lower.contains("db")) {
            return "https://dev.mysql.com/doc/";
        }

        if (lower.contains("rest")) {
            return "https://restfulapi.net/";
        }

        if (lower.contains("ci") || lower.contains("cd") || lower.contains("jenkins")) {
            return "https://www.jenkins.io/doc/";
        }

        String query = URLEncoder.encode(keyword + " 공식 문서 튜토리얼", StandardCharsets.UTF_8);
        return "https://www.google.com/search?q=" + query;
    }

    private boolean isBadYoutubeCourse(String title) {
        if (title == null || title.isBlank()) {
            return true;
        }

        String normalized = title.toLowerCase(Locale.ROOT);

        List<String> blockedKeywords = List.of(
                "#shorts",
                "shorts",
                "쇼츠",
                "ㅋㅋ",
                "ㅎㅎ",
                "존내",
                "존나",
                "개발자 특",
                "개발자특",
                "개발자 특징",
                "브이로그",
                "vlog",
                "리액션",
                "reaction",
                "웃긴",
                "짤",
                "밈",
                "meme",
                "썰",
                "하루",
                "일상",
                "premium",
                "bandicam"
        );

        return blockedKeywords.stream().anyMatch(normalized::contains);
    }

    private boolean isRelevantYoutubeCourse(String keyword, String title) {
        if (keyword == null || keyword.isBlank() || title == null || title.isBlank()) {
            return false;
        }

        String key = keyword.toLowerCase(Locale.ROOT);
        String normalizedTitle = title.toLowerCase(Locale.ROOT);

        if (key.contains("java") || key.contains("oop") || key.contains("객체지향")) {
            return containsAny(normalizedTitle, List.of("java", "자바", "객체지향", "oop", "spring"));
        }

        if (key.contains("spring")) {
            return containsAny(normalizedTitle, List.of("spring", "스프링", "spring boot", "스프링부트"));
        }

        if (key.contains("jpa") || key.contains("hibernate")) {
            return containsAny(normalizedTitle, List.of("jpa", "hibernate", "스프링", "spring"));
        }

        if (key.contains("docker")) {
            return containsAny(normalizedTitle, List.of("docker", "도커"));
        }

        if (key.contains("kubernetes") || key.contains("k8s")) {
            return containsAny(normalizedTitle, List.of("kubernetes", "k8s", "쿠버네티스"));
        }

        if (key.contains("mysql") || key.contains("maria") || key.contains("database") || key.contains("db")) {
            return containsAny(normalizedTitle, List.of("mysql", "mariadb", "db", "database", "데이터베이스", "sql"));
        }

        if (key.contains("rest")) {
            return containsAny(normalizedTitle, List.of("rest", "api", "spring", "스프링"));
        }

        if (key.contains("ci") || key.contains("cd") || key.contains("jenkins")) {
            return containsAny(normalizedTitle, List.of("jenkins", "ci", "cd", "배포", "자동화"));
        }

        return normalizedTitle.contains(key);
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private String cleanYoutubeTitle(String title) {
        if (title == null) {
            return "";
        }

        return title
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();
    }

    private RoadmapResponse buildFallbackRoadmap(RoadmapRequestDto request) {
        List<String> missingSkills = defaultList(request.getMissingSkills()).isEmpty()
                ? defaultList(request.getKeywords())
                : defaultList(request.getMissingSkills());
        List<String> skills = missingSkills.isEmpty()
                ? List.of("필수 기술", "REST API", "데이터베이스", "배포", "실전 프로젝트")
                : missingSkills;
        String targetJob = defaultTargetJob(request.getTargetJob());
        List<RoadmapWeekDto> weeks = new ArrayList<>();

        for (int index = 0; index < 5; index++) {
            String skill = skills.get(Math.min(index, skills.size() - 1));
            weeks.add(RoadmapWeekDto.builder()
                    .week(index + 1)
                    .title(index == 4 ? "실전 프로젝트로 경험 정리" : skill + " 집중 학습")
                    .goal(skill + " 역량을 보완해 채용공고 요구사항과 이력서 경험을 연결합니다.")
                    .focusSkills(List.of(skill))
                    .tasks(List.of(
                            skill + " 핵심 개념을 정리합니다.",
                            skill + "를 활용한 작은 예제를 구현합니다.",
                            "학습 결과를 GitHub와 이력서 문장으로 정리합니다."
                    ))
                    .completionCriteria(List.of(
                            skill + " 핵심 개념을 설명할 수 있습니다.",
                            skill + "를 활용한 실습 결과물을 만들었습니다.",
                            "이력서에 반영할 경험 문장을 작성했습니다."
                    ))
                    .selfCheckItems(List.of(
                            skill + "가 채용공고에서 왜 필요한지 설명할 수 있나요?",
                            skill + "를 사용한 작은 예제를 직접 구현했나요?",
                            "학습 결과를 프로젝트 경험 문장으로 정리했나요?"
                    ))
                    .recommendedCourses(List.of())
                    .build());
        }

        return RoadmapResponse.builder()
                .targetJob(targetJob)
                .summary(targetJob + "에 필요한 부족 역량을 5주 동안 보완하는 기본 로드맵입니다.")
                .weeks(weeks)
                .build();
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String defaultTargetJob(String value) {
        return value == null || value.trim().isEmpty() ? DEFAULT_TARGET_JOB : value;
    }

    private String firstOrDefault(List<String> values, String fallback) {
        if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).isBlank()) {
            return fallback;
        }

        return values.get(0);
    }
}