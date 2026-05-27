package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.CodeBoxes
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.Error
import com.ssafy.seveniTax.ui.theme.KeypadBg
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun PinConfirmScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.pinConfirm.length) {
        if (uiState.pinConfirm.length == 6) {
            viewModel.confirmPin(
                onSuccess = { navController.navigate(Route.AuthSuccess.path) },
                onMismatch = {},
                onResetRequired = {
                    navController.popBackStack(Route.PinSetup.path, inclusive = true)
                    navController.navigate(Route.PinSetup.path)
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
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(88.dp))
            Text(
                text = "간편 비밀번호",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 42.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "한 번 더 입력해주세요",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 42.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            CodeBoxes(
                code = uiState.pinConfirm,
                length = 6,
                masked = true
            )

            if (uiState.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Error
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        PinKeypad(
            onNumberClick = { viewModel.appendPinConfirm(it.toString()) },
            onDelete = { viewModel.deletePinConfirm() },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 48.dp)
        )
    }
}
