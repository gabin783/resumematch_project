package com.resumematch.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OllamaAiService {

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "gemma-kr";

    /**
     * [1단계: 필터용] 키워드만 정제 (초경량 + 시스템 프롬프트 세팅 🚀)
     */
    public String callGemmaDirectly(String prompt) {
        String systemPrompt = "너는 주어진 단어 목록에서 IT 기술 스택(프로그래밍 언어, 프레임워크, DB, 인프라 등)만 콤마(,)로 추출하는 전문 AI야. " +
                "절대 영어 문장으로 요약하거나 부연 설명을 쓰지 마. 목록에 없는 단어는 지어내지 마.";
        return sendToOllama(prompt, systemPrompt, 300, false);
    }

    /**
     * [2단계: 분석용] 이력서-JD 갭 분석
     */
    public String analyzeGapWithGemma(List<String> resumeSkills, String jdText, String targetJob) {
        String systemPrompt = "너는 전문 채용 담당자야. 마크다운(`)이나 설명 없이 무조건 JSON 형식으로만 응답해.";
        String prompt = String.format(
                "[이력서 스킬]: %s\n" +
                        "[채용 공고]: %s\n\n" +
                        "위 스킬과 공고를 비교해서 스킬 갭을 분석해줘. 반드시 아래 JSON 형식으로만 대답해:\n" +
                        "{\n" +
                        "  \"missingSkills\": [\"부족한기술1\", \"부족한기술2\"],\n" +
                        "  \"analysis\": \"이력서와 공고 비교 분석 내용\",\n" +
                        "  \"learningDirection\": \"향후 구체적인 학습 방향\"\n" +
                        "}",
                String.join(", ", resumeSkills), jdText
        );
        return sendToOllama(prompt, systemPrompt, 600, true);
    }

    /**
     * [3단계: 하이브리드 매칭용] 자바가 넘겨준 팩트 데이터로 조언 생성
     * 🚨 학원 컴퓨터(GPU 미작동) 환경을 위해 임시로 Mock(가짜) 응답이 켜져 있습니다.
     */
    public String generateHybridFeedback(int matchRate, List<String> matchedSkills, List<String> missingSkills) {

        // --------------------------------------------------------------------------
        // 🏠 [집에서 사용할 때] 고성능 5070 Ti 그래픽카드가 있을 때는 아래 주석을 해제하세요!
        // --------------------------------------------------------------------------
        /*
        String systemPrompt = "너는 IT 멘토야. 무조건 한국어로 2문장 이내로 아주 짧게 대답해.";
        String prompt = String.format(
                "매칭률: %d%%. 부족한 스킬: %s. 합격 가능성과 보완할 점을 2문장으로 짧게 조언해.",
                matchRate, missingSkills.toString()
        );
        return sendToOllama(prompt, systemPrompt, 150, false);
        */


        // --------------------------------------------------------------------------
        // 🏫 [학원에서 사용할 때] CPU 부하와 멈춤 현상을 방지하기 위한 0.1초 컷 초고속 임시 응답
        // --------------------------------------------------------------------------
        String missingSkillsText = missingSkills.isEmpty() ? "없음" : missingSkills.toString();

        return "현재 매칭률은 " + matchRate + "%로 아주 고무적인 결과를 보이고 있습니다. " +
                "공고 요구 스킬 중 현재 부족한 기술 스택인 " + missingSkillsText + " 부분을 조금만 더 집중적으로 보완하신다면 " +
                "서류 및 기술 면접 통과 확률을 극대화할 수 있을 것입니다. 힘내세요!";
    }

    /**
     * [공통] Ollama 통신 및 시스템 프롬프트 적용
     */
    private String sendToOllama(String prompt, String systemPrompt, int maxTokens, boolean requireJson) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        if (requireJson) {
            requestBody.put("format", "json");
        }

        Map<String, Object> options = new HashMap<>();
        options.put("num_predict", maxTokens);
        options.put("temperature", 0.0);
        options.put("num_ctx", 2048);

        requestBody.put("options", options);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(OLLAMA_API_URL, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            return requireJson ? "{}" : "AI 응답 지연";
        }
        return requireJson ? "{}" : "결과 없음";
    }
}