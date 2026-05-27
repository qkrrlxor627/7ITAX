package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.data.model.book.BookEntryResponse
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.BookEntryDetailViewModel
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

@Composable
fun BookEntryDetailScreen(
    navController: NavController,
    entryId: Long,
    viewModel: BookEntryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

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
            Spacer(modifier = Modifier.weight(1f))
            Text("거래 상세", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (entry != null) {
            DetailContent(entry!!, navController)
        }
    }
}

@Composable
private fun DetailContent(entry: BookEntryResponse, navController: NavController) {
    val amount = when (entry.entryType) {
        "INCOME" -> entry.incomeAmount
        "EXPENSE" -> entry.expenseAmount
        "ASSET" -> entry.fixedAssetAmount
        else -> 0L
    }
    val isExpense = entry.entryType != "INCOME"
    val amountPrefix = if (isExpense) "-" else "+"
    val formatted = NumberFormat.getNumberInstance(Locale.KOREA).format(amount)

    val deadlineDate = try {
        LocalDate.parse(entry.entryDate)
    } catch (e: Exception) { null }

    val dayOfWeek = deadlineDate?.dayOfWeek?.let {
        when (it) {
            DayOfWeek.MONDAY -> "월"; DayOfWeek.TUESDAY -> "화"
            DayOfWeek.WEDNESDAY -> "수"; DayOfWeek.THURSDAY -> "목"
            DayOfWeek.FRIDAY -> "금"; DayOfWeek.SATURDAY -> "토"
            DayOfWeek.SUNDAY -> "일"
        }
    } ?: ""
    val dateFormatted = deadlineDate?.let {
        "${it.year}.%02d.%02d (%s)".format(it.monthValue, it.dayOfMonth, dayOfWeek)
    } ?: entry.entryDate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 가맹점명
                Text(
                    text = entry.merchantName ?: entry.description ?: "거래",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 금액
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$amountPrefix$formatted",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "원", fontSize = 16.sp, color = TextSecondary,
                        modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 태그
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.categoryName != null) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .background(BrandPurple, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(entry.categoryName!!, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .border(BorderStroke(1.dp, Disabled), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("카드전표", fontSize = 13.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = Surface)

                // 정보 테이블
                DetailInfoRow("거래일", dateFormatted)
                HorizontalDivider(color = Surface)
                DetailInfoRow("분류", entry.categoryName ?: "미분류")
                HorizontalDivider(color = Surface)
                DetailInfoRow("증빙", if (entry.note.isNullOrBlank()) "없음" else "있음")
                HorizontalDivider(color = Surface)
                DetailInfoRow("결제수단", if (entry.paymentId != null) "카드결제" else "직접입력")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 버튼들
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 분류 변경
            OutlinedButton(
                onClick = { navController.navigate(Route.CategorySelect.create("book", entry.id)) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Disabled)
            ) {
                Text("분류 변경", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            // 증빙 내역 추가/수정
            OutlinedButton(
                onClick = { navController.navigate(Route.BookMemoAdd.create(entryId = entry.id)) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Disabled)
            ) {
                Text(
                    if (entry.note.isNullOrBlank()) "증빙 내역 추가" else "증빙 내역 수정",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                )
            }

            // 확인
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("확인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
