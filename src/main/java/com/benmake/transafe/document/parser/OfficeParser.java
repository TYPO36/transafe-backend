package com.benmake.transafe.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Office文档解析器（支持DOCX, PPTX, XLSX）
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
public class OfficeParser implements DocumentParser {

    @Override
    public ParseResult parse(InputStream inputStream, String fileName, String password) {
        String lowerFileName = fileName.toLowerCase();
        try {
            if (lowerFileName.endsWith(".docx")) {
                return parseDocx(inputStream);
            } else if (lowerFileName.endsWith(".pptx")) {
                return parsePptx(inputStream);
            } else if (lowerFileName.endsWith(".xlsx")) {
                return parseXlsx(inputStream);
            } else {
                return ParseResult.error(3002, "不支持的Office文件格式，仅支持DOCX/PPTX/XLSX");
            }
        } catch (Exception e) {
            log.error("Office文档解析失败: fileName={}", fileName, e);
            return ParseResult.error(3003, "Office文件已损坏: " + e.getMessage());
        }
    }

    @Override
    public boolean isPasswordProtected(InputStream inputStream) {
        // Office文档的密码保护检测较复杂，这里简化处理
        return false;
    }

    /**
     * 解析DOCX
     */
    private ParseResult parseDocx(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 1024);
        byte[] header = new byte[1024];
        int read = pushbackStream.read(header);
        if (read <= 0) {
            return ParseResult.error(3003, "文件为空或已损坏");
        }
        pushbackStream.unread(header, 0, read);

        Map<String, Object> metadata = new HashMap<>();
        XWPFDocument document = new XWPFDocument(pushbackStream);
        XWPFWordExtractor extractor = new XWPFWordExtractor(document);
        String content = extractor.getText();
        extractDocxMetadata(document, metadata);
        extractor.close();
        document.close();

        return ParseResult.success(content, metadata);
    }

    /**
     * 解析PPTX
     */
    private ParseResult parsePptx(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 1024);
        byte[] header = new byte[1024];
        int read = pushbackStream.read(header);
        if (read <= 0) {
            return ParseResult.error(3003, "文件为空或已损坏");
        }
        pushbackStream.unread(header, 0, read);

        Map<String, Object> metadata = new HashMap<>();
        XMLSlideShow slideShow = new XMLSlideShow(pushbackStream);
        StringBuilder content = new StringBuilder();
        int slideCount = slideShow.getSlides().size();
        metadata.put("slideCount", slideCount);

        var iterator = slideShow.getSlides().iterator();
        int slideNum = 1;
        while (iterator.hasNext()) {
            var slide = iterator.next();
            content.append("=== Slide ").append(slideNum++).append(" ===\n");
            var shapes = slide.getShapes().iterator();
            while (shapes.hasNext()) {
                var shape = shapes.next();
                if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
                    String text = textShape.getText();
                    if (text != null && !text.isBlank()) {
                        content.append(text).append("\n");
                    }
                }
            }
            content.append("\n");
        }
        slideShow.close();

        return ParseResult.success(content.toString(), metadata);
    }

    /**
     * 解析XLSX
     */
    private ParseResult parseXlsx(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 1024);
        byte[] header = new byte[1024];
        int read = pushbackStream.read(header);
        if (read <= 0) {
            return ParseResult.error(3003, "文件为空或已损坏");
        }
        pushbackStream.unread(header, 0, read);

        Map<String, Object> metadata = new HashMap<>();
        XSSFWorkbook workbook = new XSSFWorkbook(pushbackStream);
        StringBuilder content = new StringBuilder();
        int sheetCount = workbook.getNumberOfSheets();
        metadata.put("sheetCount", sheetCount);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            var sheet = workbook.getSheetAt(i);
            if (sheet == null) continue;

            content.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");
            var rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                var row = rowIterator.next();
                if (row == null) continue;

                var cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    var cell = cellIterator.next();
                    content.append(getCellValue(cell)).append("\t");
                }
                content.append("\n");
            }
            content.append("\n");
        }
        workbook.close();

        return ParseResult.success(content.toString(), metadata);
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * 提取DOCX元数据
     */
    private void extractDocxMetadata(XWPFDocument document, Map<String, Object> metadata) {
        try {
            var coreProps = document.getProperties().getCoreProperties();
            if (coreProps.getTitle() != null) {
                metadata.put("title", coreProps.getTitle());
            }
            if (coreProps.getCreator() != null) {
                metadata.put("author", coreProps.getCreator());
            }
            if (coreProps.getCreated() != null) {
                metadata.put("createdDate", coreProps.getCreated().getTime());
            }
            if (coreProps.getModified() != null) {
                metadata.put("modifiedDate", coreProps.getModified().getTime());
            }
        } catch (Exception e) {
            log.warn("提取DOCX元数据失败", e);
        }
    }
}
