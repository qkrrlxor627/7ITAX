package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardInputScreen(
    navController: NavController,
    viewModel: CardViewModel,
    cardType: String = "personal"
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }

    val expiryFocus = remember { FocusRequester() }
    val cvcFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val birthFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isFormComplete = cardNumber.length == 16
            && expiry.length == 4
            && cvc.length == 3
            && password.length == 2
            && birthDate.length == 6

    val previewCardColor = if (cardType == "personal") CardGold else CardBlue
    val cardTypeLabel = if (cardType == "personal") "개인 카드" else "사업자 카드"

    // 카드번호 표시 포맷: 5876 8847 2283 1234
    val displayCardNumber = buildString {
        for (i in 0 until 16) {
            if (i > 0 && i % 4 == 0) append("  ")
            if (i < cardNumber.length) {
                append(cardNumber[i])
            } else {
                append("•")
            }
        }
    }

    val displayExpiry = if (expiry.length == 4) "${expiry.substring(0, 2)}/${expiry.substring(2)}" else ""

    // 자동 포커스 이동
    LaunchedEffect(cardNumber.length) {
        if (cardNumber.length == 16) expiryFocus.requestFocus()
    }
    LaunchedEffect(expiry.length) {
        if (expiry.length == 4) cvcFocus.requestFocus()
    }
    LaunchedEffect(cvc.length) {
        if (cvc.length == 3) passwordFocus.requestFocus()
    }
    LaunchedEffect(password.length) {
        if (password.length == 2) birthFocus.requestFocus()
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
            Text(text = "카드 등록", style = Typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 카드 미리보기
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(previewCardColor)
                    .padding(20.dp)
            ) {
                // 원형 장식
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 15.dp, y = 5.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = cardTypeLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Column {
                        Text(
                            text = "신한 카드",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayCardNumber,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayExpiry,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 카드 번호
            Text(text = "카드 번호", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) cardNumber = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("카드 번호 16자리 입력", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { expiryFocus.requestFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 유효기간 + CVC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "유효기간", style = Typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) expiry = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(expiryFocus),
                        placeholder = { Text("MM/YY", color = TextSecondary) },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { cvcFocus.requestFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Disabled
                        ),
                        singleLine = true,
                        textStyle = Typography.bodyLarge
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "CVC", style = Typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) cvc = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(cvcFocus),
                        placeholder = { Text("숫자 3자리", color = TextSecondary) },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Disabled
                        ),
                        singleLine = true,
                        textStyle = Typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 카드 비밀번호 앞 2자리
            Text(text = "카드 비밀번호 앞 2자리", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus),
                placeholder = { Text("• •", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { birthFocus.requestFocus() }),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 생년월일
            Text(text = "생년월일", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) birthDate = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(birthFocus),
                placeholder = { Text("YYMMDD", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "카드 정보는 암호화되어 안전하게 보관됩니다.",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            TaxButton(
                text = "다음으로",
                onClick = {
                    viewModel.updateCardNumber(cardNumber)
                    viewModel.updateExpiry(expiry)
                    navController.navigate(Route.CardOwnerVerify.path)
                },
                enabled = isFormComplete
            )

            Spacer(modifier = Modifier
                .navigationBarsPadding()
                .height(32.dp))
        }
    }
}
