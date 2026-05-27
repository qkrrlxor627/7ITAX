package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.CardViewModel
import kotlinx.coroutines.delay

@Composable
fun CardSmsScreen(navController: NavController, viewModel: CardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var smsCode by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    // 등록 성공 시 완료 화면 이동
    LaunchedEffect(uiState.registerComplete) {
        if (uiState.registerComplete && submitted) {
            navController.navigate(Route.CardComplete.path)
        }
    }

    // 에러 시 다이얼로그
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty() && submitted) {
            errorText = uiState.errorMessage
            showErrorDialog = true
            viewModel.clearError()
            submitted = false
            smsCode = ""
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("카드 등록 실패", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { Text(errorText) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("확인", color = BrandPurple)
                }
            }
        )
    }
    var remainingSeconds by remember { mutableIntStateOf(180) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
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
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 16.dp, start = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "문자로 받은 인증번호\n6자리를 입력해 주세요",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "카드 명의자 휴대폰으로 인증번호를 보냈어요",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicTextField(
                    value = smsCode,
                    onValueChange = { code ->
                        if (code.length <= 6 && code.all(Char::isDigit)) {
                            smsCode = code
                            if (code.length == 6 && !submitted) {
                                submitted = true
                                viewModel.completeRegistration()
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        letterSpacing = 8.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester),
                    cursorBrush = SolidColor(BrandPurple)
                )
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingSeconds <= 30) Error else BrandPurple
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BrandPurple)
        }
    }
}
