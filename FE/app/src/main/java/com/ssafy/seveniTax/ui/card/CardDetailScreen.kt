package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardDetailScreen(
    navController: NavController,
    viewModel: CardViewModel,
    cardId: String
) {
    val card = viewModel.getCardById(cardId)
    val cardName = if (card?.type == "personal") "개인 카드" else "사업자 카드"
    val cardNumber = card?.cardNumber ?: ""
    val cardExpiry = card?.expiry ?: ""

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
                text = "카드 관리 상세",
                style = Typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Info rows
            DetailInfoRow(label = "이름", value = cardName)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Divider
            )
            DetailInfoRow(label = "카드 번호", value = cardNumber)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Divider
            )
            DetailInfoRow(label = "유효기간", value = cardExpiry)

            Spacer(modifier = Modifier.weight(1f))

            // 카드 삭제하기 button
            Text(
                text = "카드 삭제하기",
                style = Typography.titleMedium,
                color = Error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.deleteCard(cardId)
                        navController.popBackStack()
                    }
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier
                .navigationBarsPadding()
                .height(32.dp))
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = Typography.bodyLarge,
            color = TextPrimary
        )
    }
}
