package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.repository.VatReturnRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.ssafy.tax7i.export.service.ExcelExportHelper.MOCK_ADDRESS;
import static com.ssafy.tax7i.export.service.ExcelExportHelper.MOCK_BUSINESS_TYPE;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VatExcelExporter {

    // ── 부가세 신고서 시트 행/열 인덱스 ──
    // 인적사항
    private static final int ROW_BUSINESS_NAME = 5;
    private static final int ROW_REG_NUMBER = 6;
    private static final int ROW_TAXPAYER_NAME = 7;
    private static final int ROW_ADDRESS = 8;
    private static final int ROW_BUSINESS_TYPE = 9;
    private static final int ROW_PERIOD_LABEL = 10;
    private static final int COL_INFO = 2;
    // 매출세액
    private static final int ROW_SALES_CARD = 16;
    private static final int ROW_SALES_TOTAL = 22;
    private static final int COL_AMOUNT = 3;
    private static final int COL_TAX = 5;
    // 매입세액
    private static final int ROW_PURCHASE_GENERAL = 26;
    private static final int ROW_PURCHASE_ASSET = 27;
    private static final int ROW_PURCHASE_TOTAL = 31;
    // 납부세액
    private static final int ROW_VAT_PAYABLE = 35;
    private static final int ROW_PRELIMINARY_PAID = 39;
    private static final int ROW_FINAL_VAT = 42;
    private static final int COL_VAT_VALUE = 4;

    private final BookEntryRepository bookEntryRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final VatReturnRepository vatReturnRepository;
    private final ExcelExportHelper helper;

    public byte[] export(Long userId, int year, int half) {
        if (half != 1 && half != 2) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "half는 1 또는 2여야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

        LocalDate start, end;
        if (half == 1) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 6, 30);
        } else {
            start = LocalDate.of(year, 7, 1);
            end = LocalDate.of(year, 12, 31);
        }

        long salesAmount = 0, salesVat = 0, salesCount = 0;
        long purchaseAmount = 0, purchaseVat = 0, purchaseCount = 0;
        long assetAmount = 0, assetVat = 0, assetCount = 0;

        int page = 0;
        Page<BookEntry> entryPage;
        do {
            entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
                    userId, start, end, PageRequest.of(page, 500, Sort.by("entryDate")));
            for (BookEntry e : entryPage.getContent()) {
                if (!e.getConfirmed() || !e.getIsBusinessExpense()) continue;
                if (e.getEntryType() == EntryType.INCOME) {
                    salesAmount += e.getSupplyPrice();
                    salesVat += e.getVatAmount();
                    salesCount++;
                } else if (e.getEntryType() == EntryType.EXPENSE && e.getIsVatDeductible()) {
                    purchaseAmount += e.getSupplyPrice();
                    purchaseVat += e.getVatAmount();
                    purchaseCount++;
                } else if (e.getEntryType() == EntryType.ASSET && e.getIsVatDeductible()) {
                    assetAmount += e.getSupplyPrice();
                    assetVat += e.getVatAmount();
                    assetCount++;
                }
            }
            page++;
        } while (entryPage.hasNext());

        long totalPurchaseAmount = purchaseAmount + assetAmount;
        long totalPurchaseVat = purchaseVat + assetVat;
        long vatPayable = salesVat - totalPurchaseVat;

        long preliminaryPaid = vatReturnRepository
                .findByUserIdAndTaxYearAndTaxPeriod(userId, year, half)
                .map(vr -> vr.getPreliminaryPaid())
                .orElse(0L);
        long finalVat = vatPayable - preliminaryPaid;

        Workbook workbook = helper.loadTemplate("/templates/excel/vat_template.xlsx");
        fillDeclarationSheet(workbook.getSheetAt(0), user, profile, year, half,
                salesAmount, salesVat, purchaseAmount, purchaseVat,
                assetAmount, assetVat, totalPurchaseAmount, totalPurchaseVat,
                vatPayable, preliminaryPaid, finalVat);
        fillSalesInvoiceSheet(workbook.getSheetAt(1), user, profile, year, half,
                salesCount, salesAmount, salesVat);
        fillPurchaseInvoiceSheet(workbook.getSheetAt(2), user, profile, year, half,
                purchaseCount, purchaseAmount, purchaseVat,
                assetCount, assetAmount, assetVat);

        return helper.workbookToBytes(workbook);
    }

    private void fillDeclarationSheet(Sheet sheet, User user, BusinessProfile profile,
                                       int year, int half,
                                       long salesAmount, long salesVat,
                                       long purchaseAmount, long purchaseVat,
                                       long assetAmount, long assetVat,
                                       long totalPurchaseAmount, long totalPurchaseVat,
                                       long vatPayable, long preliminaryPaid, long finalVat) {
        if (half == 1) {
            helper.setCellValue(sheet, 2, 0,
                    "대상기간: " + year + "년 1.1 ~ 6.30 | 마감: 7월 25일 | 제출처: 홈택스 전자신고");
        }
        helper.setCellValue(sheet, ROW_BUSINESS_NAME, COL_INFO, helper.profileValue(profile, "businessName"));
        helper.setCellValue(sheet, ROW_REG_NUMBER, COL_INFO, helper.profileValue(profile, "businessRegNumber"));
        helper.setCellValue(sheet, ROW_TAXPAYER_NAME, COL_INFO, user.getName());
        helper.setCellValue(sheet, ROW_ADDRESS, COL_INFO, MOCK_ADDRESS);
        helper.setCellValue(sheet, ROW_BUSINESS_TYPE, COL_INFO, MOCK_BUSINESS_TYPE);
        String periodLabel = half == 1
                ? year + "년 1기 (1.1 ~ 6.30)"
                : year + "년 2기 (7.1 ~ 12.31)";
        helper.setCellValue(sheet, ROW_PERIOD_LABEL, COL_INFO, periodLabel);

        helper.setCellValue(sheet, ROW_SALES_CARD, COL_AMOUNT, salesAmount);
        helper.setCellValue(sheet, ROW_SALES_CARD, COL_TAX, salesVat);
        helper.setCellValue(sheet, ROW_SALES_TOTAL, COL_AMOUNT, salesAmount);
        helper.setCellValue(sheet, ROW_SALES_TOTAL, COL_TAX, salesVat);

        helper.setCellValue(sheet, ROW_PURCHASE_GENERAL, COL_AMOUNT, purchaseAmount);
        helper.setCellValue(sheet, ROW_PURCHASE_GENERAL, COL_TAX, purchaseVat);
        helper.setCellValue(sheet, ROW_PURCHASE_ASSET, COL_AMOUNT, assetAmount);
        helper.setCellValue(sheet, ROW_PURCHASE_ASSET, COL_TAX, assetVat);
        helper.setCellValue(sheet, ROW_PURCHASE_TOTAL, COL_AMOUNT, totalPurchaseAmount);
        helper.setCellValue(sheet, ROW_PURCHASE_TOTAL, COL_TAX, totalPurchaseVat);

        helper.setCellValue(sheet, ROW_VAT_PAYABLE, COL_VAT_VALUE, vatPayable);
        helper.setCellValue(sheet, ROW_PRELIMINARY_PAID, COL_VAT_VALUE, preliminaryPaid);
        helper.setCellValue(sheet, ROW_FINAL_VAT, COL_VAT_VALUE, finalVat);
    }

    private void fillSalesInvoiceSheet(Sheet sheet, User user, BusinessProfile profile,
                                        int year, int half,
                                        long salesCount, long salesAmount, long salesVat) {
        helper.setCellValue(sheet, 4, COL_INFO, helper.profileValue(profile, "businessRegNumber"));
        helper.setCellValue(sheet, 5, COL_INFO, helper.profileValue(profile, "businessName"));
        helper.setCellValue(sheet, 6, COL_INFO, user.getName());
        String periodLabel = half == 1
                ? year + "년 1기 (1.1 ~ 6.30)"
                : year + "년 2기 (7.1 ~ 12.31)";
        helper.setCellValue(sheet, 7, COL_INFO, periodLabel);

        helper.setCellValue(sheet, 11, COL_INFO, salesCount);
        helper.setCellValue(sheet, 11, COL_AMOUNT, salesAmount);
        helper.setCellValue(sheet, 11, COL_VAT_VALUE, salesVat);
        helper.setCellValue(sheet, 13, COL_INFO, salesCount);
        helper.setCellValue(sheet, 13, COL_AMOUNT, salesAmount);
        helper.setCellValue(sheet, 13, COL_VAT_VALUE, salesVat);
    }

    private void fillPurchaseInvoiceSheet(Sheet sheet, User user, BusinessProfile profile,
                                           int year, int half,
                                           long purchaseCount, long purchaseAmount, long purchaseVat,
                                           long assetCount, long assetAmount, long assetVat) {
        helper.setCellValue(sheet, 4, COL_INFO, helper.profileValue(profile, "businessRegNumber"));
        helper.setCellValue(sheet, 5, COL_INFO, helper.profileValue(profile, "businessName"));
        helper.setCellValue(sheet, 6, COL_INFO, user.getName());
        String periodLabel = half == 1
                ? year + "년 1기 (1.1 ~ 6.30)"
                : year + "년 2기 (7.1 ~ 12.31)";
        helper.setCellValue(sheet, 7, COL_INFO, periodLabel);

        helper.setCellValue(sheet, 11, COL_INFO, purchaseCount);
        helper.setCellValue(sheet, 11, COL_AMOUNT, purchaseAmount);
        helper.setCellValue(sheet, 11, COL_VAT_VALUE, purchaseVat);
        helper.setCellValue(sheet, 12, COL_INFO, assetCount);
        helper.setCellValue(sheet, 12, COL_AMOUNT, assetAmount);
        helper.setCellValue(sheet, 12, COL_VAT_VALUE, assetVat);
        helper.setCellValue(sheet, 13, COL_INFO, purchaseCount + assetCount);
        helper.setCellValue(sheet, 13, COL_AMOUNT, purchaseAmount + assetAmount);
        helper.setCellValue(sheet, 13, COL_VAT_VALUE, purchaseVat + assetVat);
    }
}
