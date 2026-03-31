package com.benmake.transafe.document.common.constant;

import lombok.Getter;

/**
 * 文档类型枚举
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Getter
public enum DocumentType {

    PDF("pdf"),
    DOC("doc"),
    DOCX("docx"),
    PPT("ppt"),
    PPTX("pptx"),
    XLS("xls"),
    XLSX("xlsx"),
    TXT("txt"),
    EML("eml"),
    ZIP("zip");

    private final String value;

    DocumentType(String value) {
        this.value = value;
    }

    /**
     * 根据字符串值获取枚举类型
     *
     * @param value 文件类型字符串
     * @return 对应的 DocumentType 或 null
     */
    public static DocumentType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为支持解析的类型
     *
     * @param value 文件类型字符串
     * @return 是否支持
     */
    public static boolean isSupported(String value) {
        return fromValue(value) != null;
    }

    /**
     * 判断是否需要递归解析附件
     *
     * @param value 文件类型字符串
     * @return 是否需要递归解析
     */
    public static boolean needsRecursiveParsing(String value) {
        DocumentType type = fromValue(value);
        return type == EML || type == ZIP;
    }
}
