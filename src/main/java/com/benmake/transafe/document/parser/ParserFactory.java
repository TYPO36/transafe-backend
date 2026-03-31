package com.benmake.transafe.document.parser;

import com.benmake.transafe.document.common.constant.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 解析器工厂（策略模式）
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final PdfParser pdfParser;
    private final OfficeParser officeParser;
    private final TextParser textParser;
    private final EmailParser emailParser;

    private Map<DocumentType, DocumentParser> parsers;

    /**
     * 初始化解析器映射
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        parsers = new EnumMap<>(DocumentType.class);
        parsers.put(DocumentType.PDF, pdfParser);
        parsers.put(DocumentType.DOC, officeParser);
        parsers.put(DocumentType.DOCX, officeParser);
        parsers.put(DocumentType.PPT, officeParser);
        parsers.put(DocumentType.PPTX, officeParser);
        parsers.put(DocumentType.XLS, officeParser);
        parsers.put(DocumentType.XLSX, officeParser);
        parsers.put(DocumentType.TXT, textParser);
        parsers.put(DocumentType.EML, emailParser);
        // ZIP文件由解析服务处理，不使用解析器
    }

    /**
     * 获取解析器
     *
     * @param fileType 文件类型
     * @return 对应的解析器，不支持则返回null
     */
    public DocumentParser getParser(String fileType) {
        DocumentType type = DocumentType.fromValue(fileType);
        if (type == null) {
            log.warn("不支持的文件类型: {}", fileType);
            return null;
        }
        return getParser(type);
    }

    /**
     * 获取解析器
     *
     * @param type 文档类型
     * @return 对应的解析器
     */
    public DocumentParser getParser(DocumentType type) {
        return parsers.get(type);
    }

    /**
     * 判断文件类型是否支持解析
     *
     * @param fileType 文件类型
     * @return 是否支持
     */
    public boolean isSupported(String fileType) {
        return getParser(fileType) != null;
    }
}
