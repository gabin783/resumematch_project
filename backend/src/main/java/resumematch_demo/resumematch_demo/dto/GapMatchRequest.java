package resumematch_demo.resumematch_demo.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GapMatchRequest {
    private List<String> resumeSkills;
    private String jdText;
    private String targetJob;
}