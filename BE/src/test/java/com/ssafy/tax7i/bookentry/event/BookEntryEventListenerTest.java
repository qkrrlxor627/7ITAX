package com.ssafy.tax7i.bookentry.event;

import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.service.BookEntryService;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.service.TaxClassificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BookEntryEventListenerTest {

    @Mock
    private BookEntryService bookEntryService;

    @Mock
    private TaxClassificationService classificationService;

    @InjectMocks
    private BookEntryEventListener eventListener;

    @Test
    void 사업자결제_장부자동생성() {
        PaymentCapturedEvent event = new PaymentCapturedEvent(
                1L, 1L, 55000L, "스타벅스", "5812", "BUSINESS", LocalDateTime.now());

        given(classificationService.classify(any())).willReturn(
                ClassificationResult.needsConfirmation("접대비", "불공제", "소득세법§35", "거래처와의 식사인가요?"));

        BookEntryResponse mockResponse = new BookEntryResponse(
                1L, 1L, LocalDate.now(), "스타벅스 결제", "스타벅스",
                EntryType.EXPENSE, 0L, 50000L, 0L, 5000L, 50000L,
                "08", "접대비", true, true, 0, false, null, null, LocalDateTime.now());
        given(bookEntryService.create(eq(1L), any(BookEntryCreateRequest.class))).willReturn(mockResponse);

        eventListener.handlePaymentCaptured(event);

        then(classificationService).should().classify(any());
        then(bookEntryService).should().create(eq(1L), any(BookEntryCreateRequest.class));
    }

    @Test
    void 개인결제_장부생성안함() {
        PaymentCapturedEvent event = new PaymentCapturedEvent(
                2L, 1L, 30000L, "편의점", "5411", "PERSONAL", LocalDateTime.now());

        eventListener.handlePaymentCaptured(event);

        then(bookEntryService).should(never()).create(any(), any());
        then(classificationService).should(never()).classify(any());
    }

    @Test
    void 장부생성실패해도_예외전파안함() {
        PaymentCapturedEvent event = new PaymentCapturedEvent(
                3L, 1L, 10000L, "테스트", "9999", "BUSINESS", LocalDateTime.now());

        given(classificationService.classify(any())).willReturn(
                ClassificationResult.needsConfirmation("기타 경비", "확인필요", null, null));

        given(bookEntryService.create(eq(1L), any(BookEntryCreateRequest.class)))
                .willThrow(new RuntimeException("DB 오류"));

        // 예외가 전파되지 않아야 함 (결제에 영향 없음)
        eventListener.handlePaymentCaptured(event);

        then(bookEntryService).should().create(eq(1L), any(BookEntryCreateRequest.class));
    }
}
