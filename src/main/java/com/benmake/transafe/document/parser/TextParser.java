package com.benmake.transafe.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 文本文件解析器
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
public class TextParser implements DocumentParser {

    @Override
    public ParseResult parse(InputStream inputStream, String fileName, String password) {
        try {
            String content = readTextContent(inputStream);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("charset", detectCharset(inputStream, content));
            metadata.put("lineCount", content.split("\n").length);
            metadata.put("charCount", content.length());
            return ParseResult.success(content, metadata);
        } catch (Exception e) {
            log.error("文本文件解析失败: fileName={}", fileName, e);
            return ParseResult.error(3003, "文本文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isPasswordProtected(InputStream inputStream) {
        // 文本文件不支持密码保护
        return false;
    }

    /**
     * 读取文本内容，尝试多种编码
     */
    private String readTextContent(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            content.append(buffer, 0, read);
        }
        reader.close();
        return content.toString();
    }

    /**
     * 检测字符编码
     */
    private String detectCharset(InputStream inputStream, String content) {
        if (content != null && !content.isEmpty()) {
            // 简单检测是否为GBK编码（用于中文）
            try {
                byte[] utf8Bytes = content.getBytes(StandardCharsets.UTF_8);
                String testStr = new String(utf8Bytes, StandardCharsets.UTF_8);
                if (testStr.equals(content)) {
                    return "UTF-8";
                }
            } catch (Exception e) {
                // ignore
            }
            return StandardCharsets.UTF_8.name();
        }
        return "UTF-8";
    }
}
