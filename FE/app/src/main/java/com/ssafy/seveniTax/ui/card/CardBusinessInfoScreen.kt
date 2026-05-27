package com.ssafy.seveniTax.ui.card

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun CardBusinessInfoScreen(navController: NavController) {
    var businessNumber by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var representativeName by remember { mutableStateOf("") }

    val businessNameFocus = remember { FocusRequester() }
    val representativeNameFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isFormComplete = businessNumber.length == 10
            && businessName.isNotBlank()
            && representativeName.isNotBlank()

    // 사업자등록번호 10자리 입력 완료 시 자동 포커스 이동
    LaunchedEffect(businessNumber.length) {
        if (businessNumber.length == 10) {
            businessNameFocus.requestFocus()
        }
    }

    // Format business number for display (123-45-67890)
    val displayBusinessNumber = buildString {
        for (i in businessNumber.indices) {
            if (i == 3 || i == 5) append("-")
            append(businessNumber[i])
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "사업자 정보 등록",
                style = Typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "사업자 정보를\n등록해주세요",
                style = Typography.headlineMedium
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "입력한 정보는 사업자 카드 구분 및 관련 기능을 제공하기 위해 활용됩니다",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            TaxButton(
                text = "등록하기",
                onClick = {
                    navController.navigate(Route.CardInput.create("business"))
                },
                enabled = isFormComplete
            )

            Spacer(modifier = Modifier
                .navigationBarsPadding()
                .height(32.dp))
        }
    }
}
