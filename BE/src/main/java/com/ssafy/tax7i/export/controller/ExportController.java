package com.ssafy.tax7i.export.controller;

import com.ssafy.tax7i.export.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excelExportService;

    // ── 부가가치세 Excel (FE: /export/vat?year=&half=) ──

    @GetMapping("/vat")
    public ResponseEntity<byte[]> exportVat(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "1") Integer half) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] excelBytes = excelExportService.exportVatExcel(userId, targetYear, half);
        return excelResponse(excelBytes, "부가세_" + targetYear + "_" + half + "기.xlsx");
    }

    // ── 종합소득세 Excel (FE: /export/income-tax?year=) ──

    @GetMapping("/income-tax")
    public ResponseEntity<byte[]> exportIncomeTax(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] excelBytes = excelExportService.exportIncomeTaxExcel(userId, targetYear);
        return excelResponse(excelBytes, "종합소득세_" + targetYear + ".xlsx");
    }

    // ── 간편장부 PDF (FE: /export/book-entries?year=) ──

    @GetMapping("/book-entries")
    public ResponseEntity<byte[]> exportBookEntries(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] pdfBytes = excelExportService.generateSimpleLedgerPdf(userId, targetYear);
        return pdfResponse(pdfBytes, "간편장부_" + targetYear + ".pdf");
    }

    // ── 별칭 (하위호환) ──

    @GetMapping("/vat/excel")
    public ResponseEntity<byte[]> exportVatExcel(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "1") Integer half) {
        return exportVat(userId, year, half);
    }

    @GetMapping("/income-tax/excel")
    public ResponseEntity<byte[]> exportIncomeTaxExcel(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        return exportIncomeTax(userId, year);
    }

    @GetMapping("/income-tax/pdf")
    public ResponseEntity<byte[]> exportIncomeTaxPdf(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        return exportBookEntries(userId, year);
    }

    @GetMapping("/simple-ledger/pdf")
    public ResponseEntity<byte[]> exportSimpleLedgerPdf(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        return exportBookEntries(userId, year);
    }

    // ── Response Helpers ──

    private String buildContentDisposition(String filename) {
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filename))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filename))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
