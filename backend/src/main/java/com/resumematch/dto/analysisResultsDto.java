package com.resumematch.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.resumematch.entity.AnalysisResult;
import com.resumematch.entity.Member;
import com.resumematch.entity.Resume;
import com.resumematch.entity.Roadmap; // ✨ 임포트 추가

import java.util.List;

@Getter
@Setter
@Builder
public class analysisResultsDto {
    private Member profile;
    private List<Resume> resumes;
    private List<AnalysisResult> analysisResults;
    private List<Roadmap> roadmaps; // ✨ 마이페이지에 보낼 로드맵 리스트 추가!
}