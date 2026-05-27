package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.tax.TaxDeadline
import com.ssafy.seveniTax.data.repository.TaxRepository
import com.ssafy.seveniTax.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class TaxFilter(val label: String) {
    ALL("전체"),
    VAT("부가세"),
    INCOME("소득세"),
    LOCAL("지방세")
}

enum class TaxType {
    VAT, INCOME, LOCAL
}

@HiltViewModel
class TaxCalendarViewModel @Inject constructor(
    private val taxRepository: TaxRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _deadlines = MutableStateFlow<List<TaxDeadline>>(emptyList())
    val deadlines: StateFlow<List<TaxDeadline>> = _deadlines.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaxFilter.ALL)
    val selectedFilter: StateFlow<TaxFilter> = _selectedFilter.asStateFlow()

    // 리마인드 알림 마스터 토글
    private val _reminderEnabled = MutableStateFlow(true)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    // 리마인드 알림 설정
    private val _reminderD7 = MutableStateFlow(true)
    val reminderD7: StateFlow<Boolean> = _reminderD7.asStateFlow()

    private val _reminderD3 = MutableStateFlow(true)
    val reminderD3: StateFlow<Boolean> = _reminderD3.asStateFlow()

    private val _reminderD1 = MutableStateFlow(true)
    val reminderD1: StateFlow<Boolean> = _reminderD1.asStateFlow()

    private val _reminderDDay = MutableStateFlow(true)
    val reminderDDay: StateFlow<Boolean> = _reminderDDay.asStateFlow()

    fun setReminderEnabled(enabled: Boolean) {
        _reminderEnabled.value = enabled
        applyReminderSettings()
    }

    fun setReminderD7(enabled: Boolean) { _reminderD7.value = enabled }
    fun setReminderD3(enabled: Boolean) { _reminderD3.value = enabled }
    fun setReminderD1(enabled: Boolean) { _reminderD1.value = enabled }
    fun setReminderDDay(enabled: Boolean) { _reminderDDay.value = enabled }

    // 알림 받을 세금 유형
    private val _vatAlarmEnabled = MutableStateFlow(true)
    val vatAlarmEnabled: StateFlow<Boolean> = _vatAlarmEnabled.asStateFlow()

    private val _incomeAlarmEnabled = MutableStateFlow(true)
    val incomeAlarmEnabled: StateFlow<Boolean> = _incomeAlarmEnabled.asStateFlow()

    private val _localAlarmEnabled = MutableStateFlow(true)
    val localAlarmEnabled: StateFlow<Boolean> = _localAlarmEnabled.asStateFlow()

    fun setVatAlarmEnabled(enabled: Boolean) { _vatAlarmEnabled.value = enabled }
    fun setIncomeAlarmEnabled(enabled: Boolean) { _incomeAlarmEnabled.value = enabled }
    fun setLocalAlarmEnabled(enabled: Boolean) { _localAlarmEnabled.value = enabled }

    fun applyReminderSettings() {
        if (!_reminderEnabled.value) {
            reminderScheduler.cancelAllReminders(_deadlines.value)
            return
        }
        val filteredDeadlines = _deadlines.value.filter { deadline ->
            when (classifyTax(deadline)) {
                TaxType.VAT -> _vatAlarmEnabled.value
                TaxType.INCOME -> _incomeAlarmEnabled.value
                TaxType.LOCAL -> _localAlarmEnabled.value
            }
        }
        // 비활성화된 세금 유형의 알림은 취소
        val disabledDeadlines = _deadlines.value - filteredDeadlines.toSet()
        reminderScheduler.cancelAllReminders(disabledDeadlines)

        reminderScheduler.scheduleReminders(
            deadlines = filteredDeadlines,
            d7 = _reminderD7.value,
            d3 = _reminderD3.value,
            d1 = _reminderD1.value,
            dDay = _reminderDDay.value
        )
    }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 세금 추정 데이터
    private val _estimatedVat = MutableStateFlow(0L)
    val estimatedVat: StateFlow<Long> = _estimatedVat.asStateFlow()

    private val _estimatedIncomeTax = MutableStateFlow(0L)
    val estimatedIncomeTax: StateFlow<Long> = _estimatedIncomeTax.asStateFlow()

    private val _estimatedLocalTax = MutableStateFlow(0L)
    val estimatedLocalTax: StateFlow<Long> = _estimatedLocalTax.asStateFlow()

    init {
        loadDeadlines()
        loadTaxEstimation()
        applyReminderSettings()
    }

    private fun loadTaxEstimation() {
        viewModelScope.launch {
            try {
                val response = taxRepository.getEstimation(null)
                if (response.isSuccessful && response.body()?.status == "success") {
                    response.body()?.data?.let { data ->
                        _estimatedVat.value = data.estimatedVat
                        _estimatedIncomeTax.value = data.estimatedIncomeTax
                        _estimatedLocalTax.value = data.estimatedLocalTax
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadMockDeadlines() {
        val today = LocalDate.now()
        _deadlines.value = listOf(
            TaxDeadline(
                taxName = "부가가치세 1기 예정 신고 및 납부",
                description = "1~3월 매출·매입 부가세 예정신고 및 납부",
                deadline = "2026-04-25",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2026, 4, 25)).toInt()
            ),
            TaxDeadline(
                taxName = "종합소득세 확정 신고 및 납부",
                description = "2025년 귀속 종합소득세 확정신고 및 납부",
                deadline = "2026-05-31",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2026, 5, 31)).toInt()
            ),
            TaxDeadline(
                taxName = "지방소득세 신고 및 납부",
                description = "종합소득세 신고분 지방소득세 납부",
                deadline = "2026-05-31",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2026, 5, 31)).toInt()
            ),
            TaxDeadline(
                taxName = "부가가치세 1기 확정 신고 및 납부",
                description = "1~6월 매출·매입 부가세 확정신고 및 납부",
                deadline = "2026-07-25",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2026, 7, 25)).toInt()
            ),
            TaxDeadline(
                taxName = "부가가치세 2기 예정 신고 및 납부",
                description = "7~9월 매출·매입 부가세 예정신고 및 납부",
                deadline = "2026-10-25",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2026, 10, 25)).toInt()
            ),
            TaxDeadline(
                taxName = "종합소득세 중간예납",
                description = "종합소득세 중간예납 납부",
                deadline = "2026-11-30",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2026, 11, 30)).toInt()
            ),
            TaxDeadline(
                taxName = "부가가치세 2기 확정 신고 및 납부",
                description = "7~12월 매출·매입 부가세 확정신고 및 납부",
                deadline = "2027-01-25",
                dDay = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2027, 1, 25)).toInt()
            )
        )
    }

    fun loadDeadlines() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = taxRepository.getDeadlines()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _deadlines.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "데이터를 불러올 수 없습니다"
                }
            } catch (e: Exception) {
                _error.value = "네트워크 오류가 발생했습니다"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectFilter(filter: TaxFilter) {
        _selectedFilter.value = filter
    }

    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun getFilteredDeadlines(): List<TaxDeadline> {
        val all = _deadlines.value
        return when (_selectedFilter.value) {
            TaxFilter.ALL -> all
            TaxFilter.VAT -> all.filter { classifyTax(it) == TaxType.VAT }
            TaxFilter.INCOME -> all.filter { classifyTax(it) == TaxType.INCOME }
            TaxFilter.LOCAL -> all.filter { classifyTax(it) == TaxType.LOCAL }
        }
    }

    fun getDeadlinesForMonth(yearMonth: YearMonth): List<TaxDeadline> {
        return getFilteredDeadlines().filter { deadline ->
            try {
                val date = LocalDate.parse(deadline.deadline)
                YearMonth.from(date) == yearMonth
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getDeadlinesForDate(date: LocalDate): List<TaxDeadline> {
        return getFilteredDeadlines().filter { deadline ->
            try {
                LocalDate.parse(deadline.deadline) == date
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getMostUrgentDeadline(): TaxDeadline? {
        return _deadlines.value
            .filter { it.dDay >= 0 }
            .minByOrNull { it.dDay }
    }

    companion object {
        fun classifyTax(deadline: TaxDeadline): TaxType {
            val name = deadline.taxName
            return when {
                name.contains("지방") -> TaxType.LOCAL
                name.contains("부가") -> TaxType.VAT
                name.contains("소득") -> TaxType.INCOME
                else -> TaxType.INCOME
            }
        }
    }
}
