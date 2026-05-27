package com.ssafy.seveniTax.ui.classification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun ClassificationResultScreen(
    navController: NavController,
    recommendedCategory: String = "복리후생비",
    confidence: Int = 97,
    confidenceReason: String = "카페/음료 업종 기반",
    merchantName: String = "스타벅스 강남점",
    amount: String = "5,500원",
    dateTime: String = "2026.03.11 14:23",
    paymentMethod: String = "7ITAX 체크카드",
    alternativeCategories: List<Pair<String, Int>> = listOf("접대비" to 8, "여비교통비" to 3),
    onConfirm: () -> Unit = {},
    onChangeCategory: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        TaxHeader(
            title = "경비 확인",
            onBack = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 메인 타이틀
            Text(
                text = "AI가 경비를",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )
            Text(
                text = "추천했어요",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // AI 추천 카드
            AiRecommendationCard(
                category = recommendedCategory,
                confidence = confidence,
                reason = confidenceReason
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 결제 정보 섹션
            Text(
                text = "결제 정보",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF898989)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PaymentInfoRow(label = "가맹점", value = merchantName)
            Spacer(modifier = Modifier.height(14.dp))
            PaymentInfoRow(label = "결제금액", value = amount)
            Spacer(modifier = Modifier.height(14.dp))
            PaymentInfoRow(label = "결제일시", value = dateTime)
            Spacer(modifier = Modifier.height(14.dp))
            PaymentInfoRow(label = "결제수단", value = paymentMethod)

            Spacer(modifier = Modifier.height(28.dp))

            // 다른 추천 경비 섹션
            if (alternativeCategories.isNotEmpty()) {
                Text(
                    text = "다른 추천 경비",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF898989)
                )

                Spacer(modifier = Modifier.height(16.dp))

                alternativeCategories.forEach { (category, percent) ->
                    AlternativeCategoryRow(
                        category = category,
                        percent = percent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 하단 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // 경비 변경 버튼 (Secondary)
            androidx.compose.material3.OutlinedButton(
                onClick = onChangeCategory,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF343434)
                )
            ) {
                Text(
                    text = "경비 변경",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 확인 버튼 (Primary)
            androidx.compose.material3.Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = BrandPurple,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "확인",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AiRecommendationCard(
    category: String,
    confidence: Int,
    reason: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Surface,
                shape = RoundedCornerShape(15.dp)
            )
            .padding(16.dp)
    ) {
        // AI 추천 뱃지
        Box(
            modifier = Modifier
                .background(
                    color = LogoPurple,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AI 추천",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 경비명
        Text(
            text = category,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF343434)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 프로그레스 바
        LinearProgressIndicator(
            progress = { confidence / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = BrandPurple,
            trackColor = Color(0xFFE0E0E0),
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 신뢰도 텍스트
        Text(
            text = "신뢰도 ${confidence}% · $reason",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF898989)
        )
    }
}

@Composable
private fun PaymentInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF898989)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF343434)
        )
    }
}

@Composable
private fun AlternativeCategoryRow(
    category: String,
    percent: Int
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF343434)
            )
            Text(
                text = "${percent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF898989)
            )
        }
        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
    }
}

@Preview(showBackground = true)
@Composable
private fun ClassificationResultScreenPreview() {
    ClassificationResultScreen(
        navController = rememberNavController()
    )
}
