package com.ssafy.seveniTax.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.viewmodel.MainViewModel

// ── 디자인 토큰 (스타일 가이드 기준) ──
private val Primary600 = Color(0xFF5655B9)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral500 = Color(0xFF898989)
private val Neutral300 = Color(0xFFCACACA)
private val ErrorColor = Color(0xFFFF4267)

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val userName = mainViewModel.getUserName().ifBlank { "사용자" }
    val userPhone = mainViewModel.getUserPhone()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃", fontWeight = FontWeight.Bold, color = Neutral900) },
            text = { Text("정말 로그아웃 하시겠습니까?", fontSize = 14.sp, color = Neutral500) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    mainViewModel.logout()
                    navController.navigate(Route.Splash.path) { popUpTo(0) { inclusive = true } }
                }) {
                    Text("로그아웃", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Neutral500)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {

        // ── 헤더 (< 설정) — 카드관리/페이가입과 동일 패턴 ──
        // ── 프로필 영역 (헤더 겸용) ──
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
            Text("설정", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary600, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(12.dp))
            Text(userName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
            if (userPhone.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(userPhone, fontSize = 14.sp, color = Neutral500)
            }
        }

        HorizontalDivider(color = Primary50)

        // ── 메뉴 리스트 ──
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            // 세금·장부
            SettingsGroupHeader("세금·장부")
            SettingsRow("알림 설정") { navController.navigate(Route.NotificationSettings.path) }
            SettingsRow("세금 캘린더") { navController.navigate(Route.TaxCalendar.path) }
            SettingsRow("공제 한도") { navController.navigate(Route.TaxSavingsDetail.path) }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Primary50)

            // 결제·카드
            SettingsGroupHeader("결제·카드")
            SettingsRow("카드 관리") { navController.navigate(Route.CardList.path) }
            SettingsRow("QR 결제") { navController.navigate(Route.QrPayment.path) }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Primary50)

            // 앱 정보
            SettingsGroupHeader("앱 정보")
            SettingsInfoRow("버전", "1.0.0")
            SettingsInfoRow("빌드", "2026.03")

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Primary50)

            // 로그아웃
            Box(
                Modifier.fillMaxWidth().clickable { showLogoutDialog = true }.padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("로그아웃", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ErrorColor)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── 그룹 헤더 (스타일 가이드: #5655B9 SemiBold 13px) ──
@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary600,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
    )
}

// ── 메뉴 행 (좌: 타이틀, 우: 셰브론 >) ──
@Composable
private fun SettingsRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Neutral900, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Neutral300, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = Primary50, modifier = Modifier.padding(horizontal = 20.dp))
}

// ── 정보 행 (좌: 라벨, 우: 값) ──
@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Neutral900)
        Text(value, fontSize = 14.sp, color = Neutral500)
    }
    HorizontalDivider(color = Primary50, modifier = Modifier.padding(horizontal = 20.dp))
}
