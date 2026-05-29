package com.resumematch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.resumematch.entity.Roadmap;

import java.util.List;

@Repository
public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    // ✨ 마이페이지용: 특정 사용자의 로드맵을 최신순으로 모두 가져오기
    List<Roadmap> findByMemberIdOrderByCreatedAtDesc(Long memberId);

}