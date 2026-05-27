package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel
import com.ssafy.seveniTax.viewmodel.EntryFilter
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.NumberFormat
import java.util.Locale

private enum class ReportTab(val label: String) {
    MONTHLY("월간"), ANNUAL("연간")
}

@Composable
fun TaxReportScreen(navController: NavController, bookEntryViewModel: BookEntryViewModel? = null) {
    var selectedTab by rememberSaveable { mutableStateOf(ReportTab.MONTHLY) }
    var selectedYear by rememberSaveable { mutableIntStateOf(java.time.LocalDate.now().year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(java.time.LocalDate.now().monthValue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text("세무 리포트", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // 탭
        Row(modifier = Modifier.fillMaxWidth()) {
            ReportTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandPurple else TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) BrandPurple else Color.Transparent)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            when (selectedTab) {
                ReportTab.MONTHLY -> {
                    val vm = bookEntryViewModel
                    val income = vm?.getIncomeFor(selectedYear, selectedMonth) ?: 0L
                    val expense = vm?.getExpenseFor(selectedYear, selectedMonth) ?: 0L
                    val expenseByCategory = vm?.getExpenseByCategoryFor(selectedYear, selectedMonth) ?: emptyList()
                    val incomeByMerchant = vm?.getIncomeByMerchantFor(selectedYear, selectedMonth) ?: emptyList()
                    val trend = vm?.getMonthlyTrend(selectedYear, selectedMonth) ?: emptyList()

                    MonthlyReport(
                        year = selectedYear,
                        month = selectedMonth,
                        income = income,
                        expense = expense,
                        expenseByCategory = expenseByCategory,
                        incomeByMerchant = incomeByMerchant,
                        trend = trend,
                        onPrev = {
                            if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- }
                            else selectedMonth--
                        },
                        onNext = {
                            if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ }
                            else selectedMonth++
                        },
                        onIncomeClick = {
                            vm?.apply {
                                selectYear(selectedYear)
                                selectMonth(selectedMonth)
                                selectFilter(EntryFilter.INCOME)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        },
                        onExpenseClick = {
                            vm?.apply {
                                selectYear(selectedYear)
                                selectMonth(selectedMonth)
                                selectFilter(EntryFilter.EXPENSE)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        },
                        onSavingsClick = {
                            navController.navigate(Route.TaxSavingsDetail.path)
                        },
                        onDotClick = { monthLabel, isIncome ->
                            val m = monthLabel.replace("월", "").toIntOrNull() ?: return@MonthlyReport
                            vm?.apply {
                                selectYear(selectedYear)
                                selectMonth(m)
                                selectFilter(if (isIncome) EntryFilter.INCOME else EntryFilter.EXPENSE)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        },
                        onCategoryClick = { category ->
                            vm?.apply {
                                selectYear(selectedYear)
                                selectMonth(selectedMonth)
                                selectFilter(EntryFilter.EXPENSE)
                                applyFilter(setOf(category), "", false, false)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        },
                        onMerchantClick = { merchant ->
                            vm?.apply {
                                selectYear(selectedYear)
                                selectMonth(selectedMonth)
                                selectFilter(EntryFilter.INCOME)
                                applyFilter(setOf("전체"), merchant, false, false)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        }
                    )
                }
                ReportTab.ANNUAL -> {
                    val vm = bookEntryViewModel
                    val annualIncome = vm?.getAnnualIncome(selectedYear) ?: 0L
                    val annualExpense = vm?.getAnnualExpense(selectedYear) ?: 0L
                    val prevIncome = vm?.getAnnualIncome(selectedYear - 1) ?: 0L
                    val prevExpense = vm?.getAnnualExpense(selectedYear - 1) ?: 0L
                    val annualExpByCat = vm?.getAnnualExpenseByCategory(selectedYear) ?: emptyList()
                    val annualIncByMerchant = vm?.getAnnualIncomeByMerchant(selectedYear) ?: emptyList()
                    val annualTrend = vm?.getAnnualMonthlyTrend(selectedYear) ?: emptyList()

                    AnnualReport(
                        year = selectedYear,
                        income = annualIncome,
                        expense = annualExpense,
                        prevIncome = prevIncome,
                        prevExpense = prevExpense,
                        expenseByCategory = annualExpByCat,
                        incomeByMerchant = annualIncByMerchant,
                        trend = annualTrend,
                        onPrev = { selectedYear-- },
                        onNext = { selectedYear++ },
                        onSavingsClick = { navController.navigate(Route.TaxSavingsDetail.path) },
                        onDotClick = { monthLabel, isIncome ->
                            val m = monthLabel.replace("월", "").toIntOrNull() ?: return@AnnualReport
                            vm?.apply {
                                selectYear(selectedYear)
                                selectMonth(m)
                                selectFilter(if (isIncome) EntryFilter.INCOME else EntryFilter.EXPENSE)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        },
                        onCategoryClick = { category ->
                            vm?.apply {
                                selectYear(selectedYear)
                                selectFilter(EntryFilter.EXPENSE)
                                applyFilter(setOf(category), "", false, false)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        },
                        onMerchantClick = { merchant ->
                            vm?.apply {
                                selectYear(selectedYear)
                                selectFilter(EntryFilter.INCOME)
                                applyFilter(setOf("전체"), merchant, false, false)
                            }
                            navController.navigate(Route.BookEntryList.path)
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── 월간 리포트 ─────────────────────────────────────

@Composable
private fun MonthlyReport(
    year: Int, month: Int,
    income: Long, expense: Long,
    expenseByCategory: List<Pair<String, Long>>,
    incomeByMerchant: List<Pair<String, Long>>,
    trend: List<Triple<String, Long, Long>>,
    onPrev: () -> Unit, onNext: () -> Unit,
    onIncomeClick: () -> Unit = {}, onExpenseClick: () -> Unit = {},
    onSavingsClick: () -> Unit = {},
    onDotClick: (monthLabel: String, isIncome: Boolean) -> Unit = { _, _ -> },
    onCategoryClick: (String) -> Unit = {},
    onMerchantClick: (String) -> Unit = {}
) {
    val net = income - expense
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

    Spacer(Modifier.height(16.dp))
    DateNavigator(text = "${year}년 ${month}월", onPrev = onPrev, onNext = onNext)
    Spacer(Modifier.height(20.dp))

    // 순이익 카드
    ReportSummaryCard("월간 순이익", net, income, -expense,
        onIncomeClick = onIncomeClick, onExpenseClick = onExpenseClick)

    Spacer(Modifier.height(24.dp))

    // 월별 추이
    if (trend.isNotEmpty()) {
        SectionTitle("월별 추이")
        Spacer(Modifier.height(12.dp))
        TrendLineChart(trend, onDotClick = onDotClick)
        Spacer(Modifier.height(24.dp))
    }

    // 계정과목별 비용
    if (expenseByCategory.isNotEmpty()) {
        SectionTitle("계정과목별 비용")
        Spacer(Modifier.height(12.dp))
        DonutChart(expenseByCategory, onCategoryClick = onCategoryClick)
        Spacer(Modifier.height(24.dp))
    }

    // 거래처별 수입
    if (incomeByMerchant.isNotEmpty()) {
        SectionTitle("거래처별 수입")
        Spacer(Modifier.height(12.dp))
        IncomeBarChart(incomeByMerchant, onMerchantClick = onMerchantClick)
    }

    Spacer(Modifier.height(24.dp))

    // 세금 추정
    val vatSales = (income * 0.1).toLong()
    val vatPurchase = (expense * 0.1).toLong()
    val vatPayable = vatSales - vatPurchase
    val taxableIncome = income - expense
    val incomeTax = (taxableIncome * 0.15).toLong()
    val localTax = (incomeTax * 0.1).toLong()
    val totalTax = vatPayable + incomeTax + localTax

    SectionTitle("월간 세금 추정")
    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("부가가치세", listOf(
        "매출세액" to "${fmt.format(vatSales)}원",
        "매입세액" to "-${fmt.format(vatPurchase)}원",
        "예상 납부액" to "${fmt.format(vatPayable)}원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("종합소득세", listOf(
        "월간 수입" to "${fmt.format(income)}원",
        "월간 경비" to "-${fmt.format(expense)}원",
        "월간 예상 소득세" to "${fmt.format(incomeTax)}원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("지방소득세", listOf(
        "금액" to "${fmt.format(localTax)}원",
        "비고" to "종합소득세의 10%"
    ))

    Spacer(Modifier.height(12.dp))

    TotalTaxBox("월간 총 예상 세금", "${fmt.format(totalTax)}원")

    Spacer(Modifier.height(16.dp))

    // 추가 절세 가능 금액
    val unconfirmedExpense = expense / 5  // 미확인 경비 약 20% 가정
    val saveable = (unconfirmedExpense * 0.15).toLong()
    SavingsHintBox(
        unconfirmedAmount = unconfirmedExpense,
        saveableAmount = saveable,
        onClick = onSavingsClick
    )
}

// ─── 연간 리포트 ────────────────────────────────────────

@Composable
private fun AnnualReport(
    year: Int,
    income: Long, expense: Long,
    prevIncome: Long, prevExpense: Long,
    expenseByCategory: List<Pair<String, Long>>,
    incomeByMerchant: List<Pair<String, Long>>,
    trend: List<Triple<String, Long, Long>>,
    onPrev: () -> Unit, onNext: () -> Unit,
    onSavingsClick: () -> Unit = {},
    onDotClick: (monthLabel: String, isIncome: Boolean) -> Unit = { _, _ -> },
    onCategoryClick: (String) -> Unit = {},
    onMerchantClick: (String) -> Unit = {}
) {
    val net = income - expense
    val prevNet = prevIncome - prevExpense
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

    fun pctChange(cur: Long, prev: Long): String {
        if (prev == 0L) return if (cur > 0) "신규" else "-"
        val pct = ((cur - prev) * 100 / prev).toInt()
        return if (pct >= 0) "+${pct}%" else "${pct}%"
    }
    fun fmtWon(v: Long): String = "${fmt.format(v)}원"

    Spacer(Modifier.height(16.dp))
    DateNavigator(text = "${year}년", onPrev = onPrev, onNext = onNext)
    Spacer(Modifier.height(20.dp))

    // 1. 아낄 수 있는 돈
    val saveable = (expense / 5 * 0.15).toLong()
    SavingsHintBox(unconfirmedAmount = expense / 5, saveableAmount = saveable, onClick = onSavingsClick)

    Spacer(Modifier.height(24.dp))

    // 2. 내 세율 구간
    SectionTitle("내 세율 구간")
    Spacer(Modifier.height(12.dp))
    TaxBracketCard(income = income, expense = expense)

    Spacer(Modifier.height(24.dp))

    // 3. 연간 순이익
    ReportSummaryCard("연간 순이익", net, income, -expense)

    Spacer(Modifier.height(24.dp))

    // 4. 전년 대비
    SectionTitle("전년 대비")
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ComparisonBox("수입", pctChange(income, prevIncome), "${fmtWon(prevIncome)} → ${fmtWon(income)}", Color(0xFFEDF8F6), Color(0xFF3DBDA2), Modifier.weight(1f))
        ComparisonBox("비용", pctChange(expense, prevExpense), "${fmtWon(prevExpense)} → ${fmtWon(expense)}", Color(0xFFFFF0F3), Color(0xFFE8475A), Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    ComparisonBox("순이익", pctChange(net, prevNet), "${fmtWon(prevNet)} → ${fmtWon(net)}", Surface, BrandPurple, Modifier.fillMaxWidth())

    Spacer(Modifier.height(24.dp))

    // 5. 월별 추이 (12개월)
    if (trend.isNotEmpty()) {
        SectionTitle("월별 추이")
        Spacer(Modifier.height(12.dp))
        TrendLineChart(trend, onDotClick = onDotClick)
        Spacer(Modifier.height(24.dp))
    }

    // 6. 연간 계정과목별 비용
    if (expenseByCategory.isNotEmpty()) {
        SectionTitle("연간 계정과목별 비용")
        Spacer(Modifier.height(12.dp))
        DonutChart(expenseByCategory, onCategoryClick = onCategoryClick)
        Spacer(Modifier.height(24.dp))
    }

    // 7. 거래처별 수입
    if (incomeByMerchant.isNotEmpty()) {
        SectionTitle("거래처별 수입")
        Spacer(Modifier.height(12.dp))
        IncomeBarChart(incomeByMerchant, onMerchantClick = onMerchantClick)
        Spacer(Modifier.height(24.dp))
    }

    // 8. 연간 세금 예상액
    val vatSales = (income * 0.1).toLong()
    val vatPurchase = (expense * 0.1).toLong()
    val vatPayable = vatSales - vatPurchase
    val taxableIncome = income - expense
    val taxRate = when {
        taxableIncome <= 14_000_000 -> 6
        taxableIncome <= 50_000_000 -> 15
        taxableIncome <= 88_000_000 -> 24
        taxableIncome <= 150_000_000 -> 35
        else -> 38
    }
    val incomeTax = (taxableIncome * taxRate / 100)
    val localTax = incomeTax / 10
    val totalTax = vatPayable + incomeTax + localTax

    SectionTitle("연간 세금 예상액")
    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("부가가치세", listOf(
        "매출세액" to "${fmt.format(vatSales)}원",
        "매입세액" to "-${fmt.format(vatPurchase)}원",
        "납부액" to "${fmt.format(vatPayable)}원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("종합소득세", listOf(
        "총 수입" to "${fmt.format(income)}원",
        "필요경비" to "-${fmt.format(expense)}원",
        "과세표준" to "${fmt.format(taxableIncome)}원",
        "적용 세율" to "${taxRate}%",
        "예상 납부액" to "${fmt.format(incomeTax)}원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("지방소득세", listOf(
        "금액" to "${fmt.format(localTax)}원",
        "비고" to "종합소득세의 10%"
    ))

    Spacer(Modifier.height(24.dp))

    // 올해 공제 요약
    SectionTitle("올해 공제 요약")
    Spacer(Modifier.height(12.dp))
    DeductionSummaryCard()

    Spacer(Modifier.height(24.dp))

    TotalTaxBox("연간 총 예상 세금", "${fmt.format(totalTax)}원")
}

// ─── 날짜 네비게이터 ─────────────────────────────────────

@Composable
private fun DateNavigator(text: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "이전", tint = TextPrimary)
        }
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPurple
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "다음", tint = TextPrimary)
        }
    }
}

// ─── 공통 컴포넌트 ──────────────────────────────────────

@Composable
private fun ReportSummaryCard(
    label: String, net: Long, income: Long, expense: Long,
    onIncomeClick: () -> Unit = {}, onExpenseClick: () -> Unit = {}
) {
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = Color(0x123629B7))
            .background(Color.White, RoundedCornerShape(15.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("+${fmt.format(net)}원", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDF8F6)).clickable { onIncomeClick() }.padding(12.dp)) {
                    Column { Text("총 수입", fontSize = 11.sp, color = Color(0xFF7ABFB3)); Text("+${fmt.format(income)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF0F3)).clickable { onExpenseClick() }.padding(12.dp)) {
                    Column { Text("총 비용", fontSize = 11.sp, color = Color(0xFFFF9DAE)); Text("${fmt.format(expense)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
}

@Composable
private fun TaxEstimateItem(title: String, items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            items.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 13.sp, color = TextSecondary)
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun TotalTaxBox(label: String, amount: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
        }
    }
}

@Composable
private fun ComparisonBox(label: String, percent: String, detail: String, bg: Color, textColor: Color, modifier: Modifier) {
    Box(modifier.background(bg, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Column {
            Text(label, fontSize = 11.sp, color = textColor)
            Spacer(Modifier.height(4.dp))
            Text(percent, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(Modifier.height(2.dp))
            Text(detail, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TaxBracketCard(income: Long = 0, expense: Long = 0) {
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    val maxLimit = 150_000_000L // 간편장부 한도 1억 5천
    val taxable = (income - expense).coerceAtLeast(0)

    data class Bracket(val limit: Long, val rate: Int)
    val brackets = listOf(
        Bracket(14_000_000, 6),
        Bracket(50_000_000, 15),
        Bracket(88_000_000, 24),
        Bracket(150_000_000, 35)
    )

    val currentBracket = brackets.lastOrNull { taxable >= it.limit }
        ?: brackets.first()
    val currentRate = when {
        taxable <= 14_000_000 -> 6
        taxable <= 50_000_000 -> 15
        taxable <= 88_000_000 -> 24
        taxable <= 150_000_000 -> 35
        else -> 38
    }
    val nextBracket = brackets.firstOrNull { taxable < it.limit }
    val progress = (taxable.toFloat() / maxLimit).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("현재 과세표준", fontSize = 13.sp, color = TextSecondary)
                Text("${fmt.format(taxable)}원", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.background(Surface, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("${currentRate}% 구간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandPurple)
            }
            Spacer(Modifier.height(12.dp))
            // 프로그레스 바 (최대 1억 5천) + 세율 구간 표기
            Box(Modifier.fillMaxWidth().height(24.dp)) {
                // 바 배경
                Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(4.dp)).background(BrandPurple))
                }
                // 세율 구간 마커
                brackets.forEach { bracket ->
                    val pos = (bracket.limit.toFloat() / maxLimit).coerceIn(0f, 1f)
                    Text(
                        text = "${bracket.rate}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (currentRate == bracket.rate) BrandPurple else TextSecondary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth(pos)
                            .wrapContentWidth(Alignment.End)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0원", fontSize = 10.sp, color = TextSecondary)
                Text("1억 5,000만원", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text("간편장부 대상 한도 (정보통신업)", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)

            if (nextBracket != null) {
                Spacer(Modifier.height(12.dp))
                val remaining = nextBracket.limit - taxable
                Box(Modifier.fillMaxWidth().background(Color(0xFFFFF8EE), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Column {
                        Text("다음 구간까지", fontSize = 12.sp, color = Color(0xFFE0A44A))
                        Text("${fmt.format(remaining)}원 더 벌면 ${nextBracket.rate}% 구간", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }

            if (income >= maxLimit) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().background(Color(0xFFFFF0F3), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text("간편장부 한도를 초과했습니다. 복식부기 전환을 권장합니다.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE8475A))
                }
            }
        }
    }
}

@Composable
private fun SavingsHintBox(unconfirmedAmount: Long, saveableAmount: Long, onClick: () -> Unit = {}) {
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFF6F3FF), Color(0xFFEDE8FF))),
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "아낄 수 있는 돈",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPurple
            )
            Text(
                "${fmt.format(saveableAmount)}원",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPurple
            )
        }
    }
}

@Composable
private fun DeductionSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            listOf(
                "경비 인정 금액" to "20,000,000원",
                "부가세 매입공제" to "2,000,000원",
                "경비 절세 효과" to "3,000,000원"
            ).forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 13.sp, color = TextSecondary)
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Surface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("총 절세 금액", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("5,000,000원", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
            }
        }
    }
}

// ─── 월별 추이 꺾은선 그래프 ──────────────────────────────

@Composable
private fun TrendLineChart(
    trend: List<Triple<String, Long, Long>> = emptyList(),
    onDotClick: (monthLabel: String, isIncome: Boolean) -> Unit = { _, _ -> }
) {
    val incomeData = trend.map { it.second / 10000f }
    val expenseData = trend.map { it.third / 10000f }
    val months = trend.map { it.first }
    val maxVal = (incomeData + expenseData).maxOrNull()?.times(1.2f) ?: 1f
    val purple = Color(0xFF5655B9)
    val pink = Color(0xFFFF9DAE)

    // 도트 좌표 저장
    var dotPositions by remember { mutableStateOf(listOf<Triple<Float, Float, Pair<Int, Boolean>>>()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(trend) {
                        detectTapGestures { tapOffset ->
                            val hitRadius = 24.dp.toPx()
                            dotPositions.minByOrNull { (x, y, _) ->
                                val dx = tapOffset.x - x
                                val dy = tapOffset.y - y
                                dx * dx + dy * dy
                            }?.let { (x, y, info) ->
                                val dx = tapOffset.x - x
                                val dy = tapOffset.y - y
                                if (dx * dx + dy * dy < hitRadius * hitRadius) {
                                    val (index, isIncome) = info
                                    if (index in months.indices) {
                                        onDotClick(months[index], isIncome)
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height - 24.dp.toPx()
                val stepX = w / (incomeData.size - 1)
                val gridLines = 4

                // 그리드
                for (i in 0..gridLines) {
                    val y = h * i / gridLines
                    drawLine(Color(0xFFF0F0F0), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                }

                // 도트 좌표 수집
                val dots = mutableListOf<Triple<Float, Float, Pair<Int, Boolean>>>()

                // 수입 라인
                val incomePath = Path()
                incomeData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    if (i == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
                }
                drawPath(incomePath, purple, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                incomeData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    dots.add(Triple(x, y, i to true))
                    drawCircle(if (i == incomeData.lastIndex) Color.White else purple, 4.dp.toPx(), Offset(x, y))
                    if (i == incomeData.lastIndex) drawCircle(purple, 4.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx()))
                }

                // 비용 라인
                val expensePath = Path()
                expenseData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    if (i == 0) expensePath.moveTo(x, y) else expensePath.lineTo(x, y)
                }
                drawPath(expensePath, pink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                expenseData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    dots.add(Triple(x, y, i to false))
                    drawCircle(if (i == expenseData.lastIndex) Color.White else pink, 4.dp.toPx(), Offset(x, y))
                    if (i == expenseData.lastIndex) drawCircle(pink, 4.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx()))
                }

                dotPositions = dots

                // 월 라벨
                val paint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                months.forEachIndexed { i, label ->
                    val x = stepX * i
                    paint.color = if (i == months.lastIndex) android.graphics.Color.parseColor("#281C9D") else android.graphics.Color.parseColor("#989898")
                    paint.isFakeBoldText = i == months.lastIndex
                    drawContext.canvas.nativeCanvas.drawText(label, x, size.height, paint)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 범례
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(8.dp).background(purple, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(4.dp))
                Text("수입", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(8.dp).background(pink, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(4.dp))
                Text("비용", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ─── 계정과목별 비용 도넛 차트 ────────────────────────────

@Composable
private fun DonutChart(categoryData: List<Pair<String, Long>> = emptyList(), onCategoryClick: (String) -> Unit = {}) {
    val total = categoryData.sumOf { it.second }.coerceAtLeast(1)
    val donutColors = listOf(
        Color(0xFFE8475A), Color(0xFF5655B9), Color(0xFFF5A623),
        Color(0xFF3DBDA2), Color(0xFF4A90D9), Color(0xFFD4A0E8)
    )
    val segments = categoryData.mapIndexed { i, (name, amount) ->
        Triple(name, amount.toFloat() / total * 100f, donutColors[i % donutColors.size])
    }
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .pointerInput(segments) {
                            detectTapGestures { tapOffset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = tapOffset.x - cx
                                val dy = tapOffset.y - cy
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                val strokeW = 20.dp.toPx()
                                val r = (minOf(size.width, size.height) - strokeW) / 2f
                                if (dist > r - strokeW && dist < r + strokeW) {
                                    var angle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360 + 90) % 360
                                    var cumAngle = 0f
                                    for ((name, pct, _) in segments) {
                                        cumAngle += pct / 100f * 360f
                                        if (angle <= cumAngle) {
                                            onCategoryClick(name)
                                            break
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val strokeW = 20.dp.toPx()
                    var startAngle = -90f
                    segments.forEach { (_, pct, color) ->
                        val sweep = pct / 100f * 360f
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(strokeW / 2, strokeW / 2),
                            size = Size(size.width - strokeW, size.height - strokeW),
                            style = Stroke(strokeW, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("총 비용", fontSize = 11.sp, color = TextSecondary)
                    Text("${fmt.format(total)}원", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 범례 - 2줄
            val chunked = segments.chunked(3)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (label, pct, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCategoryClick(label) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(4.dp))
                            Text("$label ${pct.toInt()}%", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─── 거래처별 수입 막대 그래프 ────────────────────────────

@Composable
private fun IncomeBarChart(merchantData: List<Pair<String, Long>> = emptyList(), onMerchantClick: (String) -> Unit = {}) {
    val totalIncome = merchantData.sumOf { it.second }.coerceAtLeast(1)
    val topItems = if (merchantData.size > 3) {
        val top2 = merchantData.take(2)
        val rest = merchantData.drop(2).sumOf { it.second }
        top2 + listOf("기타" to rest)
    } else merchantData
    val bars = topItems.map { (name, amount) ->
        Triple(name, amount / 10000f, (amount * 100 / totalIncome).toInt())
    }
    val maxVal = (bars.maxOfOrNull { it.second } ?: 1f) * 1.2f
    val colors = listOf(Color(0xFF281C9D), Color(0xFF5655B9), Color(0xFFA8A3D7))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(bars) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val barCount = bars.size
                            val barWidth = w / (barCount * 2 + 1)
                            bars.forEachIndexed { i, (name, _, _) ->
                                val x = barWidth * (i * 2 + 1)
                                if (tapOffset.x in x..(x + barWidth)) {
                                    onMerchantClick(name)
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val barCount = bars.size
                val barWidth = w / (barCount * 2 + 1)
                val gridLines = 4

                // 그리드
                for (i in 0..gridLines) {
                    val y = h * i / gridLines
                    drawLine(Color(0xFFF0F0F0), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                }

                // 바
                bars.forEachIndexed { i, (_, value, _) ->
                    val barH = (value / maxVal) * h
                    val x = barWidth * (i * 2 + 1)
                    drawRoundRect(
                        color = colors[i],
                        topLeft = Offset(x, h - barH),
                        size = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }

                // 값 라벨
                val paint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                bars.forEachIndexed { i, (_, value, _) ->
                    val barH = (value / maxVal) * h
                    val x = barWidth * (i * 2 + 1) + barWidth / 2
                    paint.color = android.graphics.Color.parseColor(
                        when (i) { 0 -> "#281C9D"; 1 -> "#5655B9"; else -> "#A8A3D7" }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${value.toInt()}만",
                        x, h - barH - 6.dp.toPx(),
                        paint
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 라벨
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bars.forEachIndexed { i, (name, _, pct) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onMerchantClick(name) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${pct}%", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
