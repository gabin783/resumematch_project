package resumematch_demo.resumematch_demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import resumematch_demo.resumematch_demo.entity.AnalysisResult;

import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}