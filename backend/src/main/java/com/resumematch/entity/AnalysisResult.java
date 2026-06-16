package com.resumematch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId; // 일단 1L로 고정해서 사용

    private Long resumeId; // 이 분석에 사용된 이력서 ID

    private String targetJob; // 목표 직무

    @Column(columnDefinition = "TEXT")
    private String jdText; // 입력했던 채용 공고

    @Column(columnDefinition = "TEXT")
    private String analysis; // AI의 종합 분석 내용

    @Column(columnDefinition = "TEXT")
    private String learningDirection; // AI의 추천 학습 방향

    private String missingSkills; // 부족한 스킬 (콤마로 구분해서 저장)

    @Column(columnDefinition = "TEXT")
    private String requiredSkills; // 채용공고 분석 필수 스킬(JSON)

    @Column(columnDefinition = "TEXT")
    private String preferredSkills; // 채용공고 분석 우대 스킬(JSON)

    @Column(columnDefinition = "TEXT")
    private String mainTasks; // 채용공고 분석 주요 업무(JSON)

    @Column(columnDefinition = "TEXT")
    private String jobKeywords; // 채용공고 분석 핵심 키워드(JSON)

    @Column(columnDefinition = "TEXT")
    private String jobSummary; // 채용공고 분석 요약

    private LocalDateTime jobAnalyzedAt;

    @Builder.Default
    private Boolean gapDeleted = false; // 매칭 분석 목록에서 숨김 처리

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.memberId == null) this.memberId = 1L;
        if (this.gapDeleted == null) this.gapDeleted = false;
    }
}
