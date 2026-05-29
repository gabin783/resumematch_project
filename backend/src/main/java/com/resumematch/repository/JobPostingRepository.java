package com.resumematch.repository;

import com.resumematch.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    boolean existsBySourceAndExternalJobId(String source, String externalJobId);

    boolean existsByUrl(String url);
}
