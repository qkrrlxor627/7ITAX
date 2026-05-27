package com.ssafy.seveniTax.ui.classification

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*
import kotlinx.coroutines.delay

private data class BulkItem(
    val merchantName: String,
    val amount: String,
    val aiCategory: String
)

private val sampleBulkItems = listOf(
    BulkItem("스타벅스 강남점", "5,500원", "복리후생비"),
    BulkItem("GS25 역삼점", "3,200원", "소모품비"),
    BulkItem("카카오T 택시", "18,400원", "여비교통비")
)

@Composable
fun BulkClassificationLoadingScreen(
    navController: NavController,
    onComplete: () -> Unit = {}
) {
    val items = sampleBulkItems
    var completedCount by remember { mutableIntStateOf(0) }

    // 항목별 순차 분석 시뮬레이션
    LaunchedEffect(Unit) {
        items.forEachIndexed { index, _ ->
            delay(1500L)
            completedCount = index + 1
        }
        delay(500L)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        TaxHeader(
            title = "AI 일괄 분석",
            onBack = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 로딩 애니메이션
            BulkLoadingCircle()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AI가 경비를 분석하고 있어요",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${completedCount}/${items.size}건 분석 완료",
                fontSize = 14.sp,
                color = LogoPurple,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 항목별 실시간 진행 상태
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "분석 진행 현황",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                items.forEachIndexed { index, item ->
                    val isDone = index < completedCount
                    val isProcessing = index == completedCount

                    BulkAnalysisRow(
                        merchantName = item.merchantName,
                        amount = item.amount,
                        aiCategory = item.aiCategory,
                        isDone = isDone,
                        isProcessing = isProcessing
                    )

                    if (index < items.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE8E8E8))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkLoadingCircle() {
    val infiniteTransition = rememberInfiniteTransition(label = "bulkLoading")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = Modifier.size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(70.dp)) {
            drawCircle(
                color = Color(0xFFD9D9D9),
                radius = size.minDimension / 2,
                style = Stroke(width = 5.dp.toPx())
            )
            drawArc(
                color = Color(0xFF281C9D),
                startAngle = -90f,
                sweepAngle = sweepAngle * 0.7f,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun BulkAnalysisRow(
    merchantName: String,
    amount: String,
    aiCategory: String,
    isDone: Boolean,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = merchantName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) Color(0xFF343434) else Color(0xFFCACACA)
                )
                Text(
                    text = amount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) Color(0xFF343434) else Color(0xFFCACACA)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isDone -> "✓ AI 추천: $aiCategory"
                    isProcessing -> "분석 중..."
                    else -> "대기 중"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isDone -> LogoPurple
                    isProcessing -> BrandPurple
                    else -> Color(0xFFCACACA)
                }
            )
        }
    }
}
