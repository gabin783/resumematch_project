package resumematch_demo.resumematch_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor // 모든 필드를 포함한 생성자를 자동으로 만들어줍니다.
public class JobMatchResponse {
    private int matchRate;             // 매칭률 (%)
    private List<String> matchedSkills; // 보유한 스킬
    private List<String> missingSkills; // 부족한 스킬
    private String aiFeedback;          // 로컬 AI의 피드백 문구
}