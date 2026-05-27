package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.CodeBoxes
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Accent
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.Error
import com.ssafy.seveniTax.ui.theme.KeypadBg
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun PinLoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loginPin.length) {
        if (uiState.loginPin.length == 6) {
            viewModel.loginWithPin(
                onSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "간편 비밀번호를\n입력 해주세요",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            CodeBoxes(
                code = uiState.loginPin,
                length = 6,
                masked = true
            )

            if (uiState.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Error
                )
            }

            if (uiState.pinNotSet) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "본인인증 후 PIN 재설정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Accent,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        viewModel.resetForPinSetup()
                        navController.navigate(Route.IdentityVerify.path) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        PinKeypad(
            onNumberClick = { viewModel.appendLoginPin(it.toString()) },
            onDelete = { viewModel.deleteLoginPin() },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 48.dp)
        )
    }
}
