package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.payment.MerchantResponse
import com.ssafy.seveniTax.data.model.payment.QrTokenCreateRequest
import com.ssafy.seveniTax.data.remote.PaymentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val qrToken: String = "",
    val paymentId: Long? = null,
    val amount: Long = 0,
    val payerName: String = "",
    val paymentComplete: Boolean = false,
    val paymentStatus: String = "",
    val merchants: List<MerchantResponse> = emptyList(),
    val selectedMerchant: MerchantResponse? = null,
    val merchantsLoading: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentApi: PaymentApi
) : ViewModel() {

    companion object {
        private const val TAG = "PaymentVM"
    }

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun loadMerchants() = viewModelScope.launch {
        if (_uiState.value.merchants.isNotEmpty()) return@launch
        Log.d(TAG, "▶ loadMerchants()")
        _uiState.update { it.copy(merchantsLoading = true) }
        try {
            val response = paymentApi.getMerchants()
            val body = response.body()
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                val merchants = body.data
                Log.d(TAG, "  가맹점 ${merchants.size}개 로드 완료")
                _uiState.update {
                    it.copy(
                        merchantsLoading = false,
                        merchants = merchants,
                        selectedMerchant = merchants.firstOrNull()
                    )
                }
            } else {
                Log.e(TAG, "  가맹점 목록 조회 실패: ${body?.message}")
                _uiState.update { it.copy(merchantsLoading = false) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  가맹점 목록 조회 에러: ${e.message}", e)
            _uiState.update { it.copy(merchantsLoading = false) }
        }
    }

    fun selectMerchant(merchant: MerchantResponse) {
        _uiState.update { it.copy(selectedMerchant = merchant) }
    }

    fun createQrToken(cardId: Long, amount: Long, merchantId: Long, merchantName: String) = viewModelScope.launch {
        Log.d(TAG, "▶ createQrToken() cardId=$cardId, amount=$amount, merchantId=$merchantId, merchant=$merchantName")
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val request = QrTokenCreateRequest(
                cardId = cardId,
                amount = amount,
                merchantId = merchantId,
                merchantName = merchantName,
                purpose = "BUSINESS"
            )
            Log.d(TAG, "  요청: $request")
            val response = paymentApi.createQrToken(request)
            val code = response.code()
            val body = response.body()
            val errorBody = if (!response.isSuccessful) response.errorBody()?.string() else null
            Log.d(TAG, "  응답: code=$code, status=${body?.status}, token=${body?.data?.token}")
            if (errorBody != null) Log.e(TAG, "  errorBody=$errorBody")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        qrToken = body.data.token,
                        paymentId = body.data.paymentId,
                        amount = body.data.amount,
                        payerName = body.data.payerName
                    )
                }
            } else {
                val errMsg = body?.message ?: "QR 토큰 생성 실패 (${response.code()})"
                Log.e(TAG, "  createQrToken 실패: $errMsg")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = errMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  createQrToken 에러: ${e.message}", e)
            _uiState.update {
                it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류")
            }
        }
    }

    fun checkPaymentStatus(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ checkPaymentStatus() token=$token")
        try {
            val response = paymentApi.getQrPaymentStatus(token)
            val body = response.body()
            Log.d(TAG, "  상태: ${body?.data?.status}")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        paymentStatus = body.data.status,
                        paymentComplete = body.data.status == "CAPTURED"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  checkPaymentStatus 에러: ${e.message}", e)
        }
    }

    fun lookupQrPayment(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ lookupQrPayment() token=$token")
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val response = paymentApi.getQrPaymentInfo(token)
            val code = response.code()
            val body = response.body()
            val errorBody = if (!response.isSuccessful) response.errorBody()?.string() else null
            Log.d(TAG, "  조회 응답: code=$code, data=${body?.data}")
            if (errorBody != null) Log.e(TAG, "  errorBody=$errorBody")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        qrToken = token,
                        amount = body.data.amount,
                        payerName = body.data.merchantName
                    )
                }
            } else {
                Log.e(TAG, "  조회 실패: ${body?.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = body?.message ?: "결제 정보 조회 실패") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  조회 에러: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
    }

    fun confirmQrPayment(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ confirmQrPayment() token=$token")
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val response = paymentApi.confirmQrPayment(token)
            val body = response.body()
            val errorBody = response.errorBody()?.string()
            Log.d(TAG, "  결제 응답: code=${response.code()}, body=${body}, errorBody=$errorBody")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        paymentComplete = true,
                        paymentStatus = body.data.status
                    )
                }
            } else {
                val errMsg = body?.message ?: errorBody ?: "결제 승인 실패 (${response.code()})"
                Log.e(TAG, "  결제 실패: $errMsg")
                _uiState.update { it.copy(isLoading = false, errorMessage = errMsg) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  결제 에러: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
    }

    fun startPollingStatus(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ startPollingStatus() token=$token")
        var failCount = 0
        while (true) {
            kotlinx.coroutines.delay(2000L)
            try {
                val response = paymentApi.getQrPaymentStatus(token)
                val code = response.code()
                val body = response.body()

                if (response.isSuccessful && body?.status == "success" && body.data != null) {
                    failCount = 0
                    val status = body.data.status
                    Log.d(TAG, "  폴링 상태: $status")
                    if (status == "CAPTURED" || status == "CANCELLED") {
                        _uiState.update {
                            it.copy(
                                paymentStatus = status,
                                paymentComplete = status == "CAPTURED"
                            )
                        }
                        break
                    }
                } else if (code == 410 || code == 404) {
                    // 토큰 만료 또는 없음 → 폴링 중단
                    Log.w(TAG, "  폴링 중단: HTTP $code (토큰 만료)")
                    _uiState.update {
                        it.copy(paymentStatus = "EXPIRED", errorMessage = "QR 토큰이 만료되었습니다.")
                    }
                    break
                } else {
                    failCount++
                    Log.w(TAG, "  폴링 실패: HTTP $code (${failCount}회)")
                    if (failCount >= 5) {
                        Log.e(TAG, "  폴링 중단: 연속 실패 $failCount 회")
                        _uiState.update { it.copy(errorMessage = "결제 상태 확인 실패") }
                        break
                    }
                }
            } catch (e: Exception) {
                failCount++
                Log.e(TAG, "  폴링 에러 (${failCount}회): ${e.message}")
                if (failCount >= 5) {
                    Log.e(TAG, "  폴링 중단: 연속 에러 $failCount 회")
                    _uiState.update { it.copy(errorMessage = "네트워크 오류") }
                    break
                }
            }
        }
    }

    fun resetPayment() {
        _uiState.update { PaymentUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
