package com.resumematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "roadmaps")
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자의 로드맵인지 식별하기 위한 회원 ID
    @Column(nullable = false)
    private Long memberId;

    // 목표 직무 (예: "Java 백엔드 개발자")
    @Column(nullable = false)
    private String targetJob;

    // 로드맵의 전체 내용 (JSON 문자열이나 긴 텍스트 형태로 저장하기 위해 TEXT 타입 사용)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 생성 일시
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}