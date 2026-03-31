package com.benmake.transafe.document.service;

import com.benmake.transafe.document.dto.ParseMessageDTO;
import com.benmake.transafe.document.entity.DocumentEntity;

/**
 * 文档解析服务接口
 *
 * @author TYPO
 * @since 2026-03-31
 */
public interface ParseService {

    /**
     * 处理解析消息
     *
     * @param message 解析消息
     */
    void processParse(ParseMessageDTO message);

    /**
     * 处理ZIP压缩包
     *
     * @param doc 文档实体
     * @param message 解析消息
     */
    void processZipFile(DocumentEntity doc, ParseMessageDTO message);
}
