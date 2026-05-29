package com.resumematch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.resumematch.entity.AnalysisResult;

import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}