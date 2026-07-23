package com.ssafy.seveniTax.ui.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.seveniTax.ui.components.CodeBoxes
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.PayViewModel

@Composable
fun PayVerifyScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    viewModel: PayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pin by remember { mutableStateOf("") }

    // 6자리 입력 완료 → 결제 비밀번호 저장 + Pay 계좌 개설
    LaunchedEffect(pin) {
        if (pin.length == 6 && !uiState.loading && !uiState.payComplete) {
            viewModel.completePaySignup(pin)
        }
    }

    // 가입 완료 → 완료 화면으로 이동
    LaunchedEffect(uiState.payComplete) {
        if (uiState.payComplete) onNext()
    }

    // 실패 → 안내 후 재입력을 위해 PIN 초기화
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) pin = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        // ── 상단 바 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "비밀번호 인증",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // ── 본문 ──
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "간편 비밀번호를\n입력해 주세요",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            CodeBoxes(
                code = pin,
                length = 6,
                masked = true
            )

            if (uiState.loading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "가입을 완료하고 있어요...",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            uiState.error?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Error
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // ── 키패드 ──
        PinKeypad(
            onNumberClick = { digit ->
                if (uiState.error != null) viewModel.clearError()
                if (!uiState.loading && pin.length < 6) pin += digit
            },
            onDelete = {
                if (!uiState.loading && pin.isNotEmpty()) pin = pin.dropLast(1)
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 48.dp)
        )
    }
}
