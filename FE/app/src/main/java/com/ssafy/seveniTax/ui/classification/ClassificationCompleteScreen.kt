package com.ssafy.seveniTax.ui.classification

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.ssafy.seveniTax.viewmodel.ClassificationViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun ClassificationCompleteScreen(
    navController: NavController,
    classificationViewModel: ClassificationViewModel? = null,
    merchantName: String = "",
    amount: String = "",
    category: String = "",
    memo: String = "",
    onConfirm: () -> Unit = {}
) {
    val uiState = classificationViewModel?.uiState?.collectAsState()?.value
    val result = uiState?.result
    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)

    val displayMerchant = uiState?.merchantName?.ifEmpty { null } ?: merchantName.ifEmpty { "가맹점" }
    val displayAmount = if (uiState != null && uiState.amount > 0) "${fmt.format(uiState.amount)}원" else amount.ifEmpty { "" }
    val displayCategory = uiState?.selectedCategory ?: result?.taxCategory ?: category.ifEmpty { "경비" }
    val displayMemo = result?.remark?.ifEmpty { null } ?: memo.ifEmpty { "" }

    android.util.Log.d("CompleteScreen", "▶ merchant=$displayMerchant | amount=$displayAmount | category=$displayCategory")
    android.util.Log.d("CompleteScreen", "  uiState: entryId=${uiState?.entryId} amount=${uiState?.amount} selectedCategory=${uiState?.selectedCategory}")
    android.util.Log.d("CompleteScreen", "  result: taxCategory=${result?.taxCategory} remark=${result?.remark}")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.8f))

        // 체크 아이콘
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = LogoPurple,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val strokeWidth = 3.5.dp.toPx()
                // 체크마크 그리기
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.15f, size.height * 0.52f),
                    end = Offset(size.width * 0.4f, size.height * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.4f, size.height * 0.76f),
                    end = Offset(size.width * 0.85f, size.height * 0.28f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 타이틀
        Text(
            text = "경비가 확정되었어요",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF343434)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 부제
        Text(
            text = "AI 학습에 반영되어 다음에 더 정확해져요",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF898989)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // 요약 카드
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(15.dp),
                    ambientColor = Color(0x12362EB7),
                    spotColor = Color(0x12362EB7)
                )
                .background(
                    color = Background,
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(24.dp)
        ) {
            SummaryRow(label = "가맹점", value = displayMerchant)
            SummaryDivider()
            SummaryRow(label = "금액", value = displayAmount)
            SummaryDivider()
            SummaryRow(
                label = "경비",
                value = displayCategory,
                valueColor = BrandPurple,
                valueFontWeight = FontWeight.SemiBold
            )
            if (displayMemo.isNotEmpty()) {
                SummaryDivider()
                SummaryRow(label = "증빙 자료", value = displayMemo)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI 학습 뱃지
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 체크 원 아이콘
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawCircle(
                        color = Color(0xFFA8A3D7),
                        radius = size.minDimension / 2,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                    // 작은 체크
                    drawLine(
                        color = Color(0xFFA8A3D7),
                        start = Offset(size.width * 0.25f, size.height * 0.52f),
                        end = Offset(size.width * 0.42f, size.height * 0.68f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFA8A3D7),
                        start = Offset(size.width * 0.42f, size.height * 0.68f),
                        end = Offset(size.width * 0.75f, size.height * 0.35f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "AI 학습 데이터에 반영 완료",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA8A3D7)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 하단 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = onConfirm,
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
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF343434),
    valueFontWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
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
            fontWeight = valueFontWeight,
            color = valueColor
        )
    }
}

@Composable
private fun SummaryDivider() {
    HorizontalDivider(
        color = Surface,
        thickness = 1.dp
    )
}

@Preview(showBackground = true)
@Composable
private fun ClassificationCompleteScreenPreview() {
    ClassificationCompleteScreen(
        navController = rememberNavController()
    )
}
