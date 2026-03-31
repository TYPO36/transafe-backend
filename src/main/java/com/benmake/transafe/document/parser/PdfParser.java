package com.benmake.transafe.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF文档解析器
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {

    @Override
    public ParseResult parse(InputStream inputStream, String fileName, String password) {
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 1024);
        try {
            // 检测PDF文件头
            byte[] header = new byte[1024];
            int read = pushbackStream.read(header);
            if (read <= 0) {
                return ParseResult.error(3003, "文件为空或已损坏");
            }
            pushbackStream.unread(header, 0, read);

            String headerStr = new String(header, 0, Math.min(read, 5));
            if (!headerStr.startsWith("%PDF-")) {
                return ParseResult.error(3003, "不是有效的PDF文件");
            }

            PDDocument document = PDDocument.load(pushbackStream, password);

            // 检查是否密码保护
            if (document.isEncrypted() && password == null) {
                document.close();
                return ParseResult.passwordProtected();
            }

            // 提取文本内容
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);

            // 提取元数据
            Map<String, Object> metadata = extractMetadata(document);

            document.close();

            return ParseResult.success(content, metadata);

        } catch (IOException e) {
            log.error("PDF解析失败: fileName={}", fileName, e);
            if (e.getMessage() != null && e.getMessage().contains("Password")) {
                return ParseResult.passwordProtected();
            }
            return ParseResult.error(3003, "PDF文件已损坏: " + e.getMessage());
        }
    }

    @Override
    public boolean isPasswordProtected(InputStream inputStream) {
        try {
            PDDocument document = PDDocument.load(inputStream);
            boolean encrypted = document.isEncrypted();
            document.close();
            return encrypted;
        } catch (IOException e) {
            log.warn("PDF密码检测失败", e);
            return false;
        }
    }

    /**
     * 提取PDF元数据
     */
    private Map<String, Object> extractMetadata(PDDocument document) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            var info = document.getDocumentInformation();
            metadata.put("pageCount", document.getNumberOfPages());
            if (info.getTitle() != null && !info.getTitle().isEmpty()) {
                metadata.put("title", info.getTitle());
            }
            if (info.getAuthor() != null && !info.getAuthor().isEmpty()) {
                metadata.put("author", info.getAuthor());
            }
            if (info.getCreationDate() != null) {
                metadata.put("createdDate", info.getCreationDate().getTime());
            }
            if (info.getModificationDate() != null) {
                metadata.put("modifiedDate", info.getModificationDate().getTime());
            }
        } catch (Exception e) {
            log.warn("提取PDF元数据失败", e);
        }
        return metadata;
    }
}
