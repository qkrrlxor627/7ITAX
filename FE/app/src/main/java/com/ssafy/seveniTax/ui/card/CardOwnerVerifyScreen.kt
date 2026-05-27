package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun CardOwnerVerifyScreen(navController: NavController) {
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
                text = "카드 소유자 인증",
                style = Typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "카드 명의자 본인 여부를\n확인합니다",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SMS 인증
            VerifyOptionItem(
                icon = Icons.Default.Phone,
                title = "SMS 인증",
                onClick = { navController.navigate(Route.CardSms.path) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1원 결제 인증
            VerifyOptionItem(
                icon = Icons.Default.Email,
                title = "1원 결제 인증",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 카드사 인증
            VerifyOptionItem(
                icon = Icons.Default.Lock,
                title = "카드사 인증",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "인증 5회 실패 시 등록이 제한됩니다",
                style = Typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun VerifyOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = Typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}
