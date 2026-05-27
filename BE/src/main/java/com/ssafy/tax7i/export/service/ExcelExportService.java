package com.ssafy.tax7i.export.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세금 신고서 내보내기 파사드.
 * 각 export 타입은 전용 클래스에 위임한다:
 * - 부가가치세: {@link VatExcelExporter}
 * - 종합소득세: {@link IncomeTaxExcelExporter}
 * - 간편장부 PDF: {@link SimpleLedgerPdfExporter}
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelExportService {

    private final VatExcelExporter vatExcelExporter;
    private final IncomeTaxExcelExporter incomeTaxExcelExporter;
    private final SimpleLedgerPdfExporter simpleLedgerPdfExporter;

    public byte[] exportVatExcel(Long userId, int year, int half) {
        return vatExcelExporter.export(userId, year, half);
    }

    public byte[] exportIncomeTaxExcel(Long userId, int year) {
        return incomeTaxExcelExporter.export(userId, year);
    }

    public byte[] generateSimpleLedgerPdf(Long userId, int year) {
        return simpleLedgerPdfExporter.export(userId, year);
    }
}
