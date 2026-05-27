package resumematch_demo.resumematch_demo.repository;

import resumematch_demo.resumematch_demo.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
}