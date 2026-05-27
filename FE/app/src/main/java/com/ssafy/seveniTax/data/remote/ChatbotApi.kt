package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.chatbot.ChatbotRequest
import com.ssafy.seveniTax.data.model.chatbot.ChatbotResponse
import com.ssafy.seveniTax.data.model.chatbot.ChatHistoryResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.*

interface ChatbotApi {

    @POST("chatbot")
    suspend fun sendMessage(
        @Body body: ChatbotRequest
    ): Response<ApiResponse<ChatbotResponse>>

    @GET("chatbot/history/{sessionId}")
    suspend fun getHistory(
        @Path("sessionId") sessionId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<ChatHistoryResponse>>
}
