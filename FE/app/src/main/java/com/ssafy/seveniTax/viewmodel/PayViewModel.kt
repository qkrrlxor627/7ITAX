package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.pay.AccountResponse
import com.ssafy.seveniTax.data.model.pay.BalanceResponse
import com.ssafy.seveniTax.data.repository.PayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val hasPayAccount: Boolean = false,
    val accounts: List<AccountResponse> = emptyList(),
    val currentBalance: BalanceResponse? = null,
    // Pay 가입
    val payTermsAgreed: Boolean = false,
    val payVerified: Boolean = false,
    val payComplete: Boolean = false
)

@HiltViewModel
class PayViewModel @Inject constructor(
    private val payRepository: PayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayUiState())
    val uiState: StateFlow<PayUiState> = _uiState.asStateFlow()

    fun checkPayAccount() = viewModelScope.launch {
        TODO("Implement: payRepository.getAccounts() → hasPayAccount 갱신")
    }

    fun loadAccounts() = viewModelScope.launch {
        TODO("Implement: payRepository.getAccounts()")
    }

    fun loadBalance(accountId: String) = viewModelScope.launch {
        TODO("Implement: payRepository.getBalance(accountId)")
    }

    fun agreePayTerms() {
        _uiState.update { it.copy(payTermsAgreed = true) }
    }

    fun verifyIdentity() = viewModelScope.launch {
        TODO("Implement: PASS/신분증 인증 처리 → payVerified = true")
    }

    fun createAccount(type: String, bankCode: String, alias: String? = null) = viewModelScope.launch {
        TODO("Implement: payRepository.createAccount() → payComplete = true")
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
