package com.ssafy.seveniTax.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.MainViewModel

// ── 색상 ──
private val CardBg = Color(0xFFF8F8FB)
private val UnconfirmedBg = Color(0xFFF6F3FF)
private val IncomeBg = Color(0xFFEDF8F6)
private val IncomeLabel = Color(0xFF7ABFB3)
private val ExpenseBg = Color(0xFFFFF0F3)
private val ExpenseLabel = Color(0xFFFF9DAE)
private val TaxBarBg = Color(0xFFFFF8EE)
private val TaxBarAccent = Color(0xFFE0A44A)
private val CardShadow = Color(0x123629B7)
private val NoticeBg = Color(0xFF1F2430)

private data class ActionItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val route: String
)

@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onNotificationClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    notificationCount: Int = 0
) {
    val userName = viewModel.getUserName().ifBlank { "사용자" }

    // 장부 데이터
    val bookVM: com.ssafy.seveniTax.viewmodel.BookEntryViewModel = hiltViewModel()
    val entries by bookVM.entries.collectAsState()
    val unclassifiedCount by bookVM.unconfirmedCount.collectAsState()
    LaunchedEffect(Unit) { bookVM.loadUnconfirmedCount(); bookVM.loadEntries() }

    val now = java.time.LocalDate.now()
    val monthEntries = entries.filter {
        try { val d = java.time.LocalDate.parse(it.entryDate); d.year == now.year && d.monthValue == now.monthValue }
        catch (_: Exception) { false }
    }
    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
    val totalIncome = monthEntries.filter { it.entryType == "INCOME" }.sumOf { it.incomeAmount }
    val totalExpense = monthEntries.filter { it.entryType == "EXPENSE" }.sumOf { it.expenseAmount }

    // 세금 일정
    val taxVM: com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel = hiltViewModel()
    val deadlines by taxVM.deadlines.collectAsState()
    val upcoming = deadlines.filter { it.dDay >= 0 }.sortedBy { it.dDay }.take(2)
    val nearest = upcoming.firstOrNull()

    // 세율 구간 계산
    val yearIncome = entries.filter {
        try { java.time.LocalDate.parse(it.entryDate).year == now.year } catch (_: Exception) { false }
    }.filter { it.entryType == "INCOME" }.sumOf { it.incomeAmount }
    val yearExpense = entries.filter {
        try { java.time.LocalDate.parse(it.entryDate).year == now.year } catch (_: Exception) { false }
    }.filter { it.entryType == "EXPENSE" }.sumOf { it.expenseAmount }
    val taxableIncome = (yearIncome - yearExpense).coerceAtLeast(0)

    val actions = listOf(
        ActionItem("스케쥴러", R.drawable.ic_home_scheduler, Route.TaxCalendar.path),
        ActionItem("간편장부", R.drawable.ic_home_book, Route.BookEntryList.path),
        ActionItem("레포트", R.drawable.ic_home_report, Route.TaxReport.path),
        ActionItem("증빙서류", R.drawable.ic_home_export, Route.ExportPurpose.path),
        ActionItem("QR 결제", R.drawable.ic_home_qr, Route.QrPayment.path),
        ActionItem("공제 혜택", R.drawable.ic_home_deduction, Route.TaxSavingsDetail.path),
    )

    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(BrandPurple, BrandPurple, Color.White), startY = 0f, endY = 420f)
        )
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
        ) {
            // ── 헤더 ──
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("안녕하세요, ${userName}님", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                    Box(Modifier.clickable { onNotificationClick() }) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Color.White, modifier = Modifier.padding(top = 6.dp).size(24.dp))
                        if (notificationCount > 0) {
                            Box(Modifier.size(16.dp).clip(CircleShape).background(Error).align(Alignment.TopEnd), contentAlignment = Alignment.Center) {
                                Text(notificationCount.toString(), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.padding(top = 6.dp).size(24.dp).clickable { onMenuClick() })
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── 메인 콘텐츠 ──
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp, 30.dp)).background(Color.White).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // ❶ 미분류 경비
                    if (unclassifiedCount > 0) {
                        Row(
                            Modifier.fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = CardShadow, spotColor = CardShadow)
                                .background(UnconfirmedBg, RoundedCornerShape(22.dp))
                                .clickable { navController.navigate(Route.UnclassifiedList.path) }
                                .padding(18.dp, 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("미분류 경비", fontSize = 14.sp, color = TextSecondary)
                                Spacer(Modifier.height(6.dp))
                                Text("${unclassifiedCount}건", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                                Spacer(Modifier.height(4.dp))
                                Text("신고 전에 분류와 확인이 필요한 내역이 있어요", fontSize = 12.sp, color = TextSecondary)
                            }
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(LogoPurple).padding(14.dp, 10.dp)) {
                                Text("지금 확인", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }

                    // ❷ 액션 그리드
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        actions.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { item ->
                                    Card(
                                        Modifier.weight(1f).aspectRatio(1f).clickable { navController.navigate(item.route) },
                                        shape = RoundedCornerShape(15.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(painterResource(item.iconRes), null, Modifier.size(34.dp), tint = Color.Unspecified)
                                            Spacer(Modifier.height(14.dp))
                                            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }

                    // ❹ 한눈에 보기 (수입/비용 + 예상 세금 바)
                    SummaryCard(totalIncome, totalExpense, nearest, fmt, navController)

                    // ❺ 다가오는 신고 일정 (가까운 2개)
                    SectionCard("다가오는 신고 일정", "전체 보기", { navController.navigate(Route.TaxCalendar.path) }) {
                        upcoming.forEachIndexed { i, dl ->
                            val date = try { val d = java.time.LocalDate.parse(dl.deadline); "${d.monthValue}월 ${d.dayOfMonth}일" } catch (_: Exception) { dl.deadline }
                            val ddayColor = when { dl.dDay <= 3 -> DdayError; dl.dDay <= 7 -> DdayWarning; else -> DdayNormal }
                            Row(
                                Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(16.dp)).padding(14.dp, 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(dl.taxName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(date, fontSize = 12.sp, color = TextSecondary)
                                }
                                Text(if (dl.dDay == 0) "D-Day" else "D-${dl.dDay}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ddayColor)
                            }
                            if (i < upcoming.lastIndex) Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ❻ 절세 포인트
                    SectionCard("이번 달 절세 포인트", "리포트 보기", { navController.navigate(Route.TaxReport.path) }) {
                        InsightItem("경비 처리 누락 가능 거래 ${unclassifiedCount}건", "정리하면 절세 여지를 확인할 수 있어요.")
                        Spacer(Modifier.height(12.dp))
                        InsightItem("신고 전 검토 추천", "미분류 경비를 먼저 처리하면 신고 누락 위험을 줄일 수 있어요.")
                    }
                }
            }
        }
    }
}

// ── 한눈에 보기 (수입/비용 + 예상 세금 바) ──
@Composable
private fun SummaryCard(income: Long, expense: Long, nearest: com.ssafy.seveniTax.data.model.tax.TaxDeadline?, fmt: java.text.NumberFormat, navController: NavController) {
    SectionCard("한눈에 보기", "상세", { navController.navigate(Route.TaxReport.path) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).background(IncomeBg, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("수입", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IncomeLabel, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(8.dp))
                Text("+${fmt.format(income)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column(Modifier.weight(1f).background(ExpenseBg, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("비용", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ExpenseLabel, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(8.dp))
                Text("-${fmt.format(expense)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        if (nearest != null) {
            val taxable = (income - expense).coerceAtLeast(0)
            val estimatedVat = (income / 10 - expense / 10).coerceAtLeast(0)
            val estimatedIncome = when {
                taxable <= 14_000_000 -> (taxable * 0.06).toLong()
                taxable <= 50_000_000 -> (taxable * 0.15 - 1_260_000).toLong()
                taxable <= 88_000_000 -> (taxable * 0.24 - 5_760_000).toLong()
                else -> (taxable * 0.35 - 15_440_000).toLong()
            }
            val isVat = nearest.taxName.contains("부가")
            val estimatedTax = if (isVat) estimatedVat else estimatedIncome
            val taxLabel = if (isVat) "예상 부가가치세" else "예상 종합소득세"

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().background(TaxBarBg, RoundedCornerShape(16.dp)).clickable { navController.navigate(Route.TaxCalendar.path) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(taxLabel, fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${fmt.format(estimatedTax)}원", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.background(TaxBarAccent.copy(0.15f), RoundedCornerShape(20.dp)).padding(8.dp, 2.dp)) {
                            Text("D-${nearest.dDay}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TaxBarAccent)
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFCACACA))
            }
        }
    }
}

// ── 세율 구간 카드 (세무 리포트와 동일 스타일) ──
@Composable
private fun TaxBracketCard(taxableIncome: Long, fmt: java.text.NumberFormat) {
    data class Bracket(val limit: Long, val rate: Int, val label: String)
    val brackets = listOf(
        Bracket(14_000_000, 6, "~1,400만"),
        Bracket(50_000_000, 15, "~5,000만"),
        Bracket(88_000_000, 24, "~8,800만"),
        Bracket(150_000_000, 35, "~1.5억")
    )
    val maxLimit = 150_000_000L
    val currentRate = when {
        taxableIncome <= 14_000_000 -> 6
        taxableIncome <= 50_000_000 -> 15
        taxableIncome <= 88_000_000 -> 24
        taxableIncome <= 150_000_000 -> 35
        else -> 38
    }
    val currentLabel = brackets.lastOrNull { taxableIncome >= it.limit }?.label
        ?: brackets.first().label
    val nextBracket = brackets.firstOrNull { taxableIncome < it.limit }
    val progress = (taxableIncome.toFloat() / maxLimit).coerceIn(0f, 1f)

    Column(
        Modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
            // 상단: 과세표준 + 금액
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("현재 과세표준", fontSize = 13.sp, color = TextSecondary)
                Text("${fmt.format(taxableIncome)}원", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))

            // 구간 뱃지 + 범위
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Surface, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${currentRate}% 구간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandPurple)
                }
                Spacer(Modifier.width(8.dp))
                Text("($currentLabel)", fontSize = 11.sp, color = TextSecondary)
            }

            Spacer(Modifier.height(12.dp))

            // 프로그레스 바 + 세율 마커
            Box(Modifier.fillMaxWidth().height(24.dp)) {
                Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(4.dp)).background(BrandPurple))
                }
                brackets.forEach { bracket ->
                    val pos = (bracket.limit.toFloat() / maxLimit).coerceIn(0f, 1f)
                    Text(
                        text = "${bracket.rate}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (currentRate == bracket.rate) BrandPurple else TextSecondary,
                        modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(pos).wrapContentWidth(Alignment.End)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("0원", fontSize = 10.sp, color = TextSecondary)
                Text("1억 5,000만원", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text("간편장부 대상 한도 (정보통신업)", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End)

            // 다음 구간 안내
            if (nextBracket != null) {
                val remaining = nextBracket.limit - taxableIncome
                if (remaining > 0) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().background(TaxBarBg, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text("다음 구간까지", fontSize = 12.sp, color = TaxBarAccent)
                        Text("${fmt.format(remaining)}원 더 벌면 ${nextBracket.rate}% 구간 (${nextBracket.label})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }

        // 한도 초과 경고
        if (taxableIncome >= maxLimit) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().background(Color(0xFFFFF0F3), RoundedCornerShape(8.dp)).padding(12.dp)) {
                Text("간편장부 한도를 초과했습니다. 복식부기 전환을 권장합니다.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE8475A))
            }
        }
    }
}


// ── 섹션 카드 ──
@Composable
private fun SectionCard(title: String, action: String, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1A3629B7), spotColor = Color(0x1A3629B7))
            .border(1.dp, Color(0x0D3629B7), RoundedCornerShape(22.dp))
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(action, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandPurple, modifier = Modifier.clickable { onClick() })
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

// ── 절세 포인트 ──
@Composable
private fun InsightItem(title: String, desc: String) {
    Column(Modifier.fillMaxWidth().background(TaxBarBg, RoundedCornerShape(16.dp)).padding(14.dp, 14.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(desc, fontSize = 12.sp, color = TextSecondary)
    }
}
