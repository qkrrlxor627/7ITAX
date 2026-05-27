package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.ss.usermodel.*;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ExcelExportHelper {

    static final String MOCK_ADDRESS = "서울특별시 강남구 테헤란로 212, 7층";
    static final String MOCK_BUSINESS_TYPE = "정보통신업 / 소프트웨어 개발";

    public Workbook loadTemplate(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Excel 템플릿을 찾을 수 없습니다: " + resourcePath);
            }
            return WorkbookFactory.create(is);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Excel 템플릿 로드 실패: " + e.getMessage());
        }
    }

    public byte[] workbookToBytes(Workbook workbook) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Excel 파일 생성 실패: " + e.getMessage());
        } finally {
            try { workbook.close(); } catch (IOException ignored) {}
        }
    }

    public void setCellValue(Sheet sheet, int rowIdx, int colIdx, Object value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);

        if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof String str) {
            cell.setCellValue(str);
        } else if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.toString());
        }
    }

    public byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFont(builder, "/fonts/NanumGothic-Regular.ttf", 400);
            registerFont(builder, "/fonts/NanumGothic-Bold.ttf", 700);
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF 생성 실패: " + e.getMessage());
        }
    }

    private void registerFont(PdfRendererBuilder builder, String resourcePath, int weight) {
        builder.useFont(() -> {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new RuntimeException("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return is;
        }, "NanumGothic", weight, BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    public String profileValue(BusinessProfile profile, String field) {
        if (profile == null) return "-";
        return switch (field) {
            case "businessName" -> profile.getBusinessName() != null ? profile.getBusinessName() : "-";
            case "businessRegNumber" -> profile.getBusinessRegNumber() != null ? profile.getBusinessRegNumber() : "-";
            case "industryCode" -> profile.getIndustryCode() != null ? profile.getIndustryCode() : "-";
            default -> "-";
        };
    }
}
