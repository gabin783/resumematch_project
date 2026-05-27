package resumematch_demo.resumematch_demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resumematch_demo.resumematch_demo.entity.Member;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // ✨ 카카오 ID로 회원 정보 찾기
    Optional<Member> findByKakaoId(Long kakaoId);
}