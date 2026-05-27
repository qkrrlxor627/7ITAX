package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.chatbot.ChatbotRequest
import com.ssafy.seveniTax.data.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val time: String = currentTime(),
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessionId: String? = null,
    val isSending: Boolean = false
)

private fun currentTime(): String {
    val now = java.util.Calendar.getInstance()
    val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = now.get(java.util.Calendar.MINUTE)
    val ampm = if (hour < 12) "오전" else "오후"
    val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return "$ampm ${h}:${minute.toString().padStart(2, '0')}"
}

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatbotVM"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // 초기 인사 메시지
        _uiState.value = ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = "안녕하세요!\n세무 관련 궁금한 점이 있으시면 편하게 물어보세요.",
                    isUser = false
                )
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isSending) return

        val userMsg = ChatMessage(text = text, isUser = true)
        val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true)

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + loadingMsg,
            isSending = true
        )

        viewModelScope.launch {
            try {
                val request = ChatbotRequest(
                    message = text,
                    sessionId = _uiState.value.sessionId
                )
                Log.d(TAG, "▶ sendMessage: $text, session=${request.sessionId}")

                val response = chatbotRepository.sendMessage(request)
                val code = response.code()
                val body = response.body()
                val errorBody = if (!response.isSuccessful) response.errorBody()?.string() else null

                Log.d(TAG, "  HTTP $code | success=${response.isSuccessful}")
                Log.d(TAG, "  body=$body")
                if (errorBody != null) Log.e(TAG, "  errorBody=$errorBody")

                // 로딩 메시지 제거
                val msgs = _uiState.value.messages.filter { !it.isLoading }

                if (response.isSuccessful && body?.data != null) {
                    val data = body.data
                    Log.d(TAG, "  응답: ${data.answer.take(100)}...")
                    Log.d(TAG, "  sessionId=${data.sessionId}, model=${data.model}")
                    _uiState.value = _uiState.value.copy(
                        messages = msgs + ChatMessage(text = data.answer, isUser = false),
                        sessionId = data.sessionId,
                        isSending = false
                    )
                } else {
                    // errorBody에서 errorCode 추출
                    val errorCode = try {
                        org.json.JSONObject(errorBody ?: "{}").optString("errorCode", "")
                    } catch (_: Exception) { "" }
                    val userMsg = toUserMessage(code, errorCode)
                    Log.e(TAG, "  실패: HTTP $code | errorCode=$errorCode | errorBody=$errorBody")
                    _uiState.value = _uiState.value.copy(
                        messages = msgs + ChatMessage(
                            text = userMsg,
                            isUser = false,
                            isError = true
                        ),
                        isSending = false
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "  DNS 에러: ${e.message}", e)
                val msgs = _uiState.value.messages.filter { !it.isLoading }
                _uiState.value = _uiState.value.copy(
                    messages = msgs + ChatMessage(
                        text = "인터넷 연결을 확인해주세요.",
                        isUser = false,
                        isError = true
                    ),
                    isSending = false
                )
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "  타임아웃: ${e.message}", e)
                val msgs = _uiState.value.messages.filter { !it.isLoading }
                _uiState.value = _uiState.value.copy(
                    messages = msgs + ChatMessage(
                        text = "서버 응답이 너무 오래 걸려요. 잠시 후 다시 시도해주세요.",
                        isUser = false,
                        isError = true
                    ),
                    isSending = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "  에러: ${e.javaClass.simpleName} ${e.message}", e)
                val msgs = _uiState.value.messages.filter { !it.isLoading }
                _uiState.value = _uiState.value.copy(
                    messages = msgs + ChatMessage(
                        text = "알 수 없는 오류가 발생했어요. 다시 시도해주세요.",
                        isUser = false,
                        isError = true
                    ),
                    isSending = false
                )
            }
        }
    }

    fun retry() {
        val msgs = _uiState.value.messages
        // 마지막 에러 메시지 제거 후, 마지막 유저 메시지 재전송
        val withoutError = msgs.filter { !it.isError }
        val lastUserMsg = withoutError.lastOrNull { it.isUser }
        if (lastUserMsg != null) {
            _uiState.value = _uiState.value.copy(messages = withoutError)
            sendMessage(lastUserMsg.text)
        }
    }

    private fun toUserMessage(httpCode: Int, errorCode: String): String = when {
        errorCode == "AI_SERVICE_UNAVAILABLE" -> "AI 서비스가 일시적으로 중단되었어요. 잠시 후 다시 시도해주세요."
        errorCode == "RATE_LIMIT_EXCEEDED" -> "요청이 너무 많아요. 잠시 후 다시 질문해주세요."
        errorCode == "INVALID_SESSION" -> "대화 세션이 만료되었어요. 새 대화를 시작해주세요."
        httpCode == 401 || httpCode == 403 -> "로그인이 필요합니다. 앱을 재시작해주세요."
        httpCode == 404 -> "요청한 서비스를 찾을 수 없어요."
        httpCode == 500 -> "서버에 문제가 발생했어요. 잠시 후 다시 시도해주세요."
        httpCode == 502 || httpCode == 503 || httpCode == 504 -> "서버가 응답하지 않아요. 잠시 후 다시 시도해주세요."
        else -> "오류가 발생했어요 (오류 코드: $httpCode). 다시 시도해주세요."
    }

    fun newSession() {
        _uiState.value = ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = "새 대화를 시작합니다.\n세무 관련 궁금한 점을 물어보세요!",
                    isUser = false
                )
            )
        )
    }
}
