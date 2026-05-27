package com.ssafy.tax7i.bookentry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.service.BookEntryService;
import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookEntryController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class BookEntryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BookEntryService bookEntryService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/book-entries ─────────────

    @Test
    void create_201_장부생성() throws Exception {
        BookEntryResponse response = createResponse(1L, false);
        given(bookEntryService.create(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/book-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entryDate", "2026-03-16",
                                "merchantName", "스타벅스",
                                "entryType", "EXPENSE",
                                "amount", 55000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.vatAmount").value(5000))
                .andExpect(jsonPath("$.data.supplyPrice").value(50000));
    }

    @Test
    void create_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/book-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "merchantName", "스타벅스"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ───────────── GET /api/book-entries ─────────────

    @Test
    void getEntries_200_미확인필터() throws Exception {
        BookEntryResponse response = createResponse(1L, false);
        given(bookEntryService.getEntries(
                any(), eq(false), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/book-entries")
                        .param("confirmed", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].confirmed").value(false));
    }

    // ───────────── GET /api/book-entries/{id} ─────────────

    @Test
    void getEntry_200_단건조회() throws Exception {
        BookEntryResponse response = createResponse(1L, false);
        given(bookEntryService.getEntry(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/book-entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantName").value("스타벅스"))
                .andExpect(jsonPath("$.data.expenseAmount").value(50000));
    }

    @Test
    void getEntry_404_존재하지않음() throws Exception {
        given(bookEntryService.getEntry(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.BOOK_ENTRY_NOT_FOUND));

        mockMvc.perform(get("/api/book-entries/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BOOK_ENTRY_NOT_FOUND"));
    }

    // ───────────── GET /api/book-entries/unconfirmed-count ─────────────

    @Test
    void getUnconfirmedCount_200() throws Exception {
        given(bookEntryService.getUnconfirmedCount(any())).willReturn(3L);

        mockMvc.perform(get("/api/book-entries/unconfirmed-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    // ───────────── PATCH /api/book-entries/{id}/confirm ─────────────

    @Test
    void confirm_200_확인처리() throws Exception {
        BookEntryResponse response = createResponse(1L, true);
        given(bookEntryService.confirm(any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/api/book-entries/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));
    }

    // ───────────── PATCH /api/book-entries/{id}/category ─────────────

    @Test
    void updateCategory_200_세목수정() throws Exception {
        BookEntryResponse response = new BookEntryResponse(
                1L, 1L, LocalDate.of(2026, 3, 16), "스타벅스 결제", "스타벅스",
                EntryType.EXPENSE, 0L, 50000L, 0L, 5000L, 50000L,
                "ACC_ENTERTAIN", "접대비", true, true, 0, false, null, null, LocalDateTime.now());
        given(bookEntryService.updateCategory(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(patch("/api/book-entries/1/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryCode", "ACC_ENTERTAIN",
                                "categoryName", "접대비"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryCode").value("ACC_ENTERTAIN"))
                .andExpect(jsonPath("$.data.categoryName").value("접대비"));
    }

    @Test
    void updateCategory_필수값누락_400() throws Exception {
        mockMvc.perform(patch("/api/book-entries/1/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryCode", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ───────────── PATCH /api/book-entries/{id}/personal ─────────────

    @Test
    void markAsPersonal_200() throws Exception {
        BookEntryResponse response = new BookEntryResponse(
                1L, 1L, LocalDate.of(2026, 3, 16), "스타벅스 결제", "스타벅스",
                EntryType.EXPENSE, 0L, 50000L, 0L, 5000L, 50000L,
                null, null, false, true, 0, false, null, null, LocalDateTime.now());
        given(bookEntryService.markAsPersonal(any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/api/book-entries/1/personal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBusinessExpense").value(false));
    }

    // ───────────── PATCH /api/book-entries/{id}/business ─────────────

    @Test
    void markAsBusiness_200() throws Exception {
        BookEntryResponse response = createResponse(1L, false);
        given(bookEntryService.markAsBusiness(any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/api/book-entries/1/business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBusinessExpense").value(true));
    }

    // ───────────── helpers ─────────────

    private BookEntryResponse createResponse(Long id, boolean confirmed) {
        return new BookEntryResponse(
                id, 1L, LocalDate.of(2026, 3, 16), "스타벅스 결제", "스타벅스",
                EntryType.EXPENSE, 0L, 50000L, 0L, 5000L, 50000L,
                null, null, true, true, 0, confirmed,
                confirmed ? LocalDateTime.now() : null, null, LocalDateTime.now());
    }
}
