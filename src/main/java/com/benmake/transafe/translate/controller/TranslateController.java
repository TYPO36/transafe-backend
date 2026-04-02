package com.benmake.transafe.translate.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.translate.service.TranslateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 翻译控制器
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Tag(name = "翻译", description = "文档翻译相关接口")
@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslateController {

    private final TranslateService translateService;

    /**
     * 翻译已有文档
     */
    @Operation(summary = "翻译已有文档", description = "对已解析的文档进行翻译")
    @PostMapping("/{fileId}")
    public ResponseEntity<ApiResponse<String>> translateDocument(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @RequestParam("targetLang") String targetLang,
            @RequestParam(value = "sourceLang", defaultValue = "auto") String sourceLang,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        String taskId = translateService.translateDocument(fileId, userId, targetLang, sourceLang);
        return ResponseEntity.ok(ApiResponse.success("翻译任务已创建", taskId));
    }
}