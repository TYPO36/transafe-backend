package com.benmake.transafe.document.parser;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * 邮件文件解析器（支持EML格式）
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
public class EmailParser implements DocumentParser {

    @Override
    public ParseResult parse(InputStream inputStream, String fileName, String password) {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session, inputStream);

            String content = extractContent(message);
            Map<String, Object> metadata = extractMetadata(message);

            return ParseResult.success(content, metadata);

        } catch (Exception e) {
            log.error("邮件文件解析失败: fileName={}", fileName, e);
            return ParseResult.error(3003, "邮件文件已损坏: " + e.getMessage());
        }
    }

    @Override
    public boolean isPasswordProtected(InputStream inputStream) {
        // EML文件本身不加密，但可能附件加密
        return false;
    }

    /**
     * 提取邮件内容
     */
    private String extractContent(Message message) throws Exception {
        StringBuilder content = new StringBuilder();

        // 添加主题
        String subject = message.getSubject();
        if (subject != null) {
            content.append("主题: ").append(subject).append("\n\n");
        }

        // 获取正文
        Object contentObj = message.getContent();
        if (contentObj instanceof Multipart multipart) {
            content.append(extractMultipartContent(multipart));
        } else if (contentObj instanceof String text) {
            content.append(text);
        }

        return content.toString();
    }

    /**
     * 提取多部分内容
     */
    private String extractMultipartContent(Multipart multipart) throws Exception {
        StringBuilder content = new StringBuilder();
        int partCount = multipart.getCount();

        for (int i = 0; i < partCount; i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object partContent = part.getContent();

            if (partContent instanceof Multipart subMultipart) {
                content.append(extractMultipartContent(subMultipart));
            } else if (partContent instanceof String text) {
                // 跳过HTML标签，只取纯文本
                String cleanText = text.replaceAll("<[^>]*>", "").trim();
                if (!cleanText.isEmpty()) {
                    content.append(cleanText).append("\n\n");
                }
            }
        }

        return content.toString();
    }

    /**
     * 提取邮件元数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetadata(Message message) throws Exception {
        Map<String, Object> metadata = new HashMap<>();

        // 发件人
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            InternetAddress from = (InternetAddress) fromAddresses[0];
            metadata.put("emailFrom", from.getAddress());
            if (from.getPersonal() != null) {
                metadata.put("emailFromName", from.getPersonal());
            }
        }

        // 收件人
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null && toAddresses.length > 0) {
            List<String> toList = new ArrayList<>();
            for (Address addr : toAddresses) {
                InternetAddress to = (InternetAddress) addr;
                toList.add(to.getAddress());
            }
            metadata.put("emailTo", toList);
        }

        // CC
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
        if (ccAddresses != null && ccAddresses.length > 0) {
            List<String> ccList = new ArrayList<>();
            for (Address addr : ccAddresses) {
                InternetAddress cc = (InternetAddress) addr;
                ccList.add(cc.getAddress());
            }
            metadata.put("emailCc", ccList);
        }

        // 主题
        metadata.put("emailSubject", message.getSubject());

        // 日期
        if (message.getSentDate() != null) {
            metadata.put("emailDate", message.getSentDate());
        }

        // 附件列表
        metadata.put("attachments", extractAttachments(message));

        return metadata;
    }

    /**
     * 提取附件列表
     */
    private List<Map<String, Object>> extractAttachments(Message message) throws Exception {
        List<Map<String, Object>> attachments = new ArrayList<>();

        Object contentObj = message.getContent();
        if (contentObj instanceof Multipart multipart) {
            extractAttachmentsFromMultipart(multipart, attachments);
        }

        return attachments;
    }

    /**
     * 从多部分中提取附件
     */
    private void extractAttachmentsFromMultipart(Multipart multipart, List<Map<String, Object>> attachments) throws Exception {
        int partCount = multipart.getCount();

        for (int i = 0; i < partCount; i++) {
            BodyPart part = multipart.getBodyPart(i);

            if (part.getContent() instanceof Multipart subMultipart) {
                extractAttachmentsFromMultipart(subMultipart, attachments);
            } else {
                String disposition = part.getDisposition();
                if (disposition != null && disposition.equalsIgnoreCase(BodyPart.ATTACHMENT)) {
                    Map<String, Object> attachment = new HashMap<>();
                    attachment.put("name", part.getFileName());
                    attachment.put("size", part.getSize());
                    attachment.put("contentType", part.getContentType());
                    attachments.add(attachment);
                }
            }
        }
    }
}
