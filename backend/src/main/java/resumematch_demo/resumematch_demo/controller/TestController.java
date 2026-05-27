package resumematch_demo.resumematch_demo.controller;

import resumematch_demo.resumematch_demo.service.WantedJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private WantedJobService wantedJobService;

    // 브라우저에서 이 주소로 접속하면 크롤링이 시작됩니다!
    @GetMapping("/api/test/wanted")
    public String testWantedCrawling() {
        wantedJobService.crawlWantedJobs();
        return "백엔드 터미널(콘솔) 창을 확인해 보세요! 원티드 데이터가 성공적으로 출력되었을 겁니다. 😎";
    }
}