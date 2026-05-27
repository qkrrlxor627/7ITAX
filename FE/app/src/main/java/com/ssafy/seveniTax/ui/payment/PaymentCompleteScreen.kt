package com.ssafy.seveniTax.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.ButtonVariant
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PaymentCompleteScreen(
    navController: NavController,
    paymentViewModel: PaymentViewModel
) {
    val paymentState by paymentViewModel.uiState.collectAsState()
    val fmt = NumberFormat.getNumberInstance()
    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        // ── 상단 영역 ──
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 체크 아이콘
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(LogoPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "완료",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "결제 완료",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = now,
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── 결제 정보 카드 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                InfoRow("결제 금액", "${fmt.format(paymentState.amount)}원")
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 16.dp))
                InfoRow("가맹점", paymentState.payerName.ifEmpty { "-" })
            }
        }

        // ── 하단 버튼 ──
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaxButton(
                text = "증빙 내역 추가",
                onClick = {
                    // paymentId로 연결된 장부 내역 상세로 이동
                    val paymentId = paymentState.paymentId
                    if (paymentId != null) {
                        navController.navigate(Route.BookEntryDetail.create(paymentId)) {
                            popUpTo(Route.Main.path) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Route.BookEntryList.path) {
                            popUpTo(Route.Main.path) { inclusive = false }
                        }
                    }
                    paymentViewModel.resetPayment()
                }
            )
            TaxButton(
                text = "홈으로",
                onClick = {
                    paymentViewModel.resetPayment()
                    navController.navigate(Route.Main.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                variant = ButtonVariant.Secondary
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
