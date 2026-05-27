package com.ssafy.seveniTax.ui.classification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*

data class ClassifiedTransaction(
    val id: String,
    val merchantName: String,
    val amount: String,
    val category: String
)

@Composable
fun AutoClassificationScreen(
    navController: NavController,
    transactions: List<ClassifiedTransaction> = sampleClassifiedTransactions(),
    onConfirm: () -> Unit = {},
    onEditCategory: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        TaxHeader(title = "자동 분류 알림", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 페이지 타이틀
            Text(
                text = "새로운 결제가",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )
            Text(
                text = "자동 분류되었어요",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "경비가 맞는지 확인해보세요",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF898989)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 섹션 레이블
            Text(
                text = "최근 분류 내역",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF898989)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 거래 목록
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                transactions.forEach { transaction ->
                    ClassifiedTransactionRow(
                        transaction = transaction,
                        onEdit = { onEditCategory(transaction.id) }
                    )
                }
            }
        }

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
private fun ClassifiedTransactionRow(
    transaction: ClassifiedTransaction,
    onEdit: () -> Unit
) {
    Column {
        // 가맹점명 + 금액
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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

        // 경비 + 수정 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = transaction.category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LogoPurple
            )
            Text(
                text = "수정 >",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LogoPurple,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = Surface, thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun sampleClassifiedTransactions() = listOf(
    ClassifiedTransaction(
        id = "1",
        merchantName = "교보문고",
        amount = "32,000원",
        category = "도서인쇄비"
    ),
    ClassifiedTransaction(
        id = "2",
        merchantName = "스타벅스",
        amount = "5,500원",
        category = "복리후생비"
    ),
    ClassifiedTransaction(
        id = "3",
        merchantName = "카카오T",
        amount = "18,400원",
        category = "여비교통비"
    )
)

@Preview(showBackground = true)
@Composable
private fun AutoClassificationScreenPreview() {
    AutoClassificationScreen(
        navController = rememberNavController()
    )
}
