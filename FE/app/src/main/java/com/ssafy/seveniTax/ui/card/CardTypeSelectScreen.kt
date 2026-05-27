package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardTypeSelectScreen(navController: NavController, viewModel: CardViewModel) {
    var selectedType by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "등록할 카드 유형을 선택하세요",
                style = Typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 개인 카드
            CardTypeCard(
                backgroundColor = CardGold,
                title = "개인 카드",
                cardName = "Amazon Platinium",
                cardNumber = "4756  ••••  ••••  9018",
                description = "개인 결제용 카드 등록",
                isSelected = selectedType == "personal",
                onClick = { selectedType = "personal" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 사업자 카드
            CardTypeCard(
                backgroundColor = CardBlue,
                title = "사업자 카드",
                cardName = "Amazon Platinium",
                cardNumber = "4756  ••••  ••••  9018",
                description = "사업 관련 결제 및 추후 증빙/관리용 카드 등록",
                isSelected = selectedType == "business",
                onClick = { selectedType = "business" }
            )
        }

        // 하단 버튼
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            TaxButton(
                text = "다음으로",
                onClick = {
                    selectedType?.let { viewModel.selectCardType(it) }
                    navController.navigate(Route.CardAccountSelect.path)
                },
                enabled = selectedType != null
            )
        }
    }
}

@Composable
private fun CardTypeCard(
    backgroundColor: Color,
    title: String,
    cardName: String,
    cardNumber: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) Modifier.border(2.dp, BrandPurple, RoundedCornerShape(16.dp))
                else Modifier
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        // 우측 상단 원형 장식
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-5).dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp, y = 15.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        )

        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = cardName,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cardNumber,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
