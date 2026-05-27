package com.ssafy.seveniTax.ui.classification

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ssafy.seveniTax.viewmodel.ClassificationViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun ClassificationLoadingScreen(
    navController: NavController,
    classificationViewModel: ClassificationViewModel,
    merchantName: String = "",
    amount: Long = 0L,
    industryCode: String = "",
    onComplete: () -> Unit = {}
) {
    val uiState by classificationViewModel.uiState.collectAsState()

    // 실제 AI API 호출
    LaunchedEffect(Unit) {
        val name = merchantName.ifEmpty { uiState.merchantName }
        val amt = if (amount > 0) amount else uiState.amount
        if (name.isNotEmpty()) {
            classificationViewModel.classify(name, if (amt > 0) amt else null, uiState.note)
        } else {
            delay(1000L)
            onComplete()
        }
    }

    // 결과 또는 에러 시 다음 화면 이동
    LaunchedEffect(uiState.result, uiState.error) {
        if (uiState.result != null || uiState.error != null) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        TaxHeader(
            title = "경비 분류",
            onBack = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 로딩 애니메이션 (원형 프로그레스)
            LoadingCircle()

            Spacer(modifier = Modifier.height(32.dp))

            // 제목
            Text(
                text = "AI가 경비를 분석하고 있어요",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 부제
            Text(
                text = "가맹점 정보와 결제 내역을 확인 중입니다",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            // 분석 항목 카드
            AnalysisItemsCard(
                merchantName = merchantName,
                industryCode = industryCode
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoadingCircle() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
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
        modifier = Modifier.size(85.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(85.dp)) {
            // 배경 원
            drawCircle(
                color = Color(0xFFD9D9D9),
                radius = size.minDimension / 2,
                style = Stroke(width = 6.dp.toPx())
            )
            // 프로그레스 아크
            drawArc(
                color = Color(0xFF281C9D),
                startAngle = -90f,
                sweepAngle = sweepAngle * 0.7f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun AnalysisItemsCard(
    merchantName: String,
    industryCode: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(20.dp)
    ) {
        Text(
            text = "분석 항목",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 가맹점명 (완료)
        AnalysisItem(
            label = "가맹점명: $merchantName",
            isDone = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 업종코드 (완료)
        AnalysisItem(
            label = "업종코드: $industryCode",
            isDone = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 경비 매칭 (진행 중)
        AnalysisItem(
            label = "경비 매칭 중...",
            isDone = false
        )
    }
}

@Composable
private fun AnalysisItem(
    label: String,
    isDone: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDone) {
            // 체크 아이콘
            Canvas(modifier = Modifier.size(16.dp)) {
                drawCircle(color = Color(0xFF343434), radius = 2.dp.toPx())
                // 체크마크를 점으로 대체하지 않고, 심플하게 표현
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "✓  $label",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF343434)
            )
        } else {
            // 로딩 인디케이터 (빈 원)
            Canvas(modifier = Modifier.size(16.dp)) {
                drawCircle(
                    color = LogoPurple,
                    radius = 6.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LogoPurple
            )
        }
    }
}

// Preview removed: requires ClassificationViewModel