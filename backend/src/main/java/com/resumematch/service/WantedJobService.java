package com.resumematch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematch.entity.JobPosting;
import com.resumematch.repository.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WantedJobService {
    private static final Logger log = LoggerFactory.getLogger(WantedJobService.class);
    private static final String SOURCE_WANTED = "WANTED";

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
                for (JsonNode jobNode : dataNode) {
                    String companyName = jobNode.path("company").path("name").asText();
                    String title = jobNode.path("position").asText();
                    String jobId = jobNode.path("id").asText();
                    String jobUrl = "https://www.wanted.co.kr/wd/" + jobId;

                    if (jobPostingRepository.existsBySourceAndExternalJobId(SOURCE_WANTED, jobId)) {
                        continue;
                    }

                    JobPosting jobPosting = new JobPosting();
                    jobPosting.setMemberId(1L);
                    jobPosting.setCompanyName(companyName);
                    jobPosting.setTitle(title);
                    jobPosting.setUrl(jobUrl);
                    jobPosting.setSource(SOURCE_WANTED);
                    jobPosting.setExternalJobId(jobId);
                    // TODO: Replace this placeholder with detailed JD crawling or API integration.
                    jobPosting.setContent(companyName + "의 " + title + " 공고입니다. 원티드에서 수집된 채용공고입니다.");

                    jobPostingRepository.save(jobPosting);
                }
            }
        } catch (Exception e) {
            log.warn("Wanted job fetch failed: {}", e.getMessage());
        }
    }
}
