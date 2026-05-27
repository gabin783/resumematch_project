package resumematch_demo.resumematch_demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class GapMatchResponse {
    // 프론트엔드 로드맵 API로 바로 넘겨줄 핵심 스킬 배열
    private List<String> missingSkills;

    // 현재 이력서와 공고의 차이점 분석 텍스트
    private String analysis;

    // 앞으로의 학습 방향 텍스트
    private String learningDirection;
}