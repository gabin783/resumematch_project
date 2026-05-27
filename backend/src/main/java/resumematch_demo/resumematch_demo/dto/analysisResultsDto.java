// resumematch_demo/resumematch_demo/dto/analysisResultsDto.java 파일

package resumematch_demo.resumematch_demo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import resumematch_demo.resumematch_demo.entity.AnalysisResult;
import resumematch_demo.resumematch_demo.entity.Member;
import resumematch_demo.resumematch_demo.entity.Resume;
import resumematch_demo.resumematch_demo.entity.Roadmap; // ✨ 임포트 추가

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