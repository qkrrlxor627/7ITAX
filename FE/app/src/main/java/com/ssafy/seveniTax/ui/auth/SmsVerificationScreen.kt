package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SmsVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var smsCode by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableIntStateOf(180) }
    var isExpired by remember { mutableStateOf(false) }

    // 3분 카운트다운
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        isExpired = true
    }

    // 6자리 입력 완료 시 다음 화면으로 이동
    LaunchedEffect(smsCode.length) {
        if (smsCode.length == 6) {
            // TODO: 서버에 인증번호 검증 API 호출
            navController.navigate(Route.PinSetup.path)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 타이틀
            Text(
                text = "문자로 받은\n인증번호 6자리를 입력해주세요",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 타이머
            Text(
                text = "남은 시간 $timerText",
                style = Typography.bodySmall,
                color = if (isExpired) Error else TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 인증번호 6자리 박스
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                color = if (index < smsCode.length) Surface else Color(0xFFD9D9D9).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < smsCode.length) {
                            Text(
                                text = smsCode[index].toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 문자가 안오나요?
            Text(
                text = "문자가 안오나요?",
                style = Typography.bodyMedium,
                color = Accent,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // TODO: 인증번호 재발송
                        remainingSeconds = 180
                        isExpired = false
                        smsCode = ""
                    }
                    .padding(vertical = 8.dp)
            )
        }

        PinKeypad(
            onNumberClick = { digit ->
                if (smsCode.length < 6 && !isExpired) {
                    smsCode += digit
                }
            },
            onDelete = {
                if (smsCode.isNotEmpty()) {
                    smsCode = smsCode.dropLast(1)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 8.dp)
        )
    }
}
