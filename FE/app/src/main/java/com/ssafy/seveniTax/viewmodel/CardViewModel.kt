package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.card.CardCreateRequest
import com.ssafy.seveniTax.data.model.card.CardResponse
import com.ssafy.seveniTax.data.model.pay.AccountCreateRequest
import com.ssafy.seveniTax.data.repository.CardRepository
import com.ssafy.seveniTax.data.repository.PayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisteredCard(
    val id: String,
    val cardNumber: String,
    val expiry: String,
    val type: String,
    val isDefault: Boolean = false
) {
    companion object {
        fun from(response: CardResponse): RegisteredCard {
            return RegisteredCard(
                id = response.id.toString(),
                cardNumber = "••••  ••••  ••••  ${response.last4Digits}",
                expiry = response.cardExpiryDate ?: "",
                type = if (response.cardType == "BUSINESS") "business" else "personal",
                isDefault = response.isDefault
            )
        }
    }
}

data class CardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val cards: List<RegisteredCard> = emptyList(),
    val selectedCardType: String = "",
    val cardNumber: String = "",
    val expiry: String = "",
    val lastRegisteredCard: RegisteredCard? = null,
    val registerComplete: Boolean = false,
    // 카드 등록 플로우용
    val accounts: List<com.ssafy.seveniTax.data.model.card.CardAccountResponse> = emptyList(),
    val products: List<com.ssafy.seveniTax.data.model.card.CardProductResponse> = emptyList(),
    val selectedAccountNo: String = "",
    val selectedProductNo: String = ""
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val payRepository: PayRepository
) : ViewModel() {



    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    fun loadCards() = viewModelScope.launch {
        Log.d("CardVM", "▶ loadCards() 호출")
        _uiState.update { it.copy(isLoading = true) }
        try {
            val response = cardRepository.getCards()
            Log.d("CardVM", "  loadCards 응답: status=${response.status}, data=${response.data?.size}, message=${response.message}")
            if (response.status == "success" && response.data != null) {
                _uiState.update {
                    it.copy(
                        cards = response.data.map { card -> RegisteredCard.from(card) },
                        isLoading = false
                    )
                }
            } else {
                Log.e("CardVM", "  loadCards 실패: ${response.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = response.message ?: "카드 목록 조회 실패") }
            }
        } catch (e: Exception) {
            Log.e("CardVM", "  loadCards 에러: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
    }

    fun loadAccounts() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val response = cardRepository.getMyAccounts()
            if (response.status == "success" && response.data != null) {
                if (response.data.isEmpty()) {
                    try {
                        payRepository.createAccount(AccountCreateRequest(accountType = "PERSONAL", bankCode = "001"))
                        val retry = cardRepository.getMyAccounts()
                        if (retry.status == "success" && retry.data != null) {
                            _uiState.update { it.copy(accounts = retry.data, isLoading = false) }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "계좌 자동 생성에 실패했습니다") }
                    }
                } else {
                    _uiState.update { it.copy(accounts = response.data, isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "계좌 조회 실패") }
        }
    }

    fun loadProducts() = viewModelScope.launch {
        try {
            val response = cardRepository.getCardProducts()
            if (response.status == "success" && response.data != null) {
                _uiState.update { it.copy(products = response.data) }
            } else {
            }
        } catch (e: Exception) {
        }
    }

    fun selectAccount(accountNo: String) {
        _uiState.update { it.copy(selectedAccountNo = accountNo) }
    }

    fun selectProduct(productNo: String) {
        _uiState.update { it.copy(selectedProductNo = productNo) }
    }

    fun selectCardType(type: String) {
        _uiState.update { it.copy(selectedCardType = type) }
    }

    fun updateCardNumber(value: String) {
        _uiState.update { it.copy(cardNumber = value) }
    }

    fun updateExpiry(value: String) {
        _uiState.update { it.copy(expiry = value) }
    }

    fun completeRegistration() = viewModelScope.launch {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true) }
        try {
            val request = CardCreateRequest(
                cardName = if (state.selectedCardType == "business") "사업자 카드" else "개인 카드",
                cardType = if (state.selectedCardType == "business") "BUSINESS" else "PERSONAL",
                cardUniqueNo = state.selectedProductNo,
                withdrawalAccountNo = state.selectedAccountNo,
                withdrawalDate = "4",
                otpToken = "test-token" // TODO: OTP 연동 시 실제 토큰으로 교체
            )
            val response = cardRepository.createCard(request)
            if (response.status == "success" && response.data != null) {
                val newCard = RegisteredCard.from(response.data)
                _uiState.update {
                    it.copy(
                        lastRegisteredCard = newCard,
                        registerComplete = true,
                        isLoading = false
                    )
                }
                loadCards()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "카드 생성에 실패했습니다"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "카드 생성 중 오류가 발생했습니다"
                )
            }
        }
    }

    fun deleteCard(cardId: String) = viewModelScope.launch {
        try {
            cardRepository.deleteCard(cardId)
        } catch (_: Exception) { }
        _uiState.update { state ->
            val deletedCard = state.cards.find { it.id == cardId }
            val remaining = state.cards.filter { it.id != cardId }
            val finalCards = if (deletedCard?.isDefault == true && remaining.isNotEmpty()) {
                remaining.mapIndexed { index, card ->
                    if (index == 0) card.copy(isDefault = true) else card
                }
            } else remaining
            state.copy(cards = finalCards)
        }
    }

    fun setDefaultCard(cardId: String) = viewModelScope.launch {
        try {
            cardRepository.setDefaultCard(cardId)
            loadCards()
        } catch (_: Exception) {
            _uiState.update { state ->
                state.copy(
                    cards = state.cards.map { it.copy(isDefault = it.id == cardId) }
                )
            }
        }
    }

    fun getCardById(cardId: String): RegisteredCard? {
        return _uiState.value.cards.find { it.id == cardId }
    }

    fun resetRegistration() {
        _uiState.update {
            it.copy(
                selectedCardType = "",
                cardNumber = "",
                expiry = "",
                lastRegisteredCard = null,
                registerComplete = false,
                selectedAccountNo = "",
                selectedProductNo = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
