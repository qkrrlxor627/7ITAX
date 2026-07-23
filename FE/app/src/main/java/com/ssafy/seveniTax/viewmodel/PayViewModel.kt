package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.pay.AccountCreateRequest
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
    val payComplete: Boolean = false,
    val payPinRegistered: Boolean = false
)

@HiltViewModel
class PayViewModel @Inject constructor(
    private val payRepository: PayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayUiState())
    val uiState: StateFlow<PayUiState> = _uiState.asStateFlow()

    /** Pay 계좌 보유 여부 확인 (진입 시 분기용). */
    fun checkPayAccount() = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val response = payRepository.getAccounts()
            if (response.status == "success" && response.data != null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        accounts = response.data,
                        hasPayAccount = response.data.isNotEmpty()
                    )
                }
            } else {
                _uiState.update { it.copy(loading = false, error = response.message.ifBlank { "계좌 조회에 실패했습니다." }) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: "네트워크 오류가 발생했습니다.") }
        }
    }

    /** 계좌 목록 조회. */
    fun loadAccounts() = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val response = payRepository.getAccounts()
            if (response.status == "success" && response.data != null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        accounts = response.data,
                        hasPayAccount = response.data.isNotEmpty()
                    )
                }
            } else {
                _uiState.update { it.copy(loading = false, error = response.message.ifBlank { "계좌 목록 조회에 실패했습니다." }) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: "네트워크 오류가 발생했습니다.") }
        }
    }

    /** 특정 계좌의 잔액 조회. */
    fun loadBalance(accountId: String) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val response = payRepository.getBalance(accountId)
            if (response.status == "success" && response.data != null) {
                _uiState.update { it.copy(loading = false, currentBalance = response.data) }
            } else {
                _uiState.update { it.copy(loading = false, error = response.message.ifBlank { "잔액 조회에 실패했습니다." }) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: "네트워크 오류가 발생했습니다.") }
        }
    }

    fun agreePayTerms() {
        _uiState.update { it.copy(payTermsAgreed = true) }
    }

    /**
     * 본인인증 처리.
     * 로컬 데모 뱅킹에는 외부 PASS/신분증 인증 연동이 없어 통과 처리한다.
     * 실제 PASS 연동 시 이 지점에서 인증 결과를 검증하도록 교체할 것.
     */
    fun verifyIdentity() = viewModelScope.launch {
        _uiState.update { it.copy(payVerified = true) }
    }

    /** Pay 계좌 개설. */
    fun createAccount(type: String, bankCode: String, alias: String? = null) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val response = payRepository.createAccount(
                AccountCreateRequest(accountType = type, bankCode = bankCode, alias = alias)
            )
            if (response.status == "success" && response.data != null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        payComplete = true,
                        hasPayAccount = true,
                        accounts = it.accounts + response.data
                    )
                }
            } else {
                _uiState.update { it.copy(loading = false, error = response.message.ifBlank { "계좌 개설에 실패했습니다." }) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: "네트워크 오류가 발생했습니다.") }
        }
    }

    /**
     * Pay 가입 완료 처리.
     * 1) 결제 비밀번호 저장(POST /api/pay/pin) → 2) Pay 계좌 개설.
     * 두 단계가 모두 성공해야 payComplete = true.
     */
    fun completePaySignup(pin: String, type: String = "PERSONAL", bankCode: String = "001") = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            // 1) 결제 비밀번호 저장
            val pinResponse = payRepository.setPayPin(pin)
            if (pinResponse.status != "success") {
                _uiState.update { it.copy(loading = false, error = pinResponse.message.ifBlank { "결제 비밀번호 설정에 실패했습니다." }) }
                return@launch
            }

            // 2) Pay 계좌 개설
            val accountResponse = payRepository.createAccount(
                AccountCreateRequest(accountType = type, bankCode = bankCode, alias = null)
            )
            if (accountResponse.status == "success" && accountResponse.data != null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        payVerified = true,
                        payComplete = true,
                        hasPayAccount = true,
                        accounts = it.accounts + accountResponse.data
                    )
                }
            } else {
                _uiState.update { it.copy(loading = false, error = accountResponse.message.ifBlank { "계좌 개설에 실패했습니다." }) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: "네트워크 오류가 발생했습니다.") }
        }
    }

    /**
     * 결제 비밀번호 검증(POST /api/pay/pin/verify). 결제 승인 등 재인증 지점에서 사용.
     * 검증 성공 시 [onSuccess]를 호출해 후속 동작(예: 결제 승인)을 이어붙일 수 있다.
     */
    fun verifyPayPin(pin: String, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val response = payRepository.verifyPayPin(pin)
            if (response.status == "success") {
                _uiState.update { it.copy(loading = false, payVerified = true) }
                onSuccess()
            } else {
                _uiState.update { it.copy(loading = false, error = response.message.ifBlank { "결제 비밀번호가 올바르지 않습니다." }) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: "네트워크 오류가 발생했습니다.") }
        }
    }

    /** 결제 비밀번호 설정 여부 조회(GET /api/pay/pin/status). */
    fun checkPayPinStatus() = viewModelScope.launch {
        try {
            val response = payRepository.getPayPinStatus()
            if (response.status == "success" && response.data != null) {
                _uiState.update { it.copy(payPinRegistered = response.data.registered) }
            }
        } catch (_: Exception) {
            // 상태 조회 실패는 화면 진입을 막지 않는다.
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
