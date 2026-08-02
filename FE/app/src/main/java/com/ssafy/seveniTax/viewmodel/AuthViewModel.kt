package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.auth.TermItem
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityRequest
import com.ssafy.seveniTax.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AutoLoginResult { NONE, SUCCESS, FAILURE }

enum class AuthStep {
    PHONE, SSN, TELECOM, NAME, TERMS, SMS
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val currentStep: AuthStep = AuthStep.PHONE,
    val phone: String = "",
    val residentFront: String = "",
    val residentBack: String = "",
    val telecom: String = "",
    val name: String = "",
    val smsCode: String = "",
    val userId: Long = 0L,
    val verifyToken: String = "",
    val requiresPinSetup: Boolean = false,
    val terms: List<TermItem> = emptyList(),
    val agreedTermIds: Set<String> = emptySet(),
    val allRequiredTermsAgreed: Boolean = false,
    val pin: String = "",
    val pinConfirm: String = "",
    val pinFailCount: Int = 0,
    val loginPin: String = "",
    val pinNotSet: Boolean = false,
    val authComplete: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _autoLoginResult = MutableStateFlow(AutoLoginResult.NONE)
    val autoLoginResult: StateFlow<AutoLoginResult> = _autoLoginResult.asStateFlow()

    fun checkAutoLogin() = viewModelScope.launch {
        delay(1500)
        _autoLoginResult.value =
            if (authRepository.hasStoredCredentials()) AutoLoginResult.SUCCESS else AutoLoginResult.FAILURE
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = "") }
    }

    fun updateResidentFront(value: String) {
        _uiState.update { it.copy(residentFront = value, errorMessage = "") }
    }

    fun updateResidentBack(value: String) {
        _uiState.update { it.copy(residentBack = value, errorMessage = "") }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = "") }
    }

    fun updateTelecom(telecom: String) {
        _uiState.update { it.copy(telecom = telecom, errorMessage = "") }
    }

    fun updateSmsCode(code: String) {
        _uiState.update { it.copy(smsCode = code, errorMessage = "") }
    }

    fun nextStep() {
        _uiState.update { state ->
            val next = when (state.currentStep) {
                AuthStep.PHONE -> AuthStep.SSN
                AuthStep.SSN -> AuthStep.TELECOM
                AuthStep.TELECOM -> AuthStep.NAME
                AuthStep.NAME -> AuthStep.TERMS
                AuthStep.TERMS -> AuthStep.SMS
                AuthStep.SMS -> AuthStep.SMS
            }
            state.copy(currentStep = next, errorMessage = "")
        }
    }

    fun goToStep(step: AuthStep) {
        _uiState.update { it.copy(currentStep = step, errorMessage = "") }
    }

    fun previousStep() {
        _uiState.update { state ->
            val prev = when (state.currentStep) {
                AuthStep.PHONE -> AuthStep.PHONE
                AuthStep.SSN -> AuthStep.PHONE
                AuthStep.TELECOM -> AuthStep.SSN
                AuthStep.NAME -> AuthStep.TELECOM
                AuthStep.TERMS -> AuthStep.NAME
                AuthStep.SMS -> AuthStep.TERMS
            }
            state.copy(currentStep = prev, errorMessage = "")
        }
    }

    fun loadTerms() {
        _uiState.update {
            it.copy(
                terms = listOf(
                    TermItem("SERVICE", "서비스 이용약관 동의", true),
                    TermItem("PRIVACY", "개인정보 수집 및 이용 동의", true),
                    TermItem("FINANCIAL", "전자금융거래 이용약관 동의", true)
                )
            )
        }
    }

    fun setAgreedTerms(consentTypes: Set<String>) {
        _uiState.update { state ->
            val allRequired = state.terms
                .filter { it.required }
                .all { consentTypes.contains(it.consentType) }

            state.copy(
                agreedTermIds = consentTypes,
                allRequiredTermsAgreed = allRequired
            )
        }
    }

    fun verifyIdentity(onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }

        try {
            val state = _uiState.value
            val rawPhone = state.phone.replace("-", "")
            val request = VerifyIdentityRequest(
                name = state.name.trim(),
                birthDate = buildBirthDate(state.residentFront, state.residentBack),
                gender = buildGender(state.residentBack),
                phoneNumber = rawPhone
            )
            val response = authRepository.verifyIdentity(request)
            val data = response.data ?: throw IllegalStateException("본인인증 응답이 비어 있습니다.")

            authRepository.saveUserInfo(data.userId, rawPhone, state.name.trim())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userId = data.userId,
                    verifyToken = data.verifyToken,
                    requiresPinSetup = data.requiresPinSetup
                )
            }
            onSuccess()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "verifyIdentity failed", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "본인인증에 실패했습니다."
                )
            }
        }
    }

    fun appendPin(digit: String) {
        _uiState.update {
            if (it.pin.length < 6) it.copy(pin = it.pin + digit, errorMessage = "") else it
        }
    }

    fun deletePin() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1), errorMessage = "") }
    }

    fun appendPinConfirm(digit: String) {
        _uiState.update {
            if (it.pinConfirm.length < 6) it.copy(pinConfirm = it.pinConfirm + digit, errorMessage = "") else it
        }
    }

    fun deletePinConfirm() {
        _uiState.update { it.copy(pinConfirm = it.pinConfirm.dropLast(1), errorMessage = "") }
    }

    fun confirmPin(onSuccess: () -> Unit, onMismatch: () -> Unit, onResetRequired: () -> Unit) {
        val state = _uiState.value
        if (state.pin != state.pinConfirm) {
            val newCount = state.pinFailCount + 1
            if (newCount >= 3) {
                _uiState.update {
                    it.copy(
                        pin = "",
                        pinConfirm = "",
                        pinFailCount = 0,
                        errorMessage = "PIN 확인이 3회 실패했습니다. 처음부터 다시 진행해 주세요."
                    )
                }
                onResetRequired()
            } else {
                _uiState.update {
                    it.copy(
                        pinConfirm = "",
                        pinFailCount = newCount,
                        errorMessage = "입력한 PIN이 일치하지 않습니다."
                    )
                }
                onMismatch()
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pinFailCount = 0, errorMessage = "") }
            try {
                authRepository.setupPin(state.verifyToken, state.pin, state.agreedTermIds)
                _uiState.update { it.copy(isLoading = false, authComplete = true) }
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "setupPin failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "PIN 설정에 실패했습니다."
                    )
                }
            }
        }
    }

    fun resetPin() {
        _uiState.update { it.copy(pin = "", pinConfirm = "", pinFailCount = 0, errorMessage = "") }
    }

    fun appendLoginPin(digit: String) {
        _uiState.update {
            if (it.loginPin.length < 6) it.copy(loginPin = it.loginPin + digit, errorMessage = "") else it
        }
    }

    fun deleteLoginPin() {
        _uiState.update { it.copy(loginPin = it.loginPin.dropLast(1), errorMessage = "") }
    }

    fun loginWithPin(onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = "", pinNotSet = false) }
        try {
            val phoneNumber = authRepository.getStoredPhoneNumber()?.replace("-", "")
                ?: throw IllegalStateException("저장된 휴대폰 번호가 없습니다.")
            authRepository.login(phoneNumber, _uiState.value.loginPin)
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "loginWithPin failed", e)
            val isPinNotSet = e.message?.contains("PIN_NOT_SET") == true
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loginPin = "",
                    pinNotSet = isPinNotSet,
                    errorMessage = if (isPinNotSet) {
                        "PIN이 설정되지 않았습니다"
                    } else {
                        e.message ?: "PIN이 올바르지 않습니다."
                    }
                )
            }
        }
    }

    fun resetForPinSetup() {
        authRepository.clearSession()
        _uiState.update { AuthUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }

    private fun buildBirthDate(residentFront: String, residentBack: String): String {
        val yy = residentFront.substring(0, 2)
        val mm = residentFront.substring(2, 4)
        val dd = residentFront.substring(4, 6)
        val century = when (residentBack) {
            "1", "2" -> "19"
            "3", "4" -> "20"
            else -> "19"
        }
        return "$century$yy-$mm-$dd"
    }

    private fun buildGender(residentBack: String): String {
        return when (residentBack) {
            "1", "3" -> "M"
            "2", "4" -> "F"
            else -> "M"
        }
    }
}
