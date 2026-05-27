package resumematch_demo.resumematch_demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import resumematch_demo.resumematch_demo.entity.Member;
import resumematch_demo.resumematch_demo.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final MemberRepository memberRepository;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    // ✨ 프론트엔드에서 넘어온 코드로 로그인을 처리하는 메인 메서드
    public Member kakaoLogin(String code) {
        // 1. "인가 코드"로 카카오 서버에 "액세스 토큰" 요청
        String accessToken = getAccessToken(code);

        // 2. 발급받은 토큰으로 카카오 API를 호출하여 "유저 정보(프로필)" 가져오기
        JsonNode userInfo = getUserInfo(accessToken);

        // 3. 유저 정보에서 필요한 데이터 꺼내기
        Long kakaoId = userInfo.get("id").asLong();
        String nickname = userInfo.get("properties").get("nickname").asText();

        // ✨ 수정 완료: 람다식 에러를 피하기 위해 삼항 연산자로 값을 한 번만 할당합니다.
        JsonNode kakaoAccount = userInfo.get("kakao_account");
        String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : "";

        // 4. DB에 이미 있는 유저인지 확인하고, 없으면 새로 가입시키기
        return memberRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    System.out.println("🎉 새로운 회원이 가입했습니다: " + nickname);
                    Member newMember = Member.builder()
                            .kakaoId(kakaoId)
                            .nickname(nickname)
                            .email(email) // 에러 없이 깔끔하게 저장됩니다!
                            .bio("안녕하세요! " + nickname + "입니다.") // 기본 한줄 소개
                            .build();
                    return memberRepository.save(newMember);
                });
    }

    // 🔑 액세스 토큰 발급 요청 (카카오 공식 문서 규격)
    private String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();

        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카카오 토큰 파싱 실패", e);
        }
    }

    // 👤 유저 정보 가져오기 (카카오 공식 문서 규격)
    private JsonNode getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();

        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoProfileRequest,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카카오 유저 정보 파싱 실패", e);
        }
    }
}