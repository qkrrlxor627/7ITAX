package com.ssafy.seveniTax.ui.book

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel

// ── 디자인 토큰 (HTML/스타일가이드 기준) ──
private val Primary900 = Color(0xFF281C9D)
private val Primary600 = Color(0xFF5655B9)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral500 = Color(0xFF898989)
private val Neutral400 = Color(0xFF989898)
private val CardShadow = Color(0x123629B7)
private val BarSafe = Color(0xFF52D5BA)
private val BarWarning = Color(0xFFFFAF2A)
private val BarDanger = Color(0xFFFF4267)
private val IconBg = Color(0xFFF5F5F5)

private data class DeductionItem(
    val name: String,
    val sub: String,
    val limit: Long,
    val used: Long,
    @DrawableRes val iconRes: Int
)

@Composable
fun TaxSavingsDetailScreen(navController: NavController, bookEntryViewModel: BookEntryViewModel? = null) {
    val year = java.time.LocalDate.now().year
    val exp = bookEntryViewModel?.getAnnualExpenseByCategory(year)?.toMap() ?: emptyMap()

    val items = listOf(
        DeductionItem("접대비", "연간 한도 1,200만원", 12_000_000,
            (exp["접대비"] ?: 0L).coerceAtMost(12_000_000), R.drawable.ic_deduction_meal),
        DeductionItem("차량유지비", "연간 한도 1,500만원", 15_000_000,
            (exp["차량유지비"] ?: 0L).coerceAtMost(15_000_000), R.drawable.ic_deduction_car),
        DeductionItem("노란우산공제", "소득 4천만 이하 연 500만원", 5_000_000,
            0L, R.drawable.ic_deduction_umbrella),
        DeductionItem("연금저축/IRP", "연간 한도 900만원", 9_000_000,
            0L, R.drawable.ic_deduction_chart),
        DeductionItem("기부금", "지정기부금 소득금액 30%", 3_000_000,
            (exp["기부금"] ?: 0L).coerceAtMost(3_000_000), R.drawable.ic_deduction_heart),
    )

    val tLimit = items.sumOf { it.limit }
    val tUsed = items.sumOf { it.used }
    val tRemain = tLimit - tUsed
    val pct = if (tLimit > 0) (tUsed * 100 / tLimit).toInt() else 0

    Column(Modifier.fillMaxSize().background(Color.White)) {

        // ── Top Nav (HTML 동일) ──
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Neutral900)
            }
            Spacer(Modifier.weight(1f))
            Text("절세 상세", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // ── Summary Header (HTML 동일) ──
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("아직 더 아낄 수 있는 금액",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral500)
                Spacer(Modifier.height(6.dp))
                Text("₩${fmt(tRemain)}",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary900, lineHeight = 38.sp)
                Spacer(Modifier.height(6.dp))
                Text("전체 한도의 ${pct}% 사용 중",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary600)

                Spacer(Modifier.height(20.dp))

                // 요약 박스 (HTML summary-row)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryBox("총 절세 한도", "${fmt(tLimit)}원", Modifier.weight(1f))
                    SummaryBox("사용한 금액", "${fmt(tUsed)}원", Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // 프로그레스 바 (HTML progress-bar-wrap)
                Box(
                    Modifier.fillMaxWidth().height(10.dp)
                        .clip(RoundedCornerShape(5.dp)).background(Color(0xFFF0F0F0))
                ) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(5.dp)).background(Primary600)
                    )
                }
            }

            // ── 항목별 현황 (리포트와 동일한 섹션 카드) ──
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = CardShadow, spotColor = CardShadow)
                    .background(Color.White, RoundedCornerShape(15.dp))
                    .padding(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("항목별 현황", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
                    Text("탭하여 상세 확인", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral400)
                }
                Spacer(Modifier.height(16.dp))

                items.forEachIndexed { index, item ->
                    DeductionRow(item)
                    if (index < items.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF2F1F9))
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

// ── 요약 박스 (HTML summary-box) ──
@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(Primary50, RoundedCornerShape(12.dp)).padding(16.dp, 14.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Neutral400, letterSpacing = 0.3.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Neutral900)
    }
}

// ── 공제 행 (섹션 카드 내부) ──
@Composable
private fun DeductionRow(item: DeductionItem) {
    val pct = if (item.limit > 0) (item.used * 100 / item.limit).toInt() else 0
    val remain = item.limit - item.used
    val barCol = when {
        pct >= 81 -> BarDanger
        pct >= 61 -> BarWarning
        else -> BarSafe
    }

    Column(Modifier.fillMaxWidth()) {
        // 상단: 아이콘 + 이름 + 퍼센트
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(IconBg),
                    Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.name,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
                    Spacer(Modifier.height(2.dp))
                    Text(item.sub, fontSize = 12.sp, color = Neutral400)
                }
            }
            Text("$pct%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = barCol)
        }

        // 바
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0))
        ) {
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp)).background(barCol)
            )
        }

        // 사용/남은
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Neutral900)) { append("사용 ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Neutral900)) { append("${fmtMan(item.used)}만원") }
            }, fontSize = 13.sp)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Neutral500)) { append("남은 ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Primary600)) { append("${fmtMan(remain)}만원") }
            }, fontSize = 13.sp)
        }
    }
}

private fun fmt(v: Long): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(v)
private fun fmtMan(v: Long): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(v / 10_000)
