package com.benmake.transafe.infra.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 段落文档（ES）
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "paragraph")
public class ParagraphDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String taskId;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String fileId;

    @Field(type = FieldType.Keyword)
    private String paragraphId;

    @Field(type = FieldType.Integer)
    private Integer paragraphIndex;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String originalContent;

    @Field(type = FieldType.Nested)
    private List<Translation> translations = new ArrayList<>();

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Translation {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Text)
        private String selectedText;

        @Field(type = FieldType.Text)
        private String translatedText;

        @Field(type = FieldType.Integer)
        private Integer startIndex;

        @Field(type = FieldType.Integer)
        private Integer endIndex;

        @Field(type = FieldType.Date)
        private LocalDateTime translatedAt;
    }
}