package com.ssafy.seveniTax.ui.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun PayBusinessInfoScreen(navController: NavController) {
    // TODO: 서버에서 기존 사업자 정보 조회
    val hasExistingInfo = false

    var businessNumber by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var representativeName by remember { mutableStateOf("") }

    val businessNameFocus = remember { FocusRequester() }
    val representativeNameFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isFormComplete = businessNumber.length == 10
            && businessName.isNotBlank()
            && representativeName.isNotBlank()

    LaunchedEffect(businessNumber.length) {
        if (businessNumber.length == 10) {
            businessNameFocus.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "사업자 정보 확인",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "사업자 정보를\n확인해주세요",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "페이 서비스 가입에 필요한 정보입니다",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 사업자등록번호
            Text(text = "사업자등록번호", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = businessNumber,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) businessNumber = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("123-45-67890", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { businessNameFocus.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 상호명
            Text(text = "상호명", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(businessNameFocus),
                placeholder = { Text("상호명을 입력하세요", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { representativeNameFocus.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 대표자명
            Text(text = "대표자명", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = representativeName,
                onValueChange = { representativeName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(representativeNameFocus),
                placeholder = { Text("대표자명을 입력하세요", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            TaxButton(
                text = "다음으로",
                onClick = { navController.navigate(Route.PayTerms.path) },
                enabled = isFormComplete
            )
        }
    }
}
