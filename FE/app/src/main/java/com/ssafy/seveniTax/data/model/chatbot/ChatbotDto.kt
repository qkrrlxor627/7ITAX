package com.ssafy.seveniTax.data.model.chatbot

data class ChatbotRequest(
    val message: String,
    val sessionId: String? = null
)

data class ChatbotResponse(
    val answer: String,
    val model: String? = null,
    val sessionId: String
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatHistoryResponse(
    val sessionId: String,
    val messages: List<ChatMessageDto> = emptyList(),
    val messageCount: Int = 0
)
