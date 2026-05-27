package com.ssafy.seveniTax.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel

@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: TaxCalendarViewModel
) {
    var calendarAlarmEnabled by remember { mutableStateOf(viewModel.reminderEnabled.value) }

    // 리마인드 타이밍 (ViewModel과 연동)
    var d7 by remember { mutableStateOf(viewModel.reminderD7.value) }
    var d3 by remember { mutableStateOf(viewModel.reminderD3.value) }
    var d1 by remember { mutableStateOf(viewModel.reminderD1.value) }
    var dDay by remember { mutableStateOf(viewModel.reminderDDay.value) }

    // 알림 받을 세금
    var vatEnabled by remember { mutableStateOf(viewModel.vatAlarmEnabled.value) }
    var incomeEnabled by remember { mutableStateOf(viewModel.incomeAlarmEnabled.value) }
    var localEnabled by remember { mutableStateOf(viewModel.localAlarmEnabled.value) }

    // 알림 방식
    var pushEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        SettingsHeader(onClose = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 메인 토글
            MainToggle(
                enabled = calendarAlarmEnabled,
                onToggle = { calendarAlarmEnabled = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 리마인드 타이밍
            RemindTimingSection(
                d7 = d7, d3 = d3, d1 = d1, dDay = dDay,
                onD7 = { d7 = it }, onD3 = { d3 = it },
                onD1 = { d1 = it }, onDDay = { dDay = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 알림 받을 세금
            TaxTypeSection(
                vatEnabled = vatEnabled,
                incomeEnabled = incomeEnabled,
                localEnabled = localEnabled,
                onVat = { vatEnabled = it },
                onIncome = { incomeEnabled = it },
                onLocal = { localEnabled = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 알림 방식
            NotificationMethodSection(
                pushEnabled = pushEnabled,
                onPush = { pushEnabled = it }
            )
        }

        // 저장 버튼
        SaveButton(onClick = {
            viewModel.setReminderEnabled(calendarAlarmEnabled)
            viewModel.setReminderD7(d7)
            viewModel.setReminderD3(d3)
            viewModel.setReminderD1(d1)
            viewModel.setReminderDDay(dDay)
            viewModel.setVatAlarmEnabled(vatEnabled)
            viewModel.setIncomeAlarmEnabled(incomeEnabled)
            viewModel.setLocalAlarmEnabled(localEnabled)
            viewModel.applyReminderSettings()
            navController.popBackStack()
        })
    }
}

// ─── 헤더 ───────────────────────────────────────────────

@Composable
private fun SettingsHeader(onClose: () -> Unit) {
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
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "알림 설정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.size(48.dp))
    }

    HorizontalDivider(color = Surface, thickness = 1.dp)
}

// ─── 메인 토글 ──────────────────────────────────────────

@Composable
private fun MainToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "세금 캘린더 알림",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
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
}

// ─── 리마인드 타이밍 ────────────────────────────────────

@Composable
private fun RemindTimingSection(
    d7: Boolean, d3: Boolean, d1: Boolean, dDay: Boolean,
    onD7: (Boolean) -> Unit, onD3: (Boolean) -> Unit,
    onD1: (Boolean) -> Unit, onDDay: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "리마인드 타이밍",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "마감일 기준 며칠 전에 알려드릴까요?",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimingChip("D-7", d7) { onD7(!d7) }
            TimingChip("D-3", d3) { onD3(!d3) }
            TimingChip("D-1", d1) { onD1(!d1) }
            TimingChip("당일", dDay) { onDDay(!dDay) }
        }
    }
}

@Composable
private fun TimingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BrandPurple else Disabled.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

// ─── 알림 받을 세금 ─────────────────────────────────────

@Composable
private fun TaxTypeSection(
    vatEnabled: Boolean,
    incomeEnabled: Boolean,
    localEnabled: Boolean,
    onVat: (Boolean) -> Unit,
    onIncome: (Boolean) -> Unit,
    onLocal: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "알림 받을 세금",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        TaxToggleRow(
            title = "부가가치세",
            subtitle = "1·7월 확정신고 / 4·10월 예정고지",
            enabled = vatEnabled,
            onToggle = onVat
        )

        HorizontalDivider(color = Surface, thickness = 1.dp)

        TaxToggleRow(
            title = "종합소득세",
            subtitle = "5월 확정신고",
            enabled = incomeEnabled,
            onToggle = onIncome
        )

        HorizontalDivider(color = Surface, thickness = 1.dp)

        TaxToggleRow(
            title = "지방소득세",
            subtitle = "종합소득세와 동시 신고 (5월)",
            enabled = localEnabled,
            onToggle = onLocal
        )
    }
}

@Composable
private fun TaxToggleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
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
}

// ─── 알림 방식 ──────────────────────────────────────────

@Composable
private fun NotificationMethodSection(
    pushEnabled: Boolean,
    onPush: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "알림 방식",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 푸시 알림
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "푸시 알림",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Switch(
                checked = pushEnabled,
                onCheckedChange = onPush,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Disabled
                )
            )
        }

        HorizontalDivider(color = Surface, thickness = 1.dp)

        // 카카오톡 알림 (준비 중)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "카카오톡 알림",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "준비 중",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Accent
                    )
                }
            }
            Switch(
                checked = false,
                onCheckedChange = { },
                enabled = false,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = Color.White,
                    disabledCheckedTrackColor = Disabled,
                    disabledUncheckedThumbColor = Color.White,
                    disabledUncheckedTrackColor = Disabled
                )
            )
        }
    }
}

// ─── 저장 버튼 ──────────────────────────────────────────

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            Text(
                text = "저장",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
