package com.ssafy.tax7i.bookentry.service;

import com.ssafy.tax7i.bookentry.dto.BookEntryCategoryUpdateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.dto.BookEntrySummaryResponse;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BookEntryServiceTest {

    @Mock
    private BookEntryRepository bookEntryRepository;

    @Mock
    private TaxParameterService taxParameterService;

    @InjectMocks
    private BookEntryService bookEntryService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(taxParameterService.getVatRate(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(0.10);
    }

    // ───────────── create ─────────────

    @Test
    void create_과세거래_부가세자동분리() {
        BookEntryCreateRequest request = new BookEntryCreateRequest(
                1L, LocalDate.of(2026, 3, 16), "스타벅스 결제", "스타벅스",
                EntryType.EXPENSE, 55000L, false, null, null, null, null);

        given(bookEntryRepository.save(any(BookEntry.class))).willAnswer(invocation -> {
            BookEntry entry = invocation.getArgument(0);
            setField(entry, "id", 1L);
            return entry;
        });

        BookEntryResponse response = bookEntryService.create(1L, request);

        assertThat(response.vatAmount()).isEqualTo(5000L);        // 55000 / 11 = 5000
        assertThat(response.supplyPrice()).isEqualTo(50000L);     // 55000 - 5000 = 50000
        assertThat(response.expenseAmount()).isEqualTo(50000L);
        assertThat(response.isBusinessExpense()).isTrue();
        assertThat(response.confirmed()).isFalse();
    }

    @Test
    void create_면세거래_부가세0원() {
        BookEntryCreateRequest request = new BookEntryCreateRequest(
                2L, LocalDate.of(2026, 3, 16), "병원 진료", "서울대병원",
                EntryType.EXPENSE, 30000L, true, null, null, null, null);

        given(bookEntryRepository.save(any(BookEntry.class))).willAnswer(invocation -> {
            BookEntry entry = invocation.getArgument(0);
            setField(entry, "id", 2L);
            return entry;
        });

        BookEntryResponse response = bookEntryService.create(1L, request);

        assertThat(response.vatAmount()).isEqualTo(0L);
        assertThat(response.supplyPrice()).isEqualTo(30000L);
        assertThat(response.isVatDeductible()).isFalse();
    }

    @Test
    void create_수입거래_incomeAmount설정() {
        BookEntryCreateRequest request = new BookEntryCreateRequest(
                null, LocalDate.of(2026, 3, 16), "외주 대금", "클라이언트A",
                EntryType.INCOME, 1100000L, false, null, null, null, null);

        given(bookEntryRepository.save(any(BookEntry.class))).willAnswer(invocation -> {
            BookEntry entry = invocation.getArgument(0);
            setField(entry, "id", 3L);
            return entry;
        });

        BookEntryResponse response = bookEntryService.create(1L, request);

        assertThat(response.incomeAmount()).isEqualTo(1000000L);  // 1100000 - 100000
        assertThat(response.expenseAmount()).isEqualTo(0L);
        assertThat(response.entryType()).isEqualTo(EntryType.INCOME);
    }

    @Test
    void create_고정자산_fixedAssetAmount설정() {
        BookEntryCreateRequest request = new BookEntryCreateRequest(
                null, LocalDate.of(2026, 3, 16), "맥북프로 구매", "애플스토어",
                EntryType.ASSET, 3300000L, false, null, null, null, null);

        given(bookEntryRepository.save(any(BookEntry.class))).willAnswer(invocation -> {
            BookEntry entry = invocation.getArgument(0);
            setField(entry, "id", 4L);
            return entry;
        });

        BookEntryResponse response = bookEntryService.create(1L, request);

        assertThat(response.fixedAssetAmount()).isEqualTo(3000000L);
        assertThat(response.vatAmount()).isEqualTo(300000L);
        assertThat(response.expenseAmount()).isEqualTo(0L);
    }

    // ───────────── create 예외 ─────────────

    @Test
    void create_중복paymentId_예외() {
        BookEntry existing = createEntry(1L, 1L, false);
        given(bookEntryRepository.findByPaymentId(1L)).willReturn(Optional.of(existing));

        BookEntryCreateRequest request = new BookEntryCreateRequest(
                1L, LocalDate.of(2026, 3, 16), "중복 테스트", "스타벅스",
                EntryType.EXPENSE, 55000L, false, null, null, null, null);

        assertThatThrownBy(() -> bookEntryService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_BOOK_ENTRY));
    }

    @Test
    void create_금액0이하_예외() {
        BookEntryCreateRequest request = new BookEntryCreateRequest(
                null, LocalDate.of(2026, 3, 16), "테스트", "테스트",
                EntryType.EXPENSE, 0L, false, null, null, null, null);

        assertThatThrownBy(() -> bookEntryService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ARGUMENT));
    }

    // ───────────── confirm ─────────────

    @Test
    void confirm_성공() {
        BookEntry entry = createEntry(1L, 1L, false);
        given(bookEntryRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(entry));

        BookEntryResponse response = bookEntryService.confirm(1L, 1L);

        assertThat(response.confirmed()).isTrue();
        assertThat(response.confirmedAt()).isNotNull();
    }

    @Test
    void confirm_이미확인된전표_예외() {
        BookEntry entry = createEntry(1L, 1L, true);
        given(bookEntryRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(entry));

        assertThatThrownBy(() -> bookEntryService.confirm(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CONFIRMED));
    }

    @Test
    void confirm_존재하지않는항목_예외() {
        given(bookEntryRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookEntryService.confirm(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BOOK_ENTRY_NOT_FOUND));
    }

    // ───────────── updateCategory ─────────────

    @Test
    void updateCategory_성공() {
        BookEntry entry = createEntry(1L, 1L, false);
        given(bookEntryRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(entry));

        BookEntryCategoryUpdateRequest request = new BookEntryCategoryUpdateRequest("ACC_ENTERTAIN", "접대비");
        BookEntryResponse response = bookEntryService.updateCategory(1L, 1L, request);

        assertThat(response.categoryCode()).isEqualTo("ACC_ENTERTAIN");
        assertThat(response.categoryName()).isEqualTo("접대비");
    }

    // ───────────── markAsPersonal / markAsBusiness ─────────────

    @Test
    void markAsPersonal_성공() {
        BookEntry entry = createEntry(1L, 1L, false);
        given(bookEntryRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(entry));

        BookEntryResponse response = bookEntryService.markAsPersonal(1L, 1L);

        assertThat(response.isBusinessExpense()).isFalse();
    }

    @Test
    void markAsBusiness_복원_성공() {
        BookEntry entry = createEntry(1L, 1L, false);
        entry.markAsPersonal();
        given(bookEntryRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(entry));

        BookEntryResponse response = bookEntryService.markAsBusiness(1L, 1L);

        assertThat(response.isBusinessExpense()).isTrue();
    }

    // ───────────── getEntries ─────────────

    @SuppressWarnings("unchecked")
    @Test
    void getEntries_미확인필터() {
        BookEntry entry = createEntry(1L, 1L, false);
        Pageable pageable = PageRequest.of(0, 20);
        given(bookEntryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(entry)));

        Page<BookEntryResponse> result = bookEntryService.getEntries(1L, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).confirmed()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getEntries_전체조회() {
        BookEntry entry1 = createEntry(1L, 1L, false);
        BookEntry entry2 = createEntry(2L, 1L, true);
        Pageable pageable = PageRequest.of(0, 20);
        given(bookEntryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(entry1, entry2)));

        Page<BookEntryResponse> result = bookEntryService.getEntries(1L, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ───────────── getSummary ─────────────

    @Test
    void getSummary_기본집계_정상() {
        given(bookEntryRepository.safeAggregate(eq(1L), any(), any()))
                .willReturn(new AggregateResult(50_000_000L, 20_000_000L, 3_000_000L, 15_000_000L));
        given(bookEntryRepository.sumByMonthAndYear(1L, 2026)).willReturn(List.of());
        given(bookEntryRepository.sumExpenseByCategoryAndYear(1L, 2026)).willReturn(List.of());
        given(bookEntryRepository.sumIncomeByMerchant(eq(1L), any(), any())).willReturn(List.of());

        BookEntrySummaryResponse result = bookEntryService.getSummary(1L, 2026);

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.totalIncome()).isEqualTo(50_000_000L);
        assertThat(result.totalExpense()).isEqualTo(20_000_000L);
    }

    @Test
    void getSummary_전년비교_전년데이터없으면_null() {
        // 올해 데이터
        given(bookEntryRepository.safeAggregate(eq(1L), any(), any()))
                .willReturn(new AggregateResult(0L, 0L, 0L, 0L));
        given(bookEntryRepository.sumByMonthAndYear(1L, 2026)).willReturn(List.of());
        given(bookEntryRepository.sumExpenseByCategoryAndYear(1L, 2026)).willReturn(List.of());
        given(bookEntryRepository.sumIncomeByMerchant(eq(1L), any(), any())).willReturn(List.of());

        BookEntrySummaryResponse result = bookEntryService.getSummary(1L, 2026);

        assertThat(result.comparison()).isNull();
    }

    // ───────────── getUnconfirmedCount ─────────────

    @Test
    void getUnconfirmedCount_성공() {
        given(bookEntryRepository.countByUserIdAndConfirmed(1L, false)).willReturn(5L);

        long count = bookEntryService.getUnconfirmedCount(1L);

        assertThat(count).isEqualTo(5);
    }

    // ───────────── helpers ─────────────

    private BookEntry createEntry(Long id, Long userId, boolean confirmed) {
        BookEntry entry = BookEntry.builder()
                .userId(userId)
                .paymentId(id)
                .entryDate(LocalDate.of(2026, 3, 16))
                .description("테스트 결제")
                .merchantName("스타벅스")
                .entryType(EntryType.EXPENSE)
                .expenseAmount(50000L)
                .vatAmount(5000L)
                .supplyPrice(50000L)
                .build();
        setField(entry, "id", id);
        if (confirmed) {
            entry.confirm();
        }
        return entry;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
