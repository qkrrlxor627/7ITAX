package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.card.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.CardApi
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardApi: CardApi
) : CardRepository {

    override suspend fun createCard(request: CardCreateRequest): ApiResponse<CardResponse> {
        val response = cardApi.createCard(request)
        return response.body() ?: throw Exception(extractErrorMessage(response, "카드 생성에 실패했습니다."))
    }

    override suspend fun activateCard(cardId: String, request: CardActivateRequest): ApiResponse<CardActivateResponse> {
        val response = cardApi.activateCard(cardId, request)
        return response.body() ?: throw Exception(extractErrorMessage(response, "카드 활성화에 실패했습니다."))
    }

    override suspend fun setCardPurpose(cardId: String, request: CardPurposeRequest): ApiResponse<CardPurposeResponse> {
        val response = cardApi.setCardPurpose(cardId, request)
        return response.body() ?: throw Exception(extractErrorMessage(response, "카드 용도 설정에 실패했습니다."))
    }

    override suspend fun getMyAccounts(): ApiResponse<List<com.ssafy.seveniTax.data.model.card.CardAccountResponse>> {
        val response = cardApi.getMyAccounts()
        return response.body() ?: throw Exception(extractErrorMessage(response, "계좌 목록 조회에 실패했습니다."))
    }

    override suspend fun getCardProducts(): ApiResponse<List<com.ssafy.seveniTax.data.model.card.CardProductResponse>> {
        val response = cardApi.getCardProducts()
        return response.body() ?: throw Exception(extractErrorMessage(response, "카드 상품 조회에 실패했습니다."))
    }

    override suspend fun getCards(): ApiResponse<List<CardResponse>> {
        val response = cardApi.getCards()
        return response.body() ?: throw Exception(extractErrorMessage(response, "카드 목록 조회에 실패했습니다."))
    }

    override suspend fun getCard(cardId: String): ApiResponse<CardResponse> {
        val response = cardApi.getCard(cardId)
        return response.body() ?: throw Exception(extractErrorMessage(response, "카드 조회에 실패했습니다."))
    }

    override suspend fun setDefaultCard(cardId: String): ApiResponse<CardResponse> {
        val response = cardApi.setDefaultCard(cardId)
        return response.body() ?: throw Exception(extractErrorMessage(response, "기본 카드 변경에 실패했습니다."))
    }

    override suspend fun deleteCard(cardId: String) {
        val response = cardApi.deleteCard(cardId)
        if (!response.isSuccessful) {
            throw Exception(extractErrorMessage(response, "카드 삭제에 실패했습니다."))
        }
    }

    private fun <T> extractErrorMessage(response: Response<T>, fallback: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                JSONObject(errorBody).optString("message", fallback)
            } else fallback
        } catch (_: Exception) { fallback }
    }
}
