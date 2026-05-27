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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimpleLedgerPdfExporter {

    private final BookEntryRepository bookEntryRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final TemplateEngine templateEngine;
    private final ExcelExportHelper helper;

    public byte[] export(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<LedgerEntry> entries = new ArrayList<>();
        long totalIncome = 0, totalIncomeVat = 0;
        long totalExpense = 0, totalExpenseVat = 0;
        long totalAsset = 0, totalAssetVat = 0;

        int pageNum = 0;
        Page<BookEntry> entryPage;
        do {
            entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
                    userId, start, end, PageRequest.of(pageNum, 500, Sort.by("entryDate")));
            for (BookEntry e : entryPage.getContent()) {
                if (!e.getConfirmed() || !e.getIsBusinessExpense()) continue;

                LedgerEntry le = new LedgerEntry();
                le.date = e.getEntryDate().format(dateFmt);
                le.description = e.getDescription() != null ? e.getDescription() : "";
                le.note = e.getNote() != null ? e.getNote() : "";

                if (e.getEntryType() == EntryType.INCOME) {
                    le.incomeAmount = e.getIncomeAmount();
                    le.incomeVat = e.getVatAmount();
                    totalIncome += e.getIncomeAmount();
                    totalIncomeVat += e.getVatAmount();
                } else if (e.getEntryType() == EntryType.EXPENSE) {
                    le.expenseAmount = e.getExpenseAmount();
                    le.expenseVat = e.getVatAmount();
                    totalExpense += e.getExpenseAmount();
                    totalExpenseVat += e.getVatAmount();
                } else if (e.getEntryType() == EntryType.ASSET) {
                    le.assetAmount = e.getFixedAssetAmount();
                    le.assetVat = e.getVatAmount();
                    totalAsset += e.getFixedAssetAmount();
                    totalAssetVat += e.getVatAmount();
                }

                entries.add(le);
            }
            pageNum++;
        } while (entryPage.hasNext());

        Context context = new Context();
        context.setVariable("year", year);
        context.setVariable("userName", user.getName());
        context.setVariable("businessName", helper.profileValue(profile, "businessName"));
        context.setVariable("regNumber", helper.profileValue(profile, "businessRegNumber"));
        context.setVariable("entries", entries);
        context.setVariable("entryCount", entries.size());
        context.setVariable("totalIncome", totalIncome);
        context.setVariable("totalIncomeVat", totalIncomeVat);
        context.setVariable("totalExpense", totalExpense);
        context.setVariable("totalExpenseVat", totalExpenseVat);
        context.setVariable("totalAsset", totalAsset);
        context.setVariable("totalAssetVat", totalAssetVat);

        String html = templateEngine.process("pdf/simple-ledger", context);
        return helper.htmlToPdf(html);
    }

    /** 간편장부 PDF용 DTO */
    public static class LedgerEntry {
        private String date;
        private String description;
        private long incomeAmount;
        private long incomeVat;
        private long expenseAmount;
        private long expenseVat;
        private long assetAmount;
        private long assetVat;
        private String note;

        public String getDate() { return date; }
        public String getDescription() { return description; }
        public long getIncomeAmount() { return incomeAmount; }
        public long getIncomeVat() { return incomeVat; }
        public long getExpenseAmount() { return expenseAmount; }
        public long getExpenseVat() { return expenseVat; }
        public long getAssetAmount() { return assetAmount; }
        public long getAssetVat() { return assetVat; }
        public String getNote() { return note; }
    }
}
