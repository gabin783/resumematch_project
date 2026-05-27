package resumematch_demo.resumematch_demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resumematch_demo.resumematch_demo.entity.Resume;
import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // (기존) 이 사용자의 '모든' 이력서를 최신순으로 가져오는 메서드
    List<Resume> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // (기존) 이 사용자의 '최신 5개' 이력서만 가져오는 메서드 (MypageController용)
    List<Resume> findTop5ByMemberIdOrderByCreatedAtDesc(Long memberId);

    // ✨ (추가!) 이 사용자의 '가장 최신' 이력서 딱 1개만 가져오는 메서드 (갭 매칭 분석용)
    Resume findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
}