package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.chatbot.ChatbotRequest
import com.ssafy.seveniTax.data.model.chatbot.ChatbotResponse
import com.ssafy.seveniTax.data.model.chatbot.ChatHistoryResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.ChatbotApi
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatbotRepositoryImpl @Inject constructor(
    private val api: ChatbotApi
) : ChatbotRepository {

    override suspend fun sendMessage(request: ChatbotRequest): Response<ApiResponse<ChatbotResponse>> =
        api.sendMessage(request)

    override suspend fun getHistory(sessionId: String, page: Int, size: Int): Response<ApiResponse<ChatHistoryResponse>> =
        api.getHistory(sessionId, page, size)
}
