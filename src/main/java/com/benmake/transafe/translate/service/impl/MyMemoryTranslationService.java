package com.benmake.transafe.translate.service.impl;

import com.benmake.transafe.translate.dto.TranslationResult;
import com.benmake.transafe.translate.service.TranslationService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * MyMemory 翻译服务实现
 *
 * <p>MyMemory 是一个免费的机器翻译API</p>
 * <ul>
 *   <li>免费额度：10,000 字符/天（匿名）</li>
 *   <li>注册后可提升至 30,000 字符/天</li>
 *   <li>API文档：https://mymemory.translated.net/doc/spec.php</li>
 * </ul>
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Service("myMemoryTranslationService")
@RequiredArgsConstructor
public class MyMemoryTranslationService implements TranslationService {

    private final RestTemplate restTemplate;

    @Value("${translate.mymemory.email:}")
    private String email;

    /**
     * API端点
     */
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    /**
     * 每日免费字符限制
     */
    private static final int DAILY_LIMIT = 10000;

    /**
     * 单次请求最大字符数
     */
    private static final int MAX_CHARS_PER_REQUEST = 500;

    @Override
    public TranslationResult translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return TranslationResult.success("", 0, getServiceName());
        }

        try {
            // 如果文本超过单次限制，分段翻译
            if (text.length() > MAX_CHARS_PER_REQUEST) {
                return translateLongText(text, sourceLang, targetLang);
            }

            return translateSingle(text, sourceLang, targetLang);
        } catch (Exception e) {
            log.error("MyMemory翻译失败: {}", e.getMessage(), e);
            return TranslationResult.failure("翻译服务异常: " + e.getMessage());
        }
    }

    /**
     * 单次翻译
     */
    private TranslationResult translateSingle(String text, String sourceLang, String targetLang) {
        String langPair = sourceLang + "|" + targetLang;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("q", text)
                .queryParam("langpair", langPair);

        // 添加邮箱可提升配额
        if (email != null && !email.isBlank()) {
            builder.queryParam("de", email);
        }

        String url = builder.toUriString();

        log.debug("MyMemory翻译请求: langPair={}, textLength={}", langPair, text.length());

        ResponseEntity<MyMemoryResponse> response = restTemplate.getForEntity(url, MyMemoryResponse.class);

        if (response.getBody() == null) {
            log.warn("MyMemory响应为空");
            return TranslationResult.failure("翻译服务响应为空");
        }

        MyMemoryResponse body = response.getBody();

        // 检查响应状态
        if (body.responseStatus != null && body.responseStatus != 200) {
            String errorMsg = body.responseDetails != null ? body.responseDetails : "翻译失败";
            log.warn("MyMemory翻译失败: status={}, message={}", body.responseStatus, errorMsg);
            return TranslationResult.failure(errorMsg);
        }

        if (body.responseData == null || body.responseData.translatedText == null) {
            log.warn("MyMemory翻译结果为空");
            return TranslationResult.failure("翻译结果为空");
        }

        String translatedText = body.responseData.translatedText;
        log.info("MyMemory翻译成功: charCount={}", text.length());

        return TranslationResult.success(translatedText, text.length(), getServiceName());
    }

    /**
     * 长文本分段翻译
     */
    private TranslationResult translateLongText(String text, String sourceLang, String targetLang) {
        log.info("长文本分段翻译: totalChars={}", text.length());

        StringBuilder translated = new StringBuilder();
        int totalChars = 0;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + MAX_CHARS_PER_REQUEST, text.length());

            // 尝试在句子或段落边界分割
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastQuestion = text.lastIndexOf('？', end);
                int lastExclaim = text.lastIndexOf('！', end);
                int lastPeriodEn = text.lastIndexOf('.', end);
                int lastNewline = text.lastIndexOf('\n', end);

                int breakPoint = Math.max(Math.max(Math.max(lastPeriod, lastQuestion),
                        Math.max(lastExclaim, lastPeriodEn)), lastNewline);

                if (breakPoint > start) {
                    end = breakPoint + 1;
                }
            }

            String segment = text.substring(start, end);
            TranslationResult segmentResult = translateSingle(segment, sourceLang, targetLang);

            if (!segmentResult.success()) {
                // 分段失败，保留原文
                translated.append(segment);
                log.warn("分段翻译失败，保留原文: start={}, end={}", start, end);
            } else {
                translated.append(segmentResult.translatedText());
                totalChars += segment.length();
            }

            // 添加分隔符（简单处理）
            if (end < text.length()) {
                translated.append("\n");
            }

            start = end;

            // 避免 API 限流
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return TranslationResult.success(translated.toString(), totalChars, getServiceName());
    }

    @Override
    public String getServiceName() {
        return "MyMemory";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getRemainingQuota() {
        // MyMemory 没有提供配额查询API，返回固定值
        return DAILY_LIMIT;
    }

    /**
     * MyMemory API 响应结构
     */
    private static class MyMemoryResponse {
        @JsonProperty("responseStatus")
        private Integer responseStatus;

        @JsonProperty("responseDetails")
        private String responseDetails;

        @JsonProperty("responseData")
        private ResponseData responseData;

        @JsonProperty("quotaFinished")
        private Boolean quotaFinished;

        private static class ResponseData {
            @JsonProperty("translatedText")
            private String translatedText;

            @JsonProperty("match")
            private Double match;
        }
    }
}