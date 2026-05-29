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

    private String targetJob; // 목표 직무

    @Column(columnDefinition = "TEXT")
    private String jdText; // 입력했던 채용 공고

    @Column(columnDefinition = "TEXT")
    private String analysis; // AI의 종합 분석 내용

    @Column(columnDefinition = "TEXT")
    private String learningDirection; // AI의 추천 학습 방향

    private String missingSkills; // 부족한 스킬 (콤마로 구분해서 저장)

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.memberId == null) this.memberId = 1L;
    }
}