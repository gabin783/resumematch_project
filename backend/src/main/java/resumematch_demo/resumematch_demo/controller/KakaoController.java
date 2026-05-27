package resumematch_demo.resumematch_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resumematch_demo.resumematch_demo.entity.Member;
import resumematch_demo.resumematch_demo.service.KakaoService;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 리액트 허용
public class KakaoController {

    private final KakaoService kakaoService;

    // ✨ 리액트에서 카카오 인가 코드를 보내면 받는 API
    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        System.out.println("프론트엔드에서 넘어온 인가 코드: " + code);

        try {
            // 1. 코드를 이용해 진짜 유저 정보를 받아오고 DB에 저장/조회
            Member member = kakaoService.kakaoLogin(code);

            // 2. 성공 시 유저 정보(진짜 memberId 포함)를 프론트엔드로 반환
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("카카오 로그인 실패");
        }
    }
}