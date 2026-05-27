package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.ssafy.seveniTax.viewmodel.RegisteredCard

@Composable
fun CardListScreen(navController: NavController, viewModel: CardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val cards = uiState.cards
    val isEmpty = cards.isEmpty()

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
            Text(
                text = "카드 관리",
                style = Typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            if (isEmpty) {
                // 빈 상태 — 점선 카드 + 안내
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .border(
                            border = BorderStroke(1.5.dp, LogoPurple.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "등록된 카드가 없습니다",
                        style = Typography.bodyMedium,
                        color = LogoPurple.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "카드를 등록하면 간편하게\n결제를 이용할 수 있어요",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                TaxButton(
                    text = "카드 등록하기",
                    onClick = { navController.navigate(Route.CardTypeSelect.path) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "일반 · 사업자 카드 최대 5장까지 등록 가능",
                    style = Typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 카드 목록
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cards) { card ->
                        CardWidget(
                            card = card,
                            onClick = {
                                navController.navigate(Route.CardDetail.create(card.id))
                            }
                        )
                    }
                }

                // 하단 버튼
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaxButton(
                        text = "카드 등록하기",
                        onClick = { navController.navigate(Route.CardTypeSelect.path) }
                    )
                    TaxButton(
                        text = "기본 카드 변경",
                        onClick = { navController.navigate(Route.CardChange.path) },
                        variant = ButtonVariant.Secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CardWidget(
    card: RegisteredCard,
    onClick: () -> Unit
) {
    val bgColor = if (card.type == "personal") CardGold else CardBlue
    val typeName = if (card.type == "personal") "개인 카드" else "사업자 카드"
    val displayName = if (card.isDefault) "$typeName (기본)" else typeName

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
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
                text = displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Column {
                Text(
                    text = card.cardNumber,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.expiry,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
