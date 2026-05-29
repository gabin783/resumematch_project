package com.resumematch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "job_posting")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 현재는 크롤링 테스트이므로 임시 회원 ID를 넣거나, 나중에 실제 스크랩한 유저 ID를 연결합니다.
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // (지금은 비워두거나 간단히 저장)

    @Column(length = 255)
    private String url;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}