package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.chatbot.ChatbotRequest
import com.ssafy.seveniTax.data.model.chatbot.ChatbotResponse
import com.ssafy.seveniTax.data.model.chatbot.ChatHistoryResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response

interface ChatbotRepository {
    suspend fun sendMessage(request: ChatbotRequest): Response<ApiResponse<ChatbotResponse>>
    suspend fun getHistory(sessionId: String, page: Int = 0, size: Int = 50): Response<ApiResponse<ChatHistoryResponse>>
}
