package com.ssafy.tax7i.export;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * 내보내기 샘플 파일 생성기 — 전체 9개 엔드포인트 대응.
 * 실행: ./gradlew test --tests "com.ssafy.tax7i.export.SampleExportGenerator"
 */
class SampleExportGenerator {

    private static final Path OUT = Paths.get("../docs/samples");
    private static final String FONT_R = "/fonts/NanumGothic-Regular.ttf";
    private static final String FONT_B = "/fonts/NanumGothic-Bold.ttf";

    // 샘플 장부 데이터 (12건)
    private static final String LEDGER_ROWS = """
            <tr><td class="c">2025-01-05</td><td>매출</td><td>웹개발 용역대금</td><td>클라이언트A</td><td class="r">5,000,000</td><td></td><td></td><td class="r">500,000</td></tr>
            <tr><td class="c">2025-01-10</td><td>지급수수료</td><td>클라우드 서버비</td><td>AWS Korea</td><td></td><td class="r">330,000</td><td></td><td class="r">33,000</td></tr>
            <tr><td class="c">2025-01-15</td><td>접대비</td><td>거래처 식사</td><td>강남레스토랑</td><td></td><td class="r">150,000</td><td></td><td class="r">15,000</td></tr>
            <tr><td class="c">2025-02-01</td><td>매출</td><td>앱개발 착수금</td><td>클라이언트B</td><td class="r">10,000,000</td><td></td><td></td><td class="r">1,000,000</td></tr>
            <tr><td class="c">2025-02-05</td><td>소모품비</td><td>사무용품</td><td>오피스디포</td><td></td><td class="r">85,000</td><td></td><td class="r">8,500</td></tr>
            <tr><td class="c">2025-02-10</td><td>차량유지비</td><td>주유비</td><td>GS칼텍스</td><td></td><td class="r">80,000</td><td></td><td class="r">8,000</td></tr>
            <tr><td class="c">2025-03-01</td><td>매출</td><td>유지보수 월정액</td><td>클라이언트A</td><td class="r">2,000,000</td><td></td><td></td><td class="r">200,000</td></tr>
            <tr><td class="c">2025-03-15</td><td>통신비</td><td>인터넷/전화</td><td>KT</td><td></td><td class="r">55,000</td><td></td><td class="r">5,500</td></tr>
            <tr><td class="c">2025-03-20</td><td>임차료</td><td>사무실 월세</td><td>강남빌딩</td><td></td><td class="r">1,000,000</td><td></td><td class="r">100,000</td></tr>
            <tr><td class="c">2025-04-01</td><td>고정자산</td><td>노트북 구입</td><td>삼성전자</td><td></td><td></td><td class="r">2,200,000</td><td class="r">220,000</td></tr>
            <tr><td class="c">2025-04-10</td><td>매출</td><td>컨설팅 수수료</td><td>클라이언트C</td><td class="r">3,000,000</td><td></td><td></td><td class="r">300,000</td></tr>
            <tr><td class="c">2025-05-15</td><td>복리후생비</td><td>직원 식대</td><td>배민</td><td></td><td class="r">200,000</td><td></td><td class="r">20,000</td></tr>
            """;

    @Test
    @Disabled("수동 실행 전용")
    void generateAllSamples() throws Exception {
        Files.createDirectories(OUT);

        // CSV 4건
        genBookEntriesCsv();
        genVatCsv();
        genIncomeTaxCsv();
        genLocalTaxCsv();

        // Excel 2건
        genVatExcel();
        genIncomeTaxExcel();

        // PDF 4건
        genSimpleLedgerPdf();
        genVatPdf();
        genIncomeTaxPdf();

        System.out.println("=== 샘플 " + OUT.toAbsolutePath() + " ===");
    }

    // ════════════════════════════════════════════
    //  CSV (4건)
    // ════════════════════════════════════════════

    @Test void genBookEntriesCsv() throws Exception {
        Files.createDirectories(OUT);
        var sb = new StringBuilder();
        sb.append("일자,계정과목,거래내용,거래처,수입금액,수입부가세,비용금액,비용부가세,자산증감금액,자산부가세,사업용여부,비고\n");
        sb.append("2025-01-05,매출,웹개발 용역대금,클라이언트A,5000000,500000,0,0,0,0,사업용,1월 계약분\n");
        sb.append("2025-01-10,지급수수료,클라우드 서버비,AWS Korea,0,0,330000,33000,0,0,사업용,월정액\n");
        sb.append("2025-01-15,접대비,거래처 식사,강남레스토랑,0,0,150000,15000,0,0,사업용,신년회식\n");
        sb.append("2025-02-01,매출,앱개발 착수금,클라이언트B,0,0,0,0,0,0,사업용,2월 프로젝트\n");
        sb.append("2025-04-01,고정자산,노트북 구입,삼성전자,0,0,0,0,2200000,220000,사업용,업무용 장비\n");
        csvBom("간편장부_2025.csv", sb.toString());
    }

    @Test void genVatCsv() throws Exception {
        Files.createDirectories(OUT);
        var sb = new StringBuilder();
        sb.append("부가가치세 신고 요약,2025년 1기\n\n");
        sb.append("구분,공급가액,부가세액\n");
        sb.append("매출,20000000,2000000\n");
        sb.append("매입(공제),1700000,170000\n");
        sb.append("\n납부(환급) 예상세액,,1830000\n");
        csvBom("부가세_2025_1기.csv", sb.toString());
    }

    @Test void genIncomeTaxCsv() throws Exception {
        Files.createDirectories(OUT);
        var sb = new StringBuilder();
        sb.append("종합소득세 신고 요약,2025년\n\n항목,금액\n");
        sb.append("총수입금액,80000000\n필요경비,45000000\n과세표준,33500000\n");
        sb.append("세율구간,24%\n산출세액,4780000\n지방소득세,458000\n경비처리 절세효과,2700000\n");
        csvBom("종합소득세_2025.csv", sb.toString());
    }

    @Test void genLocalTaxCsv() throws Exception {
        Files.createDirectories(OUT);
        var sb = new StringBuilder();
        sb.append("개인지방소득세 신고 요약,2025년\n");
        sb.append("법적근거,\"지방세법 §92(세율) §95(신고기한)\"\n\n항목,내용\n");
        sb.append("귀속연도,2025\n성명,홍길동\n사업자등록번호,123-45-67890\n");
        sb.append("종합소득세 결정세액,4580000\n개인지방소득세(결정세액x10%),458000\n");
        sb.append("신고기한,2026년 5월 31일\n납부처,납세지 관할 지방자치단체 (위택스)\n");
        csvBom("지방소득세_2025.csv", sb.toString());
    }

    // ════════════════════════════════════════════
    //  Excel (2건)
    // ════════════════════════════════════════════

    @Test void genVatExcel() throws Exception {
        Files.createDirectories(OUT);
        Workbook wb = loadTpl("/templates/excel/vat_template.xlsx");
        Sheet s0 = wb.getSheetAt(0);
        cell(s0, 2, 0, "대상기간: 2025년 1.1 ~ 6.30 | 마감: 7월 25일");
        cell(s0, 5, 2, "길동소프트"); cell(s0, 6, 2, "123-45-67890");
        cell(s0, 7, 2, "홍길동"); cell(s0, 10, 2, "2025년 1기 (1.1 ~ 6.30)");
        cell(s0, 16, 3, 20000000); cell(s0, 16, 5, 2000000);
        cell(s0, 22, 3, 20000000); cell(s0, 22, 5, 2000000);
        cell(s0, 26, 3, 1500000); cell(s0, 26, 5, 150000);
        cell(s0, 27, 3, 2200000); cell(s0, 27, 5, 220000);
        cell(s0, 31, 3, 3700000); cell(s0, 31, 5, 370000);
        cell(s0, 35, 4, 1630000); cell(s0, 39, 4, 0); cell(s0, 42, 4, 1630000);
        writeWb(wb, "부가세_2025_1기.xlsx");
    }

    @Test void genIncomeTaxExcel() throws Exception {
        Files.createDirectories(OUT);
        Workbook wb = loadTpl("/templates/excel/income_tax_template.xlsx");
        // 시트1: 신고서
        Sheet s0 = wb.getSheetAt(0);
        cell(s0, 5, 2, "홍길동"); cell(s0, 6, 2, "900101-*******");
        cell(s0, 12, 2, 80000000); cell(s0, 13, 2, 35000000);
        cell(s0, 18, 2, 33500000); cell(s0, 19, 2, "24%");
        cell(s0, 20, 2, 4780000); cell(s0, 21, 2, 200000);
        cell(s0, 22, 2, 4580000); cell(s0, 23, 2, 2400000); cell(s0, 24, 2, 2180000);
        // 시트2: 경비
        Sheet s1 = wb.getSheetAt(1);
        cell(s1, 5, 3, 80000000);
        cell(s1, 9, 3, 2400000); cell(s1, 11, 3, 3600000); cell(s1, 15, 3, 12000000);
        cell(s1, 16, 3, 3960000); cell(s1, 22, 3, 9600000); cell(s1, 24, 3, 41000000);
        // 시트3: 소득계산
        Sheet s2 = wb.getSheetAt(2);
        cell(s2, 5, 3, 80000000); cell(s2, 6, 3, 45000000);
        cell(s2, 7, 3, 35000000); cell(s2, 12, 3, 35000000);
        // 시트4 제거 후 간편장부 시트 추가
        if (wb.getNumberOfSheets() > 3) wb.removeSheetAt(3);
        Sheet ledger = wb.createSheet("간편장부");
        String[] hdr = {"일자","계정과목","거래내용","거래처","수입금액","비용금액","고정자산","부가세","사업용여부"};
        Row hr = ledger.createRow(0);
        for (int i = 0; i < hdr.length; i++) hr.createCell(i).setCellValue(hdr[i]);
        Object[][] data = {
            {"2025-01-05","매출","웹개발 용역대금","클라이언트A",5000000L,0L,0L,500000L,"사업용"},
            {"2025-01-10","지급수수료","클라우드 서버비","AWS Korea",0L,330000L,0L,33000L,"사업용"},
            {"2025-01-15","접대비","거래처 식사","강남레스토랑",0L,150000L,0L,15000L,"사업용"},
            {"2025-02-01","매출","앱개발 착수금","클라이언트B",10000000L,0L,0L,1000000L,"사업용"},
            {"2025-03-01","매출","유지보수 월정액","클라이언트A",2000000L,0L,0L,200000L,"사업용"},
            {"2025-03-20","임차료","사무실 월세","강남빌딩",0L,1000000L,0L,100000L,"사업용"},
            {"2025-04-01","고정자산","노트북 구입","삼성전자",0L,0L,2200000L,220000L,"사업용"},
            {"2025-04-10","매출","컨설팅 수수료","클라이언트C",3000000L,0L,0L,300000L,"사업용"},
        };
        for (int i = 0; i < data.length; i++) {
            Row r = ledger.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                if (data[i][j] instanceof Long v) r.createCell(j).setCellValue(v);
                else r.createCell(j).setCellValue(data[i][j].toString());
            }
        }
        writeWb(wb, "종합소득세_2025.xlsx");
    }

    // ════════════════════════════════════════════
    //  PDF (3건)
    // ════════════════════════════════════════════

    @Test void genSimpleLedgerPdf() throws Exception {
        Files.createDirectories(OUT);
        String html = pdfWrap("A4 landscape", """
        <h1>간  편  장  부</h1>
        <p class="sh">2025년 귀속 | 국세청고시 제2024-19호 | 소득세법 §160</p>
        <table class="info"><tr>
            <td class="l">성명</td><td>홍길동</td><td class="l">상호</td><td>길동소프트</td>
            <td class="l">사업자등록번호</td><td>123-45-67890</td><td class="l">기간</td><td>2025.01.01 ~ 2025.12.31</td>
        </tr></table>
        <table><thead><tr><th>일자</th><th>계정과목</th><th>거래내용</th><th>거래처</th><th>수입</th><th>비용</th><th>고정자산</th><th>부가세</th></tr></thead>
        <tbody>""" + LEDGER_ROWS + """
        <tr class="tot"><td colspan="4" class="c">합 계</td><td class="r">20,000,000</td><td class="r">1,900,000</td><td class="r">2,200,000</td><td class="r">2,410,000</td></tr>
        </tbody></table>
        <p class="disc">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</p>
        """);
        pdf("간편장부_2025.pdf", html);
    }

    @Test void genVatPdf() throws Exception {
        Files.createDirectories(OUT);
        String html = pdfWrap("A4", """
        <h1>부가가치세 신고서</h1>
        <p class="sh">2025년 1기 (2025.01.01 ~ 2025.06.30) | 7iTAX Simulation</p>
        <h3>사업자 정보</h3>
        <table><tr><td class="l">상호</td><td>길동소프트</td><td class="l">사업자등록번호</td><td>123-45-67890</td></tr>
        <tr><td class="l">대표자명</td><td>홍길동</td><td class="l">과세기간</td><td>2025.01.01 ~ 2025.06.30</td></tr></table>
        <h3>매출세액</h3>
        <table><thead><tr><th>구분</th><th>공급가액</th><th>세액</th></tr></thead><tbody>
        <tr><td>카드 매출</td><td class="r">20,000,000 원</td><td class="r">2,000,000 원</td></tr>
        <tr class="tot"><td class="l">매출 합계</td><td class="r">20,000,000 원</td><td class="r">2,000,000 원</td></tr></tbody></table>
        <h3>매입세액</h3>
        <table><thead><tr><th>구분</th><th>공급가액</th><th>세액</th></tr></thead><tbody>
        <tr><td>일반 매입</td><td class="r">1,500,000 원</td><td class="r">150,000 원</td></tr>
        <tr><td>고정자산 매입</td><td class="r">2,200,000 원</td><td class="r">220,000 원</td></tr>
        <tr class="tot"><td class="l">매입 합계</td><td class="r">3,700,000 원</td><td class="r">370,000 원</td></tr></tbody></table>
        <h3>납부세액</h3>
        <table>
        <tr><td class="l">차감 납부세액</td><td class="r">1,630,000 원</td></tr>
        <tr><td class="l">예정고지세액</td><td class="r">0 원</td></tr>
        <tr class="tot"><td class="l">최종 납부세액</td><td class="r">1,630,000 원</td></tr></table>
        <p class="disc">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</p>
        """);
        pdf("부가세_2025_1기.pdf", html);
    }

    @Test void genIncomeTaxPdf() throws Exception {
        Files.createDirectories(OUT);
        String html = pdfWrap("A4", """
        <h1>종합소득세 신고서</h1>
        <p class="sh">2025년 귀속 | 7iTAX Simulation</p>
        <h3>납세자 정보</h3>
        <table>
        <tr><td class="l">성명</td><td>홍길동</td><td class="l">주민등록번호</td><td>900101-*******</td></tr>
        <tr><td class="l">상호</td><td>길동소프트</td><td class="l">사업자등록번호</td><td>123-45-67890</td></tr></table>
        <h3>소득 내역</h3>
        <table>
        <tr><td class="l">총수입금액</td><td class="r">80,000,000 원</td><td class="l">필요경비</td><td class="r">45,000,000 원</td></tr>
        <tr><td class="l">소득금액</td><td class="r" colspan="3">35,000,000 원</td></tr></table>
        <h3>세액 계산</h3>
        <table>
        <tr><td class="l">과세표준</td><td class="r">33,500,000 원</td><td class="l">적용세율</td><td class="r">24%</td></tr>
        <tr><td class="l">산출세액</td><td class="r">4,780,000 원</td><td class="l">세액공제</td><td class="r">200,000 원</td></tr>
        <tr><td class="l">결정세액</td><td class="r">4,580,000 원</td><td class="l">기납부세액</td><td class="r">2,400,000 원</td></tr>
        <tr><td class="l">납부할 국세</td><td class="r">2,180,000 원</td><td class="l">지방소득세</td><td class="r">458,000 원</td></tr>
        <tr class="tot"><td class="l">총 납부세액</td><td class="r" colspan="3">2,638,000 원</td></tr></table>

        <h3>수입/경비 명세서</h3>
        <table><thead><tr><th>항목</th><th>금액</th></tr></thead><tbody>
        <tr class="tot"><td class="l">총수입금액</td><td class="r">80,000,000 원</td></tr>
        <tr><td>복리후생비</td><td class="r">2,400,000 원</td></tr>
        <tr><td>접대비</td><td class="r">3,600,000 원</td></tr>
        <tr><td>임차료</td><td class="r">12,000,000 원</td></tr>
        <tr><td>지급수수료</td><td class="r">3,960,000 원</td></tr>
        <tr><td>차량유지비</td><td class="r">9,600,000 원</td></tr>
        <tr class="tot"><td class="l">경비 합계</td><td class="r">45,000,000 원</td></tr></tbody></table>

        <div style="page-break-before:always"></div>
        <h1>간편장부 거래내역</h1>
        <p class="sh">홍길동 | 2025년 귀속</p>
        <table><thead><tr><th>일자</th><th>계정과목</th><th>거래내용</th><th>거래처</th><th>수입</th><th>비용</th><th>고정자산</th><th>부가세</th></tr></thead>
        <tbody>""" + LEDGER_ROWS + """
        </tbody></table>
        <p class="disc">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</p>
        """);
        pdf("종합소득세_2025.pdf", html);
    }

    // ════════════════════════════════════════════
    //  유틸리티
    // ════════════════════════════════════════════

    private String pdfWrap(String pageSize, String body) {
        return """
        <!DOCTYPE html><html><head><meta charset="UTF-8"/>
        <style>
        @page { size: %s; margin: 15mm; }
        body { font-family: 'NanumGothic', sans-serif; font-size: 9pt; line-height: 1.3; color: #000; }
        h1 { text-align: center; font-size: 16pt; margin-bottom: 4px; }
        h3 { font-size: 11pt; margin-top: 16px; margin-bottom: 6px; border-bottom: 1px solid #000; padding-bottom: 3px; }
        .sh { text-align: center; font-size: 9pt; color: #555; margin-bottom: 12px; }
        table { width: 100%%; border-collapse: collapse; margin-bottom: 10px; }
        th, td { border: 1px solid #333; padding: 3px 6px; font-size: 8pt; }
        th { background: #d9e2f3; text-align: center; font-weight: bold; }
        .l { background: #f5f5f5; font-weight: bold; width: 20%%; }
        .r { text-align: right; } .c { text-align: center; }
        .tot td { font-weight: bold; background: #f0f0f0; border-top: 2px solid #000; }
        .info td { border: 1px solid #999; padding: 3px 8px; }
        .info .l { background: #f0f0f0; width: 12%%; }
        .disc { margin-top: 16px; padding: 6px; border: 1px solid #999; font-size: 7pt; color: #555; text-align: center; }
        </style></head><body>
        """.formatted(pageSize) + body + "</body></html>";
    }

    private void csvBom(String name, String content) throws Exception {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + data.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(data, 0, out, bom.length, data.length);
        Files.write(OUT.resolve(name), out);
        System.out.println("  CSV: " + name);
    }

    private void pdf(String name, String html) throws Exception {
        try (var os = new ByteArrayOutputStream()) {
            var b = new PdfRendererBuilder();
            b.useFastMode();
            b.useFont(() -> getClass().getResourceAsStream(FONT_R), "NanumGothic", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            b.useFont(() -> getClass().getResourceAsStream(FONT_B), "NanumGothic", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            b.withHtmlContent(html, "/");
            b.toStream(os);
            b.run();
            Files.write(OUT.resolve(name), os.toByteArray());
            System.out.println("  PDF: " + name);
        }
    }

    private Workbook loadTpl(String path) throws Exception {
        try (var is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new FileNotFoundException(path);
            return WorkbookFactory.create(is);
        }
    }

    private void writeWb(Workbook wb, String name) throws Exception {
        try (var os = new ByteArrayOutputStream()) {
            wb.write(os);
            Files.write(OUT.resolve(name), os.toByteArray());
            System.out.println("  XLSX: " + name);
        } finally { wb.close(); }
    }

    private void cell(Sheet s, int r, int c, Object v) {
        Row row = s.getRow(r); if (row == null) row = s.createRow(r);
        Cell cell = row.getCell(c); if (cell == null) cell = row.createCell(c);
        if (v instanceof Number n) cell.setCellValue(n.doubleValue());
        else if (v instanceof String str) cell.setCellValue(str);
    }
}
