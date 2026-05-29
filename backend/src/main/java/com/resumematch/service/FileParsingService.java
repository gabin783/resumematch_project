package com.resumematch.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class FileParsingService {

    // Exception뿐만 아니라 치명적 오류(Error)도 던질 수 있도록 throws Exception을 유지합니다.
    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return "";
        }

        filename = filename.toLowerCase();

        // 1. PDF 파일일 경우 (기존 PDFBox 사용)
        if (filename.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                return pdfStripper.getText(document);
            }
        }
        // 2. 워드 파일(.docx)일 경우 (Apache POI 사용 및 예열 확인용 로그 추가 ✨)
        else if (filename.endsWith(".docx")) {
            System.out.println("--> ⏳ 워드 엔진(POI) 초기화 및 텍스트 추출 시작... (첫 실행 시 5~10초 소요될 수 있습니다)");

            try (InputStream is = file.getInputStream();
                 XWPFDocument document = new XWPFDocument(is);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

                String result = extractor.getText();
                System.out.println("--> ✅ 워드 파일 텍스트 추출 성공!");
                return result;
            }
        }
        // 3. 지원하지 않는 파일
        else {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. PDF 또는 DOCX 파일만 업로드 가능합니다.");
        }
    }
}