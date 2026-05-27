package resumematch_demo.resumematch_demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long kakaoId; // ✨ 카카오 고유 식별 번호 (중복 가입 방지)

    private String email;
    private String nickname; // 카카오에서 받아온 닉네임 (이후 프로필 수정 가능)
    private String bio;      // 한줄 소개
}