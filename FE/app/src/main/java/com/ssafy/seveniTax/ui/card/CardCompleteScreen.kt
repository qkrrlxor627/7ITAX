package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.ButtonVariant
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardCompleteScreen(navController: NavController, viewModel: CardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val registeredCard = uiState.lastRegisteredCard
    val cardColor = if (registeredCard?.type == "business") CardBlue else CardGold
    val cardTypeName = if (registeredCard?.type == "business") "사업자 카드" else "개인 카드"
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
            Text(text = "카드 등록 완료", style = Typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 카드 미리보기
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColor)
                    .padding(20.dp)
            ) {
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
                        text = cardTypeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Column {
                        Text(
                            text = registeredCard?.cardNumber ?: "",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = registeredCard?.expiry ?: "",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "카드 등록이 완료되었어요!",
                style = Typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "이 카드로 바로 결제할 수 있어요\n결제 탭에서 결제를 시작해보세요",
                style = Typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 하단 버튼
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaxButton(
                text = "카드 관리 보기",
                onClick = {
                    viewModel.resetRegistration()
                    navController.navigate(Route.CardList.path) {
                        popUpTo(Route.CardList.path) { inclusive = true }
                    }
                }
            )
            TaxButton(
                text = "홈으로 이동",
                onClick = {
                    viewModel.resetRegistration()
                    navController.navigate(Route.Main.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                variant = ButtonVariant.Secondary
            )
        }
    }
}
