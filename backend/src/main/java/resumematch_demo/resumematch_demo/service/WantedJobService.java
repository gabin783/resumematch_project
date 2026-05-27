package resumematch_demo.resumematch_demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import resumematch_demo.resumematch_demo.entity.JobPosting;
import resumematch_demo.resumematch_demo.repository.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WantedJobService {

    // ✨ DB와 연결해주는 Repository 주입!
    @Autowired
    private JobPostingRepository jobPostingRepository;

    public void crawlWantedJobs() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://www.wanted.co.kr/api/chaos/navigation/v1/results?job_group_id=518&country=kr&job_sort=job.popularity_order&years=-1&locations=all&limit=20&offset=0";

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode dataNode = rootNode.path("data");

            if (dataNode.isArray()) {
                int saveCount = 0;
                for (JsonNode jobNode : dataNode) {
                    String companyName = jobNode.path("company").path("name").asText();
                    String title = jobNode.path("position").asText();
                    String jobId = jobNode.path("id").asText();
                    String jobUrl = "https://www.wanted.co.kr/wd/" + jobId;

                    // ✨ 파싱한 데이터를 Entity 객체에 담기
                    JobPosting jobPosting = new JobPosting();
                    jobPosting.setMemberId(1L); // 테스트용 임시 유저 ID (1번 유저)
                    jobPosting.setCompanyName(companyName);
                    jobPosting.setTitle(title);
                    jobPosting.setUrl(jobUrl);
                    jobPosting.setContent("원티드 크롤링 공고"); // 일단 임시 내용

                    // ✨ DB에 저장!
                    jobPostingRepository.save(jobPosting);
                    saveCount++;
                }
                System.out.println("🎉 총 " + saveCount + "개의 채용 공고가 DB에 성공적으로 저장되었습니다!");
            }

        } catch (Exception e) {
            System.out.println("🚨 크롤링/저장 실패: " + e.getMessage());
        }
    }
}