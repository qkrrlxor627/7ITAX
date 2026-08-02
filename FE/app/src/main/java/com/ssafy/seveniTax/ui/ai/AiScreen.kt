package com.ssafy.seveniTax.ui.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.viewmodel.ChatMessage
import com.ssafy.seveniTax.viewmodel.ChatbotViewModel

// ── 색상 ──
private val Primary900 = Color(0xFF281C9D)
private val Primary600 = Color(0xFF5655B9)
private val Primary300 = Color(0xFFA8A3D7)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral500 = Color(0xFF898989)
private val Neutral400 = Color(0xFF989898)
private val Neutral300 = Color(0xFFCACACA)
private val Neutral200 = Color(0xFFE8E8E8)
private val Neutral50 = Color(0xFFF7F7F7)
private val ErrorColor = Color(0xFFFF4267)
private val SuccessColor = Color(0xFF52D5BA)

private val PurpleGradient = Brush.linearGradient(listOf(Primary900, Color(0xFF3A2FB0)))
private val SendGradient = Brush.linearGradient(listOf(Primary900, Primary600))

@Composable
fun AiScreen(
    navController: NavController,
    viewModel: ChatbotViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // 새 메시지 추가 시 자동 스크롤
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        ChatHeader(
            onBack = { navController.popBackStack() },
            onNewChat = { viewModel.newSession() }
        )

        // Chat Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { DateDividerRow() }

            items(uiState.messages, key = { it.id }) { msg ->
                when {
                    msg.isLoading -> TypingIndicator()
                    msg.isUser -> UserBubble(msg)
                    msg.isError -> AiErrorBubble(msg) { viewModel.retry() }
                    else -> AiBubble(msg)
                }
            }
        }

        // Input Area
        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            enabled = !uiState.isSending,
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            }
        )
    }
}

// ── Header ──
@Composable
private fun ChatHeader(onBack: () -> Unit, onNewChat: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary900)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("AI 챗봇", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(SuccessColor)
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onNewChat),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ── Date Divider ──
@Composable
private fun DateDividerRow() {
    val cal = java.util.Calendar.getInstance()
    val date = "${cal.get(java.util.Calendar.YEAR)}년 ${cal.get(java.util.Calendar.MONTH) + 1}월 ${cal.get(java.util.Calendar.DAY_OF_MONTH)}일"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Neutral200)
        Text(date, modifier = Modifier.padding(horizontal = 14.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Neutral400)
        HorizontalDivider(modifier = Modifier.weight(1f), color = Neutral200)
    }
}

// ── User Bubble ──
@Composable
private fun UserBubble(msg: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(PurpleGradient)
                .padding(12.dp)
        ) {
            Text(msg.text, fontSize = 13.sp, lineHeight = 21.sp, color = Color.White)
        }
        Text(msg.time, fontSize = 10.sp, color = Neutral300, modifier = Modifier.padding(top = 4.dp, end = 2.dp))
    }
}

// ── AI Bubble ──
@Composable
private fun AiBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("7iTAX AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Neutral500)
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(Neutral50)
                    .border(1.dp, Neutral200, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .padding(12.dp)
            ) {
                Text(msg.text, fontSize = 13.sp, lineHeight = 21.sp, color = Neutral900)
            }
            Text(msg.time, fontSize = 10.sp, color = Neutral300, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
    }
}

// ── AI Error Bubble ──
@Composable
private fun AiErrorBubble(msg: ChatMessage, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("7iTAX AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Neutral500)
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(Color(0xFFFFF5F7))
                    .border(1.dp, Color(0x26FF4267), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("⚠️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(msg.text, fontSize = 13.sp, lineHeight = 21.sp, color = ErrorColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.clickable(onClick = onRetry),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔄", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("다시 질문하기", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary600)
                    }
                }
            }
            Text(msg.time, fontSize = 10.sp, color = Neutral300, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
    }
}

// ── Avatar ──
@Composable
private fun AiAvatar() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Primary900, Primary600))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ai_sparkle),
            contentDescription = "AI",
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ── Typing Indicator ──
@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(Neutral50)
                .border(1.dp, Neutral200, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index -> TypingDot(delay = index * 200) }
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1400; 0f at 0; -4f at 300; 0f at 600; 0f at 1400 },
            initialStartOffset = StartOffset(delay)
        ), label = "dot"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1400; 0.3f at 0; 1f at 300; 0.3f at 600; 0.3f at 1400 },
            initialStartOffset = StartOffset(delay)
        ), label = "alpha"
    )
    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(Primary300.copy(alpha = alpha))
    )
}

// ── Chat Input Bar ──
@Composable
private fun ChatInputBar(text: String, onTextChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Neutral50)
            .border(1.5.dp, Neutral200, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 5.dp, bottom = 5.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = { Text("세무 관련 질문을 입력하세요", fontSize = 13.sp, color = Neutral400) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Primary900
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Neutral900),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled && text.isNotBlank()) SendGradient else Brush.linearGradient(listOf(Neutral300, Neutral300)))
                .clickable(enabled = enabled && text.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Text("➤", fontSize = 16.sp, color = Color.White)
        }
    }
}
