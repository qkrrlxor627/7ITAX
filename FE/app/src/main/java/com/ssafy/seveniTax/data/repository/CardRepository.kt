package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.card.*
import com.ssafy.seveniTax.data.model.common.ApiResponse

interface CardRepository {
    suspend fun createCard(request: CardCreateRequest): ApiResponse<CardResponse>
    suspend fun activateCard(cardId: String, request: CardActivateRequest): ApiResponse<CardActivateResponse>
    suspend fun setCardPurpose(cardId: String, request: CardPurposeRequest): ApiResponse<CardPurposeResponse>
    suspend fun getMyAccounts(): ApiResponse<List<com.ssafy.seveniTax.data.model.card.CardAccountResponse>>
    suspend fun getCardProducts(): ApiResponse<List<com.ssafy.seveniTax.data.model.card.CardProductResponse>>
    suspend fun getCards(): ApiResponse<List<CardResponse>>
    suspend fun getCard(cardId: String): ApiResponse<CardResponse>
    suspend fun setDefaultCard(cardId: String): ApiResponse<CardResponse>
    suspend fun deleteCard(cardId: String)
}
