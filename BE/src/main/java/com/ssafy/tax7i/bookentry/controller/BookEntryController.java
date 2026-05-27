package com.ssafy.tax7i.bookentry.controller;

import com.ssafy.tax7i.bookentry.dto.BookEntryCategoryUpdateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryNoteUpdateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.dto.BookEntrySummaryResponse;
import com.ssafy.tax7i.bookentry.dto.IncomeCreateRequest;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.service.BookEntryService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/book-entries")
@RequiredArgsConstructor
public class BookEntryController {

    private final BookEntryService bookEntryService;

    @PostMapping
    public ResponseEntity<SuccessResponse<BookEntryResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BookEntryCreateRequest request) {
        BookEntryResponse response = bookEntryService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    @PostMapping("/income")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> createIncome(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody IncomeCreateRequest request) {
        BookEntryResponse response = bookEntryService.createIncome(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<SuccessResponse<BookEntrySummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year) {
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "연도는 1900~2099 범위여야 합니다.");
        }
        BookEntrySummaryResponse response = bookEntryService.getSummary(userId, year);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<Page<BookEntryResponse>>> getEntries(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Boolean confirmed,
            @RequestParam(required = false) EntryType entryType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean hasEvidence,
            @RequestParam(required = false) Boolean unclassified,
            @PageableDefault(size = 20, sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BookEntryResponse> response = bookEntryService.getEntries(
                userId, confirmed, entryType, startDate, endDate,
                categoryCode, keyword, hasEvidence, unclassified, pageable);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{entryId}")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> getEntry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId) {
        BookEntryResponse response = bookEntryService.getEntry(userId, entryId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/unconfirmed-count")
    public ResponseEntity<SuccessResponse<Long>> getUnconfirmedCount(
            @AuthenticationPrincipal Long userId) {
        long count = bookEntryService.getUnconfirmedCount(userId);
        return ResponseEntity.ok(SuccessResponse.of(count));
    }

    @PatchMapping("/{entryId}/confirm")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> confirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId) {
        BookEntryResponse response = bookEntryService.confirm(userId, entryId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PatchMapping("/{entryId}/category")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> updateCategory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId,
            @Valid @RequestBody BookEntryCategoryUpdateRequest request) {
        BookEntryResponse response = bookEntryService.updateCategory(userId, entryId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PatchMapping("/{entryId}/note")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> updateNote(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId,
            @Valid @RequestBody BookEntryNoteUpdateRequest request) {
        BookEntryResponse response = bookEntryService.updateNote(userId, entryId, request.note());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PatchMapping("/{entryId}/personal")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> markAsPersonal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId) {
        BookEntryResponse response = bookEntryService.markAsPersonal(userId, entryId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PatchMapping("/{entryId}/business")
    public ResponseEntity<SuccessResponse<BookEntryResponse>> markAsBusiness(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId) {
        BookEntryResponse response = bookEntryService.markAsBusiness(userId, entryId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
