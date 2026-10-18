package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Accent
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.BrandPurple
import com.ssafy.seveniTax.ui.theme.Disabled
import com.ssafy.seveniTax.ui.theme.Error
import com.ssafy.seveniTax.ui.theme.Surface
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary
import com.ssafy.seveniTax.viewmodel.AuthViewModel

/** 시드 사용자 바로가기 (BE CardDataInitializer와 같은 번호) */
private data class QuickUser(val name: String, val phone: String, val desc: String)

private val QUICK_USERS = listOf(
    QuickUser("이싸피", "01022222222", "사업자 카드 · 결제하면 장부 자동 생성"),
    QuickUser("김싸피", "01011111111", "개인 카드 · 장부 생성 안 됨"),
)

/**
 * 간편 로그인(DEBUG 빌드 전용). 본인인증·PIN 없이 전화번호로 사용자를 구분한다.
 * 처음 보는 번호면 BE가 사용자·계좌(500만원)·사업자 카드를 만들어 준다
 */
@Composable
fun SimpleLoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf(viewModel.getStoredPhoneNumber().filter(Char::isDigit)) }
    var name by remember { mutableStateOf("") }
    val phoneValid = Regex("^01\\d{8,9}$").matches(phone)

    fun login(targetPhone: String, targetName: String) {
        viewModel.simpleLogin(targetPhone, targetName) {
            navController.navigate(Route.Main.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("간편 로그인", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "개발용 로그인입니다. 전화번호가 곧 사용자입니다.\n처음 쓰는 번호는 계좌(500만원)와 사업자 카드가 자동으로 만들어집니다.",
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))
        Text("바로 로그인", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        QUICK_USERS.forEach { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, Disabled, RoundedCornerShape(14.dp))
                    .clickable(enabled = !uiState.isLoading) { login(user.phone, user.name) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(user.desc, fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(formatPhoneForm(user.phone), fontSize = 13.sp, color = Accent, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("다른 번호로 로그인", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit).take(11) },
            label = { Text("전화번호 (예: 01033333333)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPurple),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            label = { Text("이름 (처음 쓰는 번호일 때만, 선택)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPurple),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(uiState.errorMessage, fontSize = 13.sp, color = Error)
        }

        Spacer(modifier = Modifier.height(20.dp))
        TaxButton(
            text = if (uiState.isLoading) "로그인 중..." else "로그인",
            onClick = { login(phone, name) },
            enabled = phoneValid && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
