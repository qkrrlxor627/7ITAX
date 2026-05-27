package com.ssafy.seveniTax.ui.classification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*

data class UnclassifiedTransaction(
    val id: String,
    val merchantName: String,
    val amount: String,
    val dateTime: String,
    val aiCategory: String
)

@Composable
fun UnclassifiedListScreen(
    navController: NavController,
    transactions: List<UnclassifiedTransaction> = emptyList(),
    showFirstVisitToast: Boolean = false,
    aiRecommendedInitial: Boolean = false,
    onBulkConfirm: () -> Unit = {},
    onAiRecommend: () -> Unit = {},
    onReviewAll: () -> Unit = {},
    onTransactionClick: (String) -> Unit = {}
) {
    var showToast by remember { mutableStateOf(showFirstVisitToast) }
    var aiRecommended by rememberSaveable { mutableStateOf(aiRecommendedInitial) }
    val totalAmount = "총 27,100원" // 실제로는 계산

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Background)
        ) {
            TaxHeader(title = "미분류 경비 내역", onBack = { navController.popBackStack() })

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 28.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // 페이지 타이틀
                Text(
                    text = "아직 확인하지 않은",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF343434)
                )
                Text(
                    text = "결제가 있어요",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF343434)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "경비를 확인하면 절세에 도움이 돼요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF898989)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 건수 뱃지 + 총액
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = LogoPurple,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${transactions.size}건",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = totalAmount,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF898989)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 거래 카드 목록
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    transactions.forEach { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            showAiCategory = aiRecommended,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 푸터 안내
                    Text(
                        text = "미확인 건은 최대 3회 알림 후 미분류로 보관됩니다",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFCACACA),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 하단 버튼
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!aiRecommended) {
                    // AI 추천 전: 추천 받기 버튼만
                    Button(
                        onClick = { aiRecommended = true; onAiRecommend() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "AI로 경비 추천 받기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    // AI 추천 후: 일괄 확정 버튼
                    Button(
                        onClick = onBulkConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "AI 추천대로 일괄 확정",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 첫 진입 토스트 오버레이
        if (showToast) {
            FirstVisitToastOverlay(
                onDismiss = { showToast = false }
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: UnclassifiedTransaction,
    showAiCategory: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.merchantName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF343434)
                )
                Text(
                    text = transaction.amount,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF343434)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = transaction.dateTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCACACA)
            )

            if (showAiCategory) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "AI 추천: ${transaction.aiCategory}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LogoPurple
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "상세보기",
            tint = Color(0xFFCACACA),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FirstVisitToastOverlay(
    onDismiss: () -> Unit
) {
    // 딤 오버레이
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // 토스트 카드
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .background(
                    color = Color(0xFF343434),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(20.dp)
        ) {
            // 아이콘 + 타이틀
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 인포 아이콘
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            width = 1.5.dp,
                            color = LogoPurple,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LogoPurple
                    )
                }

                Text(
                    text = "미분류 내역 안내",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 본문
            Text(
                text = "결제 후 AI가 자동으로 경비를 추천해요.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCACACA)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    append("미확인 건은 ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    ) {
                        append("최대 3회 알림")
                    }
                    append(" 후")
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCACACA)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "미분류로 보관됩니다.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCACACA)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 확인 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "확인했어요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA8A3D7),
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}

private fun sampleTransactions() = listOf(
    UnclassifiedTransaction(
        id = "1",
        merchantName = "스타벅스 강남점",
        amount = "5,500원",
        dateTime = "03.11 14:23",
        aiCategory = "복리후생비"
    ),
    UnclassifiedTransaction(
        id = "2",
        merchantName = "GS25 역삼점",
        amount = "3,200원",
        dateTime = "03.11 12:05",
        aiCategory = "소모품비"
    ),
    UnclassifiedTransaction(
        id = "3",
        merchantName = "카카오T 택시",
        amount = "18,400원",
        dateTime = "03.10 22:31",
        aiCategory = "여비교통비"
    )
)

@Preview(showBackground = true)
@Composable
private fun UnclassifiedListScreenPreview() {
    UnclassifiedListScreen(
        navController = rememberNavController()
    )
}

@Preview(showBackground = true)
@Composable
private fun UnclassifiedListScreenWithToastPreview() {
    UnclassifiedListScreen(
        navController = rememberNavController(),
        showFirstVisitToast = true
    )
}
