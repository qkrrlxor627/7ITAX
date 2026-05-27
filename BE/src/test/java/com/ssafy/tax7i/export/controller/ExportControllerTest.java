package com.ssafy.tax7i.export.controller;

import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.export.service.ExcelExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ExportControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ExcelExportService excelExportService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── GET /api/export/simple-ledger/pdf ─────────────

    @Test
    void exportSimpleLedgerPdf_200() throws Exception {
        given(excelExportService.generateSimpleLedgerPdf(any(), anyInt()))
                .willReturn(new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF

        mockMvc.perform(get("/api/export/simple-ledger/pdf")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void exportSimpleLedgerPdf_200_파라미터없음_현재연도() throws Exception {
        given(excelExportService.generateSimpleLedgerPdf(any(), anyInt()))
                .willReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        mockMvc.perform(get("/api/export/simple-ledger/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    // ───────────── GET /api/export/vat/excel ─────────────

    @Test
    void exportVatExcel_200_1기() throws Exception {
        given(excelExportService.exportVatExcel(any(), anyInt(), eq(1)))
                .willReturn(new byte[]{0x50, 0x4B});

        mockMvc.perform(get("/api/export/vat/excel")
                        .param("year", "2025")
                        .param("half", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("2025")));
    }

    @Test
    void exportVatExcel_200_2기() throws Exception {
        given(excelExportService.exportVatExcel(any(), anyInt(), eq(2)))
                .willReturn(new byte[]{0x50, 0x4B});

        mockMvc.perform(get("/api/export/vat/excel")
                        .param("year", "2025")
                        .param("half", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportVatExcel_200_파라미터없음_기본값1기() throws Exception {
        given(excelExportService.exportVatExcel(any(), anyInt(), eq(1)))
                .willReturn(new byte[]{0x50, 0x4B});

        mockMvc.perform(get("/api/export/vat/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    // ───────────── GET /api/export/income-tax/excel ─────────────

    @Test
    void exportIncomeTaxExcel_200() throws Exception {
        given(excelExportService.exportIncomeTaxExcel(any(), anyInt()))
                .willReturn(new byte[]{0x50, 0x4B});

        mockMvc.perform(get("/api/export/income-tax/excel")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("2025")));
    }

    @Test
    void exportIncomeTaxExcel_200_파라미터없음_현재연도() throws Exception {
        given(excelExportService.exportIncomeTaxExcel(any(), anyInt()))
                .willReturn(new byte[]{0x50, 0x4B});

        mockMvc.perform(get("/api/export/income-tax/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
