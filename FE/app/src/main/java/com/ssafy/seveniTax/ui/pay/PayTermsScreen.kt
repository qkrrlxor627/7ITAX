package com.ssafy.seveniTax.ui.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

private data class PayTerm(
    val title: String,
    val required: Boolean
)

private val payTerms = listOf(
    PayTerm("페이 서비스 이용약관", true),
    PayTerm("개인정보 수집 및 이용 동의", true),
    PayTerm("전자금융 이용약관", true),
    PayTerm("결제내역 수집 및 이용 동의", true),
    PayTerm("자동 기장 처리 동의", true),
    PayTerm("마케팅 정보 수신 동의", false),
)

@Composable
fun PayTermsScreen(navController: NavController) {
    val agreedSet = remember { mutableStateMapOf<Int, Boolean>() }
    val allChecked = payTerms.indices.all { agreedSet[it] == true }
    val allRequiredChecked = payTerms.indices
        .filter { payTerms[it].required }
        .all { agreedSet[it] == true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        // ── 상단 바 ──
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
                text = "약관 동의",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // ── 본문 ──
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "서비스 이용을 위해\n약관에 동의해 주세요",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 전체 동의
            TermCheckRow(
                text = "아래 약관에 모두 동의합니다",
                checked = allChecked,
                onCheckedChange = {
                    val newValue = !allChecked
                    payTerms.indices.forEach { agreedSet[it] = newValue }
                },
                bold = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 개별 약관
            payTerms.forEachIndexed { index, term ->
                TermCheckRow(
                    text = "${term.title} (${if (term.required) "필수" else "선택"})",
                    checked = agreedSet[index] == true,
                    onCheckedChange = {
                        agreedSet[index] = !(agreedSet[index] ?: false)
                    },
                    showArrow = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── 하단 버튼 ──
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            TaxButton(
                text = "다음으로",
                onClick = { navController.navigate(Route.PayVerify.path) },
                enabled = allRequiredChecked
            )
        }
    }
}

@Composable
private fun TermCheckRow(
    text: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    bold: Boolean = false,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(8.dp))
            .clickable { onCheckedChange() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = BrandPurple,
                uncheckedColor = Disabled
            ),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "상세보기",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
