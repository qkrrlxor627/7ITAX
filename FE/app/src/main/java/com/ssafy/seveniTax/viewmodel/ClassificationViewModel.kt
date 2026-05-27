package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.classification.ClassificationRequest
import com.ssafy.seveniTax.data.model.classification.ClassificationResponse
import com.ssafy.seveniTax.data.repository.ClassificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassificationUiState(
    val isLoading: Boolean = false,
    val result: ClassificationResponse? = null,
    val error: String? = null,
    // 분류 대상 정보
    val entryId: Long = -1L,
    val merchantName: String = "",
    val amount: Long = 0L,
    val dateTime: String = "",
    val note: String? = null,
    // 수동 선택한 카테고리
    val selectedCategory: String? = null
)

@HiltViewModel
class ClassificationViewModel @Inject constructor(
    private val classificationRepository: ClassificationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ClassificationVM"
    }

    private val _uiState = MutableStateFlow(ClassificationUiState())
    val uiState: StateFlow<ClassificationUiState> = _uiState.asStateFlow()

    // 일괄 분류 결과
    private val _bulkResults = MutableStateFlow<List<BulkClassificationItem>>(emptyList())
    val bulkResults: StateFlow<List<BulkClassificationItem>> = _bulkResults.asStateFlow()

    fun setEntryInfo(entryId: Long, merchantName: String, amount: Long, dateTime: String, note: String? = null) {
        _uiState.value = _uiState.value.copy(
            entryId = entryId,
            merchantName = merchantName,
            amount = amount,
            dateTime = dateTime,
            note = note
        )
    }

    fun setSelectedCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun classify(merchantName: String, amount: Long? = null, note: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            Log.d(TAG, "▶ classify() merchant=$merchantName, amount=$amount, note=$note")
            try {
                val request = ClassificationRequest(
                    merchantName = merchantName,
                    amount = amount,
                    note = note
                )
                val response = classificationRepository.classify(request)
                val body = response.body()
                if (response.isSuccessful && body?.status == "success" && body.data != null) {
                    Log.d(TAG, "  분류 결과: ${body.data.taxCategory} (${body.data.confidenceScore}%)")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = body.data
                    )
                } else {
                    val errMsg = body?.message ?: "분류 실패 (${response.code()})"
                    Log.e(TAG, "  분류 실패: $errMsg")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "  분류 에러: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "네트워크 오류"
                )
            }
        }
    }

    fun classifyBulk(entries: List<BulkEntryInput>) {
        viewModelScope.launch {
            val results = mutableListOf<BulkClassificationItem>()
            _bulkResults.value = entries.map {
                BulkClassificationItem(it.entryId, it.merchantName, it.amount, null, false)
            }

            entries.forEachIndexed { index, entry ->
                Log.d(TAG, "▶ classifyBulk [${index + 1}/${entries.size}] ${entry.merchantName}")
                try {
                    val request = ClassificationRequest(
                        merchantName = entry.merchantName,
                        amount = entry.amountLong,
                        note = entry.note
                    )
                    val response = classificationRepository.classify(request)
                    val body = response.body()
                    val category = if (response.isSuccessful && body?.status == "success") {
                        body.data?.taxCategory ?: "미분류"
                    } else "미분류"

                    results.add(BulkClassificationItem(
                        entryId = entry.entryId,
                        merchantName = entry.merchantName,
                        amount = entry.amount,
                        aiCategory = category,
                        isDone = true
                    ))
                } catch (_: Exception) {
                    results.add(BulkClassificationItem(
                        entryId = entry.entryId,
                        merchantName = entry.merchantName,
                        amount = entry.amount,
                        aiCategory = "미분류",
                        isDone = true
                    ))
                }

                // 진행 상태 업데이트
                _bulkResults.value = results.toList() + entries.drop(results.size).map {
                    BulkClassificationItem(it.entryId, it.merchantName, it.amount, null, false)
                }
            }
        }
    }

    fun updateBulkCategory(entryId: Long, newCategory: String) {
        _bulkResults.value = _bulkResults.value.map { item ->
            if (item.entryId == entryId) item.copy(aiCategory = newCategory, isDone = true)
            else item
        }
    }

    fun removeBulkEntry(entryId: Long) {
        _bulkResults.value = _bulkResults.value.filter { it.entryId != entryId }
    }

    fun reset() {
        _uiState.value = ClassificationUiState()
        _bulkResults.value = emptyList()
    }
}

data class BulkEntryInput(
    val entryId: Long,
    val merchantName: String,
    val amount: String,
    val amountLong: Long,
    val note: String? = null
)

data class BulkClassificationItem(
    val entryId: Long,
    val merchantName: String,
    val amount: String,
    val aiCategory: String?,
    val isDone: Boolean
)
