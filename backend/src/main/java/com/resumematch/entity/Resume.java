package com.resumematch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 나중에 로그인을 붙일 때를 대비해 memberId를 미리 만들어둡니다.
    // 지금은 무조건 1번으로 저장하면 됩니다!
    private Long memberId;

    private String originalFileName;

    // 분석된 스킬들을 콤마로 연결된 문자열로 저장하거나 별도 테이블로 관리할 수 있습니다.
    // 여기서는 간단하게 문자열로 저장하는 방식을 택하겠습니다.
    @Column(columnDefinition = "TEXT")
    private String skills;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.memberId == null) {
            this.memberId = 1L; // 로그인 기능 전까지는 1번 사용자로 고정!
        }
    }
}