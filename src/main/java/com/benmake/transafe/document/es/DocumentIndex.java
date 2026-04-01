package com.benmake.transafe.document.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ES索引文档
 *
 * <p>用于全文搜索和内容检索</p>
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "transafe_documents")
public class DocumentIndex {

    /**
     * MySQL document 表的主键ID
     */
    @Id
    private Long documentId;

    /**
     * 文件唯一标识（UUID）
     */
    @Field(type = FieldType.Keyword)
    private String fileId;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 父文档file_id
     */
    @Field(type = FieldType.Keyword)
    private String parentId;

    /**
     * 根文档file_id
     */
    @Field(type = FieldType.Keyword)
    private String rootId;

    // ==================== 文件元数据 ====================

    /**
     * 文件名（分词）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String fileName;

    /**
     * 文件名（精确值）
     */
    @Field(type = FieldType.Keyword, name = "file_name_keyword")
    private String fileNameKeyword;

    /**
     * 文件大小
     */
    @Field(type = FieldType.Long)
    private Long fileSize;

    /**
     * 文件存储路径
     */
    @Field(type = FieldType.Keyword)
    private String fileStoragePath;

    /**
     * 文件类型
     */
    @Field(type = FieldType.Keyword)
    private String fileType;

    // ==================== 内容（ES 存储） ====================

    /**
     * 文档内容（分词）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    /**
     * 翻译后内容（分词）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String translatedContent;

    // ==================== 解析状态 ====================

    /**
     * 解析状态
     */
    @Field(type = FieldType.Keyword)
    private String parseStatus;

    /**
     * 解析错误码
     */
    @Field(type = FieldType.Integer)
    private Integer parseErrorCode;

    /**
     * 解析错误消息
     */
    @Field(type = FieldType.Text)
    private String parseErrorMessage;

    /**
     * 是否有密码保护
     */
    @Field(type = FieldType.Boolean)
    private Boolean hasPassword;

    /**
     * 是否为附件
     */
    @Field(type = FieldType.Boolean)
    private Boolean isAttachment;

    /**
     * 优先级
     */
    @Field(type = FieldType.Integer)
    private Integer priority;

    // ==================== 元数据 ====================

    /**
     * 元数据
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    /**
     * 邮件元数据
     */
    @Field(type = FieldType.Object)
    private EmailMetadata emailMetadata;

    /**
     * 附件列表
     */
    @Field(type = FieldType.Nested)
    private List<AttachmentInfo> attachments;

    // ==================== 审计字段 ====================

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    /**
     * 邮件元数据内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailMetadata {

        @Field(type = FieldType.Text)
        private String subject;

        @Field(type = FieldType.Keyword)
        private String from;

        @Field(type = FieldType.Keyword)
        private String to;

        @Field(type = FieldType.Keyword)
        private String cc;

        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
        private LocalDateTime date;
    }

    /**
     * 附件信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {

        @Field(type = FieldType.Text)
        private String name;

        @Field(type = FieldType.Long)
        private Long size;

        @Field(type = FieldType.Keyword)
        private String fileId;
    }
}
