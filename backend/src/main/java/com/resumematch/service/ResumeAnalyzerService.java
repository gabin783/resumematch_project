package com.resumematch.service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token; // ✅ Token 임포트 필수!
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeAnalyzerService {

    private Komoran komoran;

    @PostConstruct
    public void init() {
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);
    }

    public List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        KomoranResult analyzeResultList = komoran.analyze(text);

        // ✅ getNouns() 대신 전체 토큰을 검사하여 영어(SL)도 포함시킵니다.
        return analyzeResultList.getTokenList().stream()
                .filter(token -> {
                    String pos = token.getPos();
                    // NNG(일반명사), NNP(고유명사), SL(외국어/영어) 태그만 통과
                    return pos.equals("NNG") || pos.equals("NNP") || pos.equals("SL");
                })
                .map(Token::getMorph)
                .filter(morph -> {
                    // 영어는 C, R 처럼 1글자도 스킬일 수 있으므로 통과, 한글은 2글자 이상만 통과
                    if (morph.matches("^[a-zA-Z]+$")) return true;
                    return morph.length() >= 2;
                })
                .distinct()
                .collect(Collectors.toList());
    }
}