package com.ssafy.seveniTax.ui.calendar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── 체크리스트 아이템 모델 ─────────────────────────────

private data class ChecklistItem(
    val title: String,
    val subtitle: String,
    val isCompleted: Boolean,
    val hasNavigation: Boolean = true
)

// ─── 세금 상세 정보 ─────────────────────────────────────

private data class TaxDetailInfo(
    val taxType: String,
    val reportType: String,
    val reportPeriod: String,
    val deadlineFormatted: String,
    val checklist: List<ChecklistItem>
)

private fun buildTaxDetailInfo(taxName: String, deadline: String, description: String): TaxDetailInfo {
    val deadlineDate = try {
        LocalDate.parse(deadline)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val dayOfWeek = when (deadlineDate.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

    val deadlineFormatted = "${deadlineDate.year}.%02d.%02d (%s)".format(
        deadlineDate.monthValue, deadlineDate.dayOfMonth, dayOfWeek
    )

    return when {
        taxName.contains("중간예납") -> TaxDetailInfo(
            taxType = "종합소득세",
            reportType = "중간예납 납부",
            reportPeriod = "",
            deadlineFormatted = deadlineFormatted,
            checklist = listOf(
                ChecklistItem("고지서 수령 확인", "세무서에서 발송한 고지서 확인", false),
                ChecklistItem("중간예납세액 확인", "직전 연도 소득세의 50%", false),
                ChecklistItem("납부 완료", "홈택스 또는 은행에서 납부", false)
            )
        )
        taxName.contains("종합소득세") -> TaxDetailInfo(
            taxType = "종합소득세",
            reportType = "확정신고",
            reportPeriod = "${deadlineDate.year}.05.01 ~ ${deadlineDate.year}.05.31",
            deadlineFormatted = deadlineFormatted,
            checklist = listOf(
                ChecklistItem("간편장부 정리", "5/3 완료", true),
                ChecklistItem("경비 증빙 확인", "5/8 완료", true),
                ChecklistItem("소득금액 계산", "AI가 자동 계산해드려요", false),
                ChecklistItem("신고서 작성", "홈택스에서 제출", false)
            )
        )
        taxName.contains("부가") && taxName.contains("예정") -> TaxDetailInfo(
            taxType = "부가가치세",
            reportType = "예정고지 납부",
            reportPeriod = "",
            deadlineFormatted = deadlineFormatted,
            checklist = listOf(
                ChecklistItem("고지서 수령 확인", "세무서에서 발송한 고지서 확인", false),
                ChecklistItem("고지 금액 확인", "직전 반기 납부세액의 50%", false),
                ChecklistItem("납부 완료", "홈택스 또는 은행에서 납부", false)
            )
        )
        taxName.contains("부가") -> TaxDetailInfo(
            taxType = "부가가치세",
            reportType = description.ifEmpty { "확정신고" },
            reportPeriod = if (taxName.contains("1기")) "${deadlineDate.year}.01.01 ~ ${deadlineDate.year}.06.30"
                          else "${deadlineDate.year}.07.01 ~ ${deadlineDate.year}.12.31",
            deadlineFormatted = deadlineFormatted,
            checklist = listOf(
                ChecklistItem("매출 내역 정리", "카드 결제 내역 확인", false),
                ChecklistItem("매입 증빙 수집", "세금계산서 확인", false),
                ChecklistItem("부가세 계산", "AI가 자동 계산해드려요", false),
                ChecklistItem("신고서 작성", "홈택스에서 제출", false)
            )
        )
        taxName.contains("지방") -> TaxDetailInfo(
            taxType = "지방소득세",
            reportType = description.ifEmpty { "확정신고" },
            reportPeriod = "${deadlineDate.year}.05.01 ~ ${deadlineDate.year}.05.31",
            deadlineFormatted = deadlineFormatted,
            checklist = listOf(
                ChecklistItem("종합소득세 신고 완료", "종합소득세 먼저 신고 필요", false),
                ChecklistItem("지방소득세 계산", "종합소득세의 10%", false),
                ChecklistItem("신고서 작성", "위택스에서 제출", false)
            )
        )
        else -> TaxDetailInfo(
            taxType = taxName,
            reportType = description,
            reportPeriod = "",
            deadlineFormatted = deadlineFormatted,
            checklist = emptyList()
        )
    }
}

// ─── 메인 화면 ──────────────────────────────────────────

@Composable
fun TaxCalendarDetailScreen(
    navController: NavController,
    taxName: String,
    deadline: String,
    dDay: Int,
    description: String,
    viewModel: TaxCalendarViewModel
) {
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val d7 by viewModel.reminderD7.collectAsState()
    val d3 by viewModel.reminderD3.collectAsState()
    val d1 by viewModel.reminderD1.collectAsState()
    val dDayReminder by viewModel.reminderDDay.collectAsState()
    val detailInfo = remember(taxName, deadline, description) {
        buildTaxDetailInfo(taxName, deadline, description)
    }

    // 체크리스트 상태 관리
    var checkStates by remember {
        mutableStateOf(detailInfo.checklist.map { it.isCompleted })
    }
    val completedCount = checkStates.count { it }
    val totalCount = detailInfo.checklist.size
    val allCompleted = totalCount > 0 && completedCount == totalCount

    // D-day 색상
    val ddayColor = when {
        dDay <= 3 -> DdayError
        dDay <= 7 -> DdayWarning
        else -> DdayNormal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        DetailHeader(onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 상단 요약
            DetailTopSection(
                taxName = taxName,
                deadlineFormatted = detailInfo.deadlineFormatted,
                dDay = dDay,
                ddayColor = ddayColor
            )

            // 정보 카드
            InfoCard(detailInfo)

            Spacer(modifier = Modifier.height(16.dp))

            // 리마인드 알림
            ReminderCard(
                enabled = reminderEnabled,
                onToggle = { viewModel.setReminderEnabled(it) },
                d7 = d7,
                d3 = d3,
                d1 = d1,
                dDay = dDayReminder,
                onChangeReminder = { navController.navigate(Route.NotificationSettings.path) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 준비 체크리스트
            if (detailInfo.checklist.isNotEmpty()) {
                ChecklistCard(
                    items = detailInfo.checklist,
                    checkStates = checkStates,
                    onToggle = { index ->
                        checkStates = checkStates.toMutableList().also {
                            it[index] = !it[index]
                        }
                    },
                    completedCount = completedCount,
                    totalCount = totalCount,
                    allCompleted = allCompleted
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 하단 버튼들
            BottomButtons(allCompleted = allCompleted)

            Spacer(modifier = Modifier
                .navigationBarsPadding()
                .height(24.dp))
        }
    }
}

// ─── 헤더 ───────────────────────────────────────────────

@Composable
private fun DetailHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "일정 상세",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.weight(1f))

        // 오른쪽 여백 (뒤로가기 아이콘 너비만큼)
        Spacer(modifier = Modifier.size(48.dp))
    }
}

// ─── 상단 요약 ──────────────────────────────────────────

@Composable
private fun DetailTopSection(
    taxName: String,
    deadlineFormatted: String,
    dDay: Int,
    ddayColor: Color = DdayNormal
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 아이콘
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = taxName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$deadlineFormatted 마감",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // D-day 배지
        Box(
            modifier = Modifier
                .background(ddayColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (dDay == 0) "D-Day" else "D-$dDay",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─── 정보 카드 ──────────────────────────────────────────

@Composable
private fun InfoCard(info: TaxDetailInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            InfoRow("세금 종류", info.taxType)
            InfoDivider()
            InfoRow("신고 유형", info.reportType)
            if (info.reportPeriod.isNotEmpty()) {
                InfoDivider()
                InfoRow("신고 기간", info.reportPeriod)
            }
            InfoDivider()
            InfoRow("마감일", info.deadlineFormatted)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = Surface,
        thickness = 1.dp
    )
}

// ─── 체크리스트 카드 ────────────────────────────────────

@Composable
private fun ChecklistCard(
    items: List<ChecklistItem>,
    checkStates: List<Boolean>,
    onToggle: (Int) -> Unit,
    completedCount: Int,
    totalCount: Int,
    allCompleted: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(20.dp)) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "세금 신고 가이드",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$completedCount/$totalCount 완료",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Accent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 프로그레스 바
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Disabled.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Accent, BrandPurple)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 체크리스트 항목들
                items.forEachIndexed { index, item ->
                    val isChecked = checkStates.getOrElse(index) { item.isCompleted }
                    ChecklistItemRow(
                        item = item.copy(isCompleted = isChecked),
                        onToggle = { onToggle(index) }
                    )
                }
            }

            // 전부 완료 시 오버레이
            if (allCompleted) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5).copy(alpha = 0.93f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "준비 완료!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "홈택스에서 신고를 진행하세요",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(item: ChecklistItem, onToggle: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 체크박스
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (item.isCompleted) Accent else Color.White
                )
                .then(
                    if (!item.isCompleted)
                        Modifier.border(1.5.dp, Disabled, RoundedCornerShape(8.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 텍스트
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                color = if (item.isCompleted) TextSecondary else TextPrimary,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

    }

    HorizontalDivider(color = Surface, thickness = 1.dp)
}

// ─── 리마인드 알림 카드 ─────────────────────────────────

@Composable
private fun ReminderCard(
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit = {},
    d7: Boolean = true,
    d3: Boolean = true,
    d1: Boolean = true,
    dDay: Boolean = true,
    onChangeReminder: () -> Unit = {}
) {
    val enabledChips = if (enabled) {
        buildList {
            if (d7) add("D-7")
            if (d3) add("D-3")
            if (d1) add("D-1")
            if (dDay) add("당일")
        }
    } else emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 헤더 + 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "리마인드 알림",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Disabled
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // D-day 칩들 (활성화된 것만 표시)
            if (enabledChips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    enabledChips.forEach { label ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, Disabled, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (enabled) "설정된 알림이 없습니다" else "알림이 꺼져 있습니다",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 알림 시점 변경 (ON일 때만 표시)
            if (enabled) Row(
                modifier = Modifier.clickable { onChangeReminder() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "알림 시점 변경",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Accent
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── 하단 버튼들 ────────────────────────────────────────

@Composable
private fun BottomButtons(allCompleted: Boolean = false) {
    val context = LocalContext.current
    val hometaxUrl = "https://hometax.go.kr/websquare/websquare.html?w2xPath=/ui/pp/index_pp.xml&menuCd=index3"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 홈택스에서 신고하기 (체크리스트 완료 시 강조)
        if (allCompleted) {
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(hometaxUrl))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text(
                    text = "홈택스에서 신고하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        } else {
            OutlinedButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(hometaxUrl))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Disabled)
            ) {
                Text(
                    text = "홈택스에서 신고하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPurple
                )
            }
        }
    }
}
