package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.book.BookEntryResponse
import com.ssafy.seveniTax.data.repository.BookEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import com.ssafy.seveniTax.data.model.book.BookEntryRequest
import com.ssafy.seveniTax.data.model.book.CategoryUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EntryFilter(val label: String) {
    ALL("전체"), INCOME("수입"), EXPENSE("비용"), ASSET("자산")
}

enum class BookTab(val label: String) {
    LIST("장부 목록"), CATEGORY("세목별")
}

@HiltViewModel
class BookEntryViewModel @Inject constructor(
    private val bookEntryRepository: BookEntryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<BookEntryResponse>>(emptyList())
    val entries: StateFlow<List<BookEntryResponse>> = _entries.asStateFlow()

    private val _unconfirmedCount = MutableStateFlow(0)
    val unconfirmedCount: StateFlow<Int> = _unconfirmedCount.asStateFlow()

    private val _selectedYear = MutableStateFlow(java.time.LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(java.time.LocalDate.now().monthValue)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedFilter = MutableStateFlow(EntryFilter.ALL)
    val selectedFilter: StateFlow<EntryFilter> = _selectedFilter.asStateFlow()

    private val _selectedTab = MutableStateFlow(BookTab.LIST)
    val selectedTab: StateFlow<BookTab> = _selectedTab.asStateFlow()

    // 필터 설정
    private val _filterCategories = MutableStateFlow(setOf("전체"))
    val filterCategories: StateFlow<Set<String>> = _filterCategories.asStateFlow()

    private val _filterMerchant = MutableStateFlow("")
    val filterMerchant: StateFlow<String> = _filterMerchant.asStateFlow()

    private val _filterOnlyWithReceipt = MutableStateFlow(false)
    val filterOnlyWithReceipt: StateFlow<Boolean> = _filterOnlyWithReceipt.asStateFlow()

    private val _filterOnlyUnclassified = MutableStateFlow(false)
    val filterOnlyUnclassified: StateFlow<Boolean> = _filterOnlyUnclassified.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _bulkConfirmDone = MutableStateFlow(false)
    val bulkConfirmDone: StateFlow<Boolean> = _bulkConfirmDone.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadMockEntries() {
        _entries.value = listOf(
            BookEntryResponse(
                id = 1, entryDate = "2025-03-22", merchantName = "스타벅스 강남점",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 5500,
                fixedAssetAmount = 0, vatAmount = 500, supplyPrice = 5000,
                categoryCode = "welfare", categoryName = "복리후생비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "팀 미팅 커피", createdAt = "2025-03-22T14:23:00"
            ),
            BookEntryResponse(
                id = 2, entryDate = "2025-03-21", merchantName = "카카오T 택시",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 18400,
                fixedAssetAmount = 0, vatAmount = 1673, supplyPrice = 16727,
                categoryCode = "transport", categoryName = "여비교통비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, createdAt = "2025-03-21T22:31:00"
            ),
            BookEntryResponse(
                id = 3, entryDate = "2025-03-20", merchantName = "클라이언트A",
                description = "3월 용역 대금",
                entryType = "INCOME", incomeAmount = 3300000, expenseAmount = 0,
                fixedAssetAmount = 0, vatAmount = 300000, supplyPrice = 3000000,
                categoryCode = "sales", categoryName = "매출",
                isBusinessExpense = false, isVatDeductible = false,
                confirmed = true, createdAt = "2025-03-20T10:00:00"
            ),
            BookEntryResponse(
                id = 4, entryDate = "2025-03-19", merchantName = "GS25 역삼점",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 3200,
                fixedAssetAmount = 0, vatAmount = 291, supplyPrice = 2909,
                categoryCode = "supplies", categoryName = "소모품비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = false, createdAt = "2025-03-19T12:05:00"
            ),
            BookEntryResponse(
                id = 5, entryDate = "2025-03-18", merchantName = "교보문고",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 32000,
                fixedAssetAmount = 0, vatAmount = 2909, supplyPrice = 29091,
                categoryCode = "books", categoryName = "도서인쇄비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "개발 서적 구매", createdAt = "2025-03-18T15:40:00"
            ),
            BookEntryResponse(
                id = 6, entryDate = "2025-03-17", merchantName = "클라이언트B",
                description = "디자인 작업비",
                entryType = "INCOME", incomeAmount = 1100000, expenseAmount = 0,
                fixedAssetAmount = 0, vatAmount = 100000, supplyPrice = 1000000,
                categoryCode = "sales", categoryName = "매출",
                isBusinessExpense = false, isVatDeductible = false,
                confirmed = true, createdAt = "2025-03-17T09:00:00"
            ),
            BookEntryResponse(
                id = 7, entryDate = "2025-03-15", merchantName = "거래처 식사",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 85000,
                fixedAssetAmount = 0, vatAmount = 7727, supplyPrice = 77273,
                categoryCode = "entertain", categoryName = "접대비",
                isBusinessExpense = true, isVatDeductible = false,
                confirmed = true, note = "거래처 미팅 식사", createdAt = "2025-03-15T19:30:00"
            ),
            BookEntryResponse(
                id = 8, entryDate = "2025-03-14", merchantName = "애플코리아",
                entryType = "ASSET", incomeAmount = 0, expenseAmount = 0,
                fixedAssetAmount = 1990000, vatAmount = 180909, supplyPrice = 1809091,
                categoryCode = "asset", categoryName = "비품",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "업무용 맥북", createdAt = "2025-03-14T11:00:00"
            ),
            BookEntryResponse(
                id = 9, entryDate = "2025-03-12", merchantName = "KT",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 55000,
                fixedAssetAmount = 0, vatAmount = 5000, supplyPrice = 50000,
                categoryCode = "comm", categoryName = "통신비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = false, createdAt = "2025-03-12T00:00:00"
            ),
            BookEntryResponse(
                id = 10, entryDate = "2025-03-10", merchantName = "네이버클라우드",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 110000,
                fixedAssetAmount = 0, vatAmount = 10000, supplyPrice = 100000,
                categoryCode = "fee", categoryName = "지급수수료",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "서버 호스팅비", createdAt = "2025-03-10T00:00:00"
            )
        )
    }

    fun loadEntries() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("BookEntryVM", "▶ loadEntries() 호출")
                val response = bookEntryRepository.getEntries(confirmed = null, page = 0, size = 200)
                val code = response.code()
                val body = response.body()
                val errorBody = if (!response.isSuccessful) response.errorBody()?.string() else null

                Log.d("BookEntryVM", "  HTTP $code | status=${body?.status} | data=${body?.data != null}")
                if (errorBody != null) Log.e("BookEntryVM", "  errorBody=$errorBody")

                if (response.isSuccessful && body?.status == "success") {
                    val entries = body.data?.content ?: emptyList()
                    Log.d("BookEntryVM", "  entries=${entries.size}건")
                    entries.take(3).forEach { e ->
                        Log.d("BookEntryVM", "    [${e.id}] ${e.entryDate} ${e.merchantName} ${e.entryType} income=${e.incomeAmount} expense=${e.expenseAmount}")
                    }
                    _entries.value = entries

                    // 최신 데이터의 연/월로 자동 설정
                    val latestDate = entries.mapNotNull {
                        try { java.time.LocalDate.parse(it.entryDate) } catch (_: Exception) { null }
                    }.maxOrNull()
                    if (latestDate != null) {
                        _selectedYear.value = latestDate.year
                        _selectedMonth.value = latestDate.monthValue
                        Log.d("BookEntryVM", "  자동 설정: ${latestDate.year}년 ${latestDate.monthValue}월")
                    }
                } else {
                    val errMsg = body?.message ?: errorBody?.take(200) ?: "데이터를 불러올 수 없습니다 (HTTP $code)"
                    Log.e("BookEntryVM", "  실패: $errMsg")
                    _error.value = errMsg
                }
            } catch (e: Exception) {
                Log.e("BookEntryVM", "  에러: ${e.message}", e)
                _error.value = "네트워크 오류가 발생했습니다"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUnconfirmedCount() {
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.getUnconfirmedCount()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _unconfirmedCount.value = response.body()?.data ?: 0
                }
            } catch (_: Exception) { }
        }
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun selectFilter(filter: EntryFilter) {
        _selectedFilter.value = filter
    }

    fun selectTab(tab: BookTab) {
        _selectedTab.value = tab
    }

    fun applyFilter(categories: Set<String>, merchant: String, onlyReceipt: Boolean, onlyUnclassified: Boolean) {
        _filterCategories.value = categories
        _filterMerchant.value = merchant
        _filterOnlyWithReceipt.value = onlyReceipt
        _filterOnlyUnclassified.value = onlyUnclassified
    }

    private val categoryNameToCode = mapOf(
        "복리후생비" to "welfare",
        "접대비" to "entertain",
        "여비교통비" to "transport",
        "소모품비" to "supplies",
        "도서인쇄비" to "books",
        "통신비" to "comm",
        "지급수수료" to "fee",
        "비품" to "asset",
        "매출" to "sales",
        "감가상각비" to "depreciation",
        "광고선전비" to "advertising",
        "교육훈련비" to "education",
        "보험료" to "insurance",
        "세금과공과" to "tax",
        "수선비" to "repair",
        "수도광열비" to "utility",
        "운반비" to "delivery",
        "임차료" to "rent",
        "차량유지비" to "vehicle"
    )

    fun updateEntryCategory(entryId: Long, newCategory: String) {
        val categoryCode = categoryNameToCode[newCategory] ?: newCategory.lowercase()
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.updateCategory(
                    entryId,
                    CategoryUpdateRequest(categoryCode = categoryCode, categoryName = newCategory)
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    Log.d("BookEntryVM", "경비 변경 성공: $entryId → $newCategory")
                    // 카테고리 변경 후 확정 처리
                    confirmEntry(entryId)
                    loadEntries()
                } else {
                    val errMsg = response.body()?.message ?: "경비 변경 실패 (${response.code()})"
                    Log.e("BookEntryVM", "경비 변경 실패: $errMsg")
                    _error.value = errMsg
                }
            } catch (e: Exception) {
                Log.e("BookEntryVM", "경비 변경 에러: ${e.message}", e)
                _error.value = "네트워크 오류가 발생했습니다"
            }
        }
    }

    fun confirmEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.confirmEntry(entryId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    Log.d("BookEntryVM", "경비 확정 성공: $entryId")
                    loadEntries()
                } else {
                    Log.e("BookEntryVM", "경비 확정 실패: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e("BookEntryVM", "경비 확정 에러: ${e.message}", e)
            }
        }
    }

    /**
     * 일괄 확정: 카테고리 맵에 있는 항목은 카테고리 변경 + 확정, 없는 항목은 확정만.
     * 모든 API 호출 완료 후 bulkConfirmDone = true 로 변경.
     */
    fun bulkConfirmEntries(categoryMap: Map<Long, String>, entryIds: List<Long>) {
        _bulkConfirmDone.value = false
        viewModelScope.launch {
            for (id in entryIds) {
                val category = categoryMap[id]
                try {
                    if (category != null && category != "미분류") {
                        val code = categoryNameToCode[category] ?: category.lowercase()
                        val resp = bookEntryRepository.updateCategory(
                            id, CategoryUpdateRequest(categoryCode = code, categoryName = category)
                        )
                        if (resp.isSuccessful && resp.body()?.status == "success") {
                            Log.d("BookEntryVM", "일괄 경비 변경 성공: $id → $category")
                        } else {
                            Log.e("BookEntryVM", "일괄 경비 변경 실패: $id ${resp.body()?.message}")
                        }
                    }
                    val confirmResp = bookEntryRepository.confirmEntry(id)
                    if (confirmResp.isSuccessful && confirmResp.body()?.status == "success") {
                        Log.d("BookEntryVM", "일괄 확정 성공: $id")
                    } else {
                        Log.e("BookEntryVM", "일괄 확정 실패: $id ${confirmResp.body()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("BookEntryVM", "일괄 확정 에러: $id ${e.message}", e)
                }
            }
            loadEntries()
            loadUnconfirmedCount()
            _bulkConfirmDone.value = true
        }
    }

    fun clearBulkConfirmDone() {
        _bulkConfirmDone.value = false
    }

    fun createTestEntries() {
        val today = java.time.LocalDate.now().toString()
        val testData = listOf(
            BookEntryRequest(entryDate = today, merchantName = "스타벅스 강남점", entryType = "EXPENSE", amount = 5500, description = "커피"),
            BookEntryRequest(entryDate = today, merchantName = "카카오T 택시", entryType = "EXPENSE", amount = 18400, description = "택시비"),
            BookEntryRequest(entryDate = today, merchantName = "쿠팡", entryType = "EXPENSE", amount = 32000, description = "사무용품"),
            BookEntryRequest(entryDate = today, merchantName = "GS25 역삼점", entryType = "EXPENSE", amount = 4800, description = "간식"),
            BookEntryRequest(entryDate = today, merchantName = "네이버클라우드", entryType = "EXPENSE", amount = 110000, description = "서버 호스팅"),
        )
        viewModelScope.launch {
            testData.forEach { req ->
                try {
                    val response = bookEntryRepository.createEntry(req)
                    if (response.isSuccessful) {
                        Log.d("BookEntryVM", "테스트 데이터 생성: ${req.merchantName}")
                    } else {
                        Log.e("BookEntryVM", "테스트 데이터 실패: ${req.merchantName} (${response.code()})")
                    }
                } catch (e: Exception) {
                    Log.e("BookEntryVM", "테스트 데이터 에러: ${e.message}")
                }
            }
            loadEntries()
            loadUnconfirmedCount()
        }
    }

    fun updateNote(entryId: Long, note: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.updateNote(
                    entryId,
                    com.ssafy.seveniTax.data.model.book.NoteUpdateRequest(note)
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    Log.d("BookEntryVM", "메모 저장 성공: entryId=$entryId")
                    onResult(true)
                } else {
                    Log.e("BookEntryVM", "메모 저장 실패: ${response.body()?.message}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("BookEntryVM", "메모 저장 에러: ${e.message}", e)
                onResult(false)
            }
        }
    }

    fun resetFilter() {
        _filterCategories.value = setOf("전체")
        _filterMerchant.value = ""
        _filterOnlyWithReceipt.value = false
        _filterOnlyUnclassified.value = false
    }

    private fun getEntriesForMonth(): List<BookEntryResponse> {
        val year = _selectedYear.value
        val month = _selectedMonth.value
        return _entries.value.filter {
            try {
                val date = java.time.LocalDate.parse(it.entryDate)
                date.year == year && date.monthValue == month
            } catch (_: Exception) { false }
        }
    }

    fun getFilteredEntries(): List<BookEntryResponse> {
        var result = getEntriesForMonth()

        // 탭 필터 (전체/수입/비용/자산)
        result = when (_selectedFilter.value) {
            EntryFilter.ALL -> result
            EntryFilter.INCOME -> result.filter { it.entryType == "INCOME" }
            EntryFilter.EXPENSE -> result.filter { it.entryType == "EXPENSE" }
            EntryFilter.ASSET -> result.filter { it.entryType == "ASSET" }
        }

        // 계정과목 필터
        val cats = _filterCategories.value
        if (!cats.contains("전체")) {
            result = result.filter { it.categoryName in cats }
        }

        // 거래처 검색
        val merchant = _filterMerchant.value
        if (merchant.isNotBlank()) {
            result = result.filter {
                (it.merchantName ?: "").contains(merchant, ignoreCase = true) ||
                (it.description ?: "").contains(merchant, ignoreCase = true)
            }
        }

        // 미분류 거래만
        if (_filterOnlyUnclassified.value) {
            result = result.filter { !it.confirmed }
        }

        return result
    }

    fun getTotalIncome(): Long = getEntriesForMonth().filter { it.entryType == "INCOME" }.sumOf { it.incomeAmount }
    fun getTotalExpense(): Long = getEntriesForMonth().filter { it.entryType == "EXPENSE" }.sumOf { it.expenseAmount }
    fun getTotalAsset(): Long = getEntriesForMonth().filter { it.entryType == "ASSET" }.sumOf { it.fixedAssetAmount }
    fun getNetProfit(): Long = getTotalIncome() - getTotalExpense()

    // 세목별 비용 집계
    fun getExpenseByCategory(): List<Pair<String, Long>> {
        return getEntriesForMonth()
            .filter { it.entryType == "EXPENSE" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.expenseAmount } }
            .sortedByDescending { it.second }
    }

    // 세목별 수입 집계
    fun getIncomeByCategory(): List<Pair<String, Long>> {
        return getEntriesForMonth()
            .filter { it.entryType == "INCOME" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.incomeAmount } }
            .sortedByDescending { it.second }
    }

    // ── 리포트용: 특정 년/월 기준 조회 ──

    private fun getEntriesFor(year: Int, month: Int): List<BookEntryResponse> {
        return _entries.value.filter {
            try {
                val date = java.time.LocalDate.parse(it.entryDate)
                date.year == year && date.monthValue == month
            } catch (_: Exception) { false }
        }
    }

    fun getIncomeFor(year: Int, month: Int): Long = getEntriesFor(year, month).filter { it.entryType == "INCOME" }.sumOf { it.incomeAmount }
    fun getExpenseFor(year: Int, month: Int): Long = getEntriesFor(year, month).filter { it.entryType == "EXPENSE" }.sumOf { it.expenseAmount }

    fun getExpenseByCategoryFor(year: Int, month: Int): List<Pair<String, Long>> {
        return getEntriesFor(year, month)
            .filter { it.entryType == "EXPENSE" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.expenseAmount } }
            .sortedByDescending { it.second }
    }

    fun getIncomeByMerchantFor(year: Int, month: Int): List<Pair<String, Long>> {
        return getEntriesFor(year, month)
            .filter { it.entryType == "INCOME" }
            .groupBy { it.merchantName ?: it.description ?: "기타" }
            .map { (name, entries) -> name to entries.sumOf { it.incomeAmount } }
            .sortedByDescending { it.second }
    }

    fun getMonthlyTrend(year: Int, month: Int): List<Triple<String, Long, Long>> {
        return (5 downTo 0).map { offset ->
            var m = month - offset
            var y = year
            while (m <= 0) { m += 12; y-- }
            val label = "${m}월"
            Triple(label, getIncomeFor(y, m), getExpenseFor(y, m))
        }
    }

    // ── 연간 집계 ──

    private fun getEntriesForYear(year: Int): List<BookEntryResponse> {
        return _entries.value.filter {
            try { java.time.LocalDate.parse(it.entryDate).year == year }
            catch (_: Exception) { false }
        }
    }

    fun getAnnualIncome(year: Int): Long = getEntriesForYear(year).filter { it.entryType == "INCOME" }.sumOf { it.incomeAmount }
    fun getAnnualExpense(year: Int): Long = getEntriesForYear(year).filter { it.entryType == "EXPENSE" }.sumOf { it.expenseAmount }

    fun getAnnualExpenseByCategory(year: Int): List<Pair<String, Long>> {
        return getEntriesForYear(year)
            .filter { it.entryType == "EXPENSE" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.expenseAmount } }
            .sortedByDescending { it.second }
    }

    fun getAnnualIncomeByMerchant(year: Int): List<Pair<String, Long>> {
        return getEntriesForYear(year)
            .filter { it.entryType == "INCOME" }
            .groupBy { it.merchantName ?: it.description ?: "기타" }
            .map { (name, entries) -> name to entries.sumOf { it.incomeAmount } }
            .sortedByDescending { it.second }
    }

    fun getAnnualMonthlyTrend(year: Int): List<Triple<String, Long, Long>> {
        return (1..12).map { m ->
            Triple("${m}월", getIncomeFor(year, m), getExpenseFor(year, m))
        }
    }
}
