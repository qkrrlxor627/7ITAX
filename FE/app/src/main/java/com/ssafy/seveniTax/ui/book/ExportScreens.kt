package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.ExportViewModel

// ═══════════════════════════════════════════════════════
// Step 1: 목적 선택
// ═══════════════════════════════════════════════════════

private data class ExportPurposeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val enabled: Boolean = false
)

private val purposes = listOf(
    ExportPurposeItem("vat1", "부가가치세 1기", "1월 ~ 6월", "📋", enabled = true),
    ExportPurposeItem("vat2", "부가가치세 2기", "7월 ~ 12월", "📋", enabled = true),
    ExportPurposeItem("income", "종합소득세", "1월 ~ 12월", "📊", enabled = true),
    ExportPurposeItem("local", "지방소득세", "1월 ~ 12월", "📊", enabled = true),
    ExportPurposeItem("custom", "직접 설정", "원하는 기간을 선택합니다", "⚙️", enabled = true)
)

@Composable
fun ExportPurposeScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        ExportHeader(title = "데이터 내보내기", onClose = { navController.popBackStack() }, useClose = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "어떤 용도로 내보내시나요?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "용도에 맞는 기간이 자동으로 설정됩니다",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            purposes.forEach { purpose ->
                PurposeCard(purpose) {
                    if (!purpose.enabled) {
                        Toast.makeText(context, "준비중입니다", Toast.LENGTH_SHORT).show()
                        return@PurposeCard
                    }
                    if (purpose.id == "custom") {
                        navController.navigate(Route.ExportDateRange.create(purpose.id))
                    } else {
                        val (start, end) = getDefaultDates(purpose.id)
                        navController.navigate(Route.ExportFormat.create(purpose.id, start, end))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PurposeCard(item: ExportPurposeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.enabled) Surface else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.enabled) TextPrimary else TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            if (!item.enabled) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("준비중", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Step 2: 기간 설정
// ═══════════════════════════════════════════════════════

@Composable
fun ExportDateRangeScreen(navController: NavController, purpose: String) {
    val (defaultStart, defaultEnd) = getDefaultDates(purpose)
    var startDate by remember { mutableStateOf(defaultStart) }
    var endDate by remember { mutableStateOf(defaultEnd) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { startDate = it; showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { endDate = it; showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ExportHeader(title = "기간 설정", onClose = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("내보낼 기간을 선택하세요", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("시작일과 종료일을 설정해주세요", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            // 시작일
            Text("시작일", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .clickable { showStartPicker = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(startDate, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 종료일
            Text("종료일", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .clickable { showEndPicker = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(endDate, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }

        // 다음 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    navController.navigate(Route.ExportFormat.create(purpose, startDate, endDate))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("다음", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialDate.split(".")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: java.time.LocalDate.now().year
    val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
    val initialMillis = java.time.LocalDate.of(year, month, day)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected("${date.year}.%02d.%02d".format(date.monthValue, date.dayOfMonth))
                }
            }) {
                Text("확인", color = BrandPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    ) {
        DatePicker(state = state)
    }
}

private fun getDefaultDates(purpose: String): Pair<String, String> {
    val year = java.time.LocalDate.now().year.toString()
    return when (purpose) {
        "vat1" -> "$year.01.01" to "$year.06.30"
        "vat2" -> "$year.07.01" to "$year.12.31"
        "income", "local" -> "$year.01.01" to "$year.12.31"
        else -> "$year.01.01" to "$year.03.31"
    }
}

// ═══════════════════════════════════════════════════════
// Step 3: 파일 형식 선택
// ═══════════════════════════════════════════════════════

private data class FormatItem(
    val id: String,
    val title: String,
    val extension: String,
    val description: String,
    val icon: String,
    val enabled: Boolean = false
)

private val formats = listOf(
    FormatItem("pdf", "PDF", ".pdf", "PDF 다운로드", "📄", enabled = true),
    FormatItem("excel", "Excel", ".xlsx", "Excel 파일 다운로드", "📊", enabled = true)
)

@Composable
fun ExportFormatScreen(
    navController: NavController,
    purpose: String,
    startDate: String,
    endDate: String,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val purposeLabel = purposes.find { it.id == purpose }?.title ?: "직접 설정"
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 연도 추출
    val year = try { startDate.take(4).toInt() } catch (_: Exception) { 2025 }

    // 다운로드 결과 처리
    LaunchedEffect(uiState.successFileName) {
        uiState.successFileName?.let { fileName ->
            Toast.makeText(context, "다운로드 완료: $fileName", Toast.LENGTH_SHORT).show()
            viewModel.clearState()
            navController.popBackStack(Route.BookEntryList.path, false)
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ExportHeader(title = "파일 형식 선택", onClose = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 컨텍스트 배너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(purposeLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandPurple)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$startDate ~ $endDate", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("파일 형식을 선택하세요", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (uiState.isExporting) "다운로드 중..." else "선택하면 다운로드 폴더에 저장됩니다",
                fontSize = 13.sp,
                color = if (uiState.isExporting) BrandPurple else TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            formats.forEach { format ->
                FormatCard(format) {
                    if (!uiState.isExporting) {
                        when (format.id) {
                            "pdf" -> when (purpose) {
                                "vat1" -> viewModel.exportVat(year, 1)
                                "vat2" -> viewModel.exportVat(year, 2)
                                "income" -> viewModel.exportIncomeTax(year)
                                "local" -> viewModel.exportLocalTax(year)
                                else -> viewModel.exportSimpleLedgerPdf(year)
                            }
                            "excel" -> when (purpose) {
                                "vat1" -> viewModel.exportVatExcel(year, 1)
                                "vat2" -> viewModel.exportVatExcel(year, 2)
                                "income" -> viewModel.exportIncomeTaxExcel(year)
                                "local" -> viewModel.exportLocalTax(year)
                                else -> viewModel.exportIncomeTaxExcel(year)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FormatCard(item: FormatItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.enabled) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.enabled) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                Text(item.icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.enabled) TextPrimary else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.extension, fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.description, fontSize = 12.sp, color = TextSecondary)
            }
            if (!item.enabled) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("준비중", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 공용 헤더
// ═══════════════════════════════════════════════════════

@Composable
private fun ExportHeader(title: String, onClose: () -> Unit, useClose: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = if (useClose) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "닫기",
                tint = TextPrimary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(48.dp))
    }
}
