package com.resumematch.service;

import com.resumematch.dto.CourseDto;
import com.resumematch.dto.RoadmapRequestDto;
import com.resumematch.dto.RoadmapResponse;
import com.resumematch.dto.RoadmapWeekDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoadmapService {
    private static final Logger log = LoggerFactory.getLogger(RoadmapService.class);

    private static final String DEFAULT_TARGET_JOB = "맞춤형 AI 학습 로드맵";

    private final RoadmapLlmGenerateService roadmapLlmGenerateService;

    private enum ResourceType {
        FREE,
        PAID,
        OFFICIAL
    }

    private record CuratedCourse(
            ResourceType resourceType,
            String title,
            String provider,
            String url,
            List<String> keywords,
            String level,
            String time,
            List<String> tags
    ) {
    }

    private static final List<CuratedCourse> CURATED_COURSES = List.of(
            new CuratedCourse(
                    ResourceType.FREE,
                    "생활코딩 Java 입문",
                    "생활코딩",
                    "https://opentutorials.org/course/1223",
                    List.of("Java", "자바", "프로그래밍기초"),
                    "입문",
                    "Java 기본 문법과 프로그래밍 기초",
                    List.of("Java", "무료", "입문")
            ),
            new CuratedCourse(
                    ResourceType.FREE,
                    "생활코딩 Java 객체지향 프로그래밍",
                    "생활코딩",
                    "https://opentutorials.org/course/4074",
                    List.of("Java", "자바", "OOP", "객체지향", "클래스", "상속", "다형성"),
                    "입문",
                    "객체지향 개념과 Java 클래스 구조",
                    List.of("Java", "OOP", "무료")
            ),
            new CuratedCourse(
                    ResourceType.FREE,
                    "MDN JavaScript Guide",
                    "MDN",
                    "https://developer.mozilla.org/ko/docs/Web/JavaScript/Guide",
                    List.of("JavaScript", "JS", "자바스크립트", "웹", "프론트엔드"),
                    "입문",
                    "JavaScript 핵심 개념 정리",
                    List.of("JavaScript", "무료")
            ),
            new CuratedCourse(
                    ResourceType.FREE,
                    "Pro Git Book 한국어",
                    "Git",
                    "https://git-scm.com/book/ko/v2",
                    List.of("Git", "GitHub", "형상관리", "버전관리"),
                    "입문",
                    "Git 기본 사용법과 브랜치 흐름",
                    List.of("Git", "GitHub", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Oracle Java Documentation",
                    "Oracle",
                    "https://docs.oracle.com/en/java/",
                    List.of("Java", "자바"),
                    "참고",
                    "Java 공식 문서와 API 참고 자료",
                    List.of("Java", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Spring 공식 Guides",
                    "Spring",
                    "https://spring.io/guides",
                    List.of("Spring", "SpringBoot", "Spring Boot", "SpringMVC", "Spring MVC", "RESTAPI", "REST API", "REST", "API", "백엔드"),
                    "입문",
                    "Spring Boot와 REST API 예제",
                    List.of("Spring Boot", "REST API", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Spring Boot Reference Documentation",
                    "Spring",
                    "https://docs.spring.io/spring-boot/index.html",
                    List.of("SpringBoot", "Spring Boot", "Spring", "백엔드"),
                    "참고",
                    "Spring Boot 공식 레퍼런스 문서",
                    List.of("Spring Boot", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Spring Data JPA 공식 문서",
                    "Spring",
                    "https://spring.io/projects/spring-data-jpa",
                    List.of("JPA", "Hibernate", "SpringDataJPA", "엔티티", "데이터베이스"),
                    "참고",
                    "Spring Data JPA 개념과 프로젝트 문서",
                    List.of("JPA", "Spring", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "React 공식 Learn 문서",
                    "React",
                    "https://react.dev/learn",
                    List.of("React", "리액트", "컴포넌트", "프론트엔드"),
                    "입문",
                    "React 컴포넌트와 상태 관리 기초",
                    List.of("React", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "TypeScript Handbook",
                    "TypeScript",
                    "https://www.typescriptlang.org/docs/handbook/intro.html",
                    List.of("TypeScript", "TS", "타입스크립트"),
                    "입문",
                    "TypeScript 핵심 문법과 타입 시스템 공식 문서",
                    List.of("TypeScript", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Redux Toolkit Quick Start",
                    "Redux Toolkit",
                    "https://redux-toolkit.js.org/tutorials/quick-start",
                    List.of("ReduxToolkit", "Redux Toolkit", "Redux", "상태관리"),
                    "입문",
                    "Redux Toolkit 상태 관리 공식 빠른 시작",
                    List.of("Redux Toolkit", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Cypress 첫 E2E 테스트",
                    "Cypress",
                    "https://docs.cypress.io/app/end-to-end-testing/writing-your-first-end-to-end-test",
                    List.of("Cypress", "E2E", "테스트", "UI테스트"),
                    "입문",
                    "Cypress 공식 E2E 테스트 작성 가이드",
                    List.of("Cypress", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Docker Get Started",
                    "Docker",
                    "https://docs.docker.com/get-started/",
                    List.of("Docker", "도커", "컨테이너", "배포"),
                    "입문",
                    "Docker 컨테이너 기본 개념과 실행 흐름",
                    List.of("Docker", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "AWS EC2 시작하기",
                    "AWS",
                    "https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/UserGuide/EC2_GetStarted.html",
                    List.of("AWS", "EC2", "Linux", "리눅스", "클라우드", "배포"),
                    "입문",
                    "EC2 인스턴스 생성과 서버 운영 기초",
                    List.of("AWS", "EC2", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "GitHub Actions 공식 문서",
                    "GitHub",
                    "https://docs.github.com/ko/actions",
                    List.of("CI", "CD", "CICD", "CI/CD", "GitHubActions", "GitHub Actions", "배포자동화", "자동화"),
                    "참고",
                    "GitHub Actions 기반 CI/CD 구성",
                    List.of("CI/CD", "GitHub", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Kubernetes Tutorials",
                    "Kubernetes",
                    "https://kubernetes.io/docs/tutorials/",
                    List.of("Kubernetes", "K8s", "쿠버네티스", "컨테이너오케스트레이션"),
                    "참고",
                    "Kubernetes 공식 튜토리얼",
                    List.of("Kubernetes", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Terraform Tutorials",
                    "Terraform",
                    "https://developer.hashicorp.com/terraform/tutorials",
                    List.of("Terraform", "IaC", "인프라자동화"),
                    "참고",
                    "Terraform 공식 튜토리얼",
                    List.of("Terraform", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Redis Docs",
                    "Redis",
                    "https://redis.io/docs/latest/",
                    List.of("Redis", "Cache", "캐시", "인메모리"),
                    "참고",
                    "Redis 공식 문서",
                    List.of("Redis", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "Apache Kafka Quickstart",
                    "Apache Kafka",
                    "https://kafka.apache.org/quickstart",
                    List.of("Kafka", "ApacheKafka", "메시징", "Event", "이벤트", "스트리밍"),
                    "입문",
                    "Kafka 공식 빠른 시작 가이드",
                    List.of("Kafka", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "MySQL 공식 문서",
                    "MySQL",
                    "https://dev.mysql.com/doc/",
                    List.of("MySQL", "SQL", "Database", "DB", "데이터베이스"),
                    "참고",
                    "MySQL 기본 문서와 SQL 참고 자료",
                    List.of("MySQL", "DB", "공식", "무료")
            ),
            new CuratedCourse(
                    ResourceType.OFFICIAL,
                    "OpenAI API Docs",
                    "OpenAI",
                    "https://platform.openai.com/docs",
                    List.of("OpenAI", "AI", "LLM", "생성형AI", "AI도구"),
                    "참고",
                    "OpenAI API와 LLM 활용 공식 문서",
                    List.of("AI", "LLM", "공식", "무료")
            )
    );

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

        long idCounter = 1L;

        for (RoadmapWeekDto week : roadmap.getWeeks()) {
            List<CourseDto> resources = buildCuratedCourses(week, idCounter);
            idCounter += resources.size();
            week.setRecommendedCourses(resources);
        }
    }

    private List<CourseDto> buildCuratedCourses(RoadmapWeekDto week, long startId) {
        if (week == null) {
            return List.of();
        }

        List<String> normalizedTargets = collectMatchTargets(week).stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeCourseKeyword)
                .toList();

        if (normalizedTargets.isEmpty()) {
            return List.of();
        }

        List<CourseDto> result = new ArrayList<>();
        Set<String> selectedUrls = new LinkedHashSet<>();
        long nextId = startId;

        for (ResourceType resourceType : List.of(ResourceType.FREE, ResourceType.PAID, ResourceType.OFFICIAL)) {
            CuratedCourse course = findFirstMatchingCourse(resourceType, normalizedTargets, selectedUrls);
            if (course == null) {
                continue;
            }

            result.add(toCourseDto(nextId++, course));
            selectedUrls.add(course.url());
        }

        return result;
    }

    private List<String> collectMatchTargets(RoadmapWeekDto week) {
        List<String> matchTargets = new ArrayList<>();

        if (week.getFocusSkills() != null) {
            matchTargets.addAll(week.getFocusSkills());
        }

        if (week.getTitle() != null && !week.getTitle().isBlank()) {
            matchTargets.add(week.getTitle());
        }

        if (week.getGoal() != null && !week.getGoal().isBlank()) {
            matchTargets.add(week.getGoal());
        }

        return matchTargets;
    }

    private CuratedCourse findFirstMatchingCourse(ResourceType resourceType, List<String> normalizedTargets, Set<String> selectedUrls) {
        return CURATED_COURSES.stream()
                .filter(course -> course.resourceType() == resourceType)
                .filter(this::hasValidCourseUrl)
                .filter(course -> !selectedUrls.contains(course.url()))
                .filter(course -> matchesCuratedCourse(course, normalizedTargets))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesCuratedCourse(CuratedCourse course, List<String> normalizedTargets) {
        if (course == null || course.keywords() == null || normalizedTargets == null || normalizedTargets.isEmpty()) {
            return false;
        }

        return course.keywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(this::normalizeCourseKeyword)
                .anyMatch(courseKeyword -> normalizedTargets.stream()
                        .anyMatch(target -> isKeywordMatch(target, courseKeyword)));
    }

    private boolean isKeywordMatch(String target, String courseKeyword) {
        if (target == null || courseKeyword == null || target.isBlank() || courseKeyword.isBlank()) {
            return false;
        }

        if (target.equals(courseKeyword)) {
            return true;
        }

        return target.length() >= 5
                && courseKeyword.length() >= 5
                && (target.contains(courseKeyword) || courseKeyword.contains(target));
    }

    private boolean hasValidCourseUrl(CuratedCourse course) {
        return course != null
                && course.url() != null
                && !course.url().isBlank()
                && !"#".equals(course.url().trim());
    }

    private CourseDto toCourseDto(long id, CuratedCourse course) {
        return CourseDto.builder()
                .id(id)
                .step("보조 강의")
                .title(course.title())
                .provider(course.provider())
                .url(course.url())
                .time(course.time())
                .level(course.level())
                .keyword(firstOrDefault(course.keywords(), course.title()))
                .tags(course.tags())
                .build();
    }

    private String normalizeCourseKeyword(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("/", "")
                .replace(".", "")
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
                            skill + "를 사용한 작은 예제를 구현합니다.",
                            "학습 결과를 GitHub와 이력서 문장으로 정리합니다."
                    ))
                    .completionCriteria(List.of(
                            skill + " 핵심 개념을 설명할 수 있습니다.",
                            skill + "를 사용한 실습 결과물을 만들었습니다.",
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
