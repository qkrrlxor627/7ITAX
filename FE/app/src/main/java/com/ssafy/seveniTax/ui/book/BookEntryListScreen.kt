package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.data.model.book.BookEntryResponse
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel
import com.ssafy.seveniTax.viewmodel.BookTab
import com.ssafy.seveniTax.viewmodel.EntryFilter
import java.text.NumberFormat
import java.util.Locale

// ─── 색상 상수 ──────────────────────────────────────────

private val IncomeBg = Color(0xFFEDF8F6)
private val IncomeText = Color(0xFF7ABFB3)
private val IncomeValue = Color(0xFF3DBDA2)
private val ExpenseBg = Color(0xFFFFF0F3)
private val ExpenseText = Color(0xFFFF9DAE)
private val ExpenseValue = Color(0xFFE8475A)
private val AssetBg = Color(0xFFFFF8EE)
private val AssetText = Color(0xFFE0A44A)

// ─── 메인 화면 ──────────────────────────────────────────

@Composable
fun BookEntryListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: BookEntryViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val unconfirmedCount by viewModel.unconfirmedCount.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val filteredEntries = viewModel.getFilteredEntries()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        BookHeader(
            onBack = { navController.popBackStack() },
            onExport = { navController.navigate(Route.ExportPurpose.path) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 요약 카드
            key(selectedYear, selectedMonth) {
                SummaryCard(
                    netProfit = viewModel.getNetProfit(),
                    totalIncome = viewModel.getTotalIncome(),
                    totalExpense = viewModel.getTotalExpense(),
                    totalAsset = viewModel.getTotalAsset()
                )
            }

            // 미확인 배너
            if (unconfirmedCount > 0) {
                UnconfirmedBanner(
                    count = unconfirmedCount,
                    onClick = { navController.navigate(Route.UnclassifiedList.path) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 월 드롭다운 + 필터
            MonthFilterRow(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onYearSelected = { viewModel.selectYear(it) },
                onMonthSelected = { viewModel.selectMonth(it) },
                onFilterClick = { navController.navigate(Route.BookFilter.path) }
            )

            // 탭
            BookTabs(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == BookTab.LIST) {
                // 필터 칩
                FilterChips(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 거래 리스트
                if (filteredEntries.isEmpty()) {
                    Text(
                        text = "거래 내역이 없습니다",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    filteredEntries.forEach { entry ->
                        TransactionItem(
                            entry = entry,
                            onClick = { navController.navigate(Route.BookEntryDetail.create(entry.id)) }
                        )
                    }
                }
            } else {
                // 세목별 뷰
                key(selectedYear, selectedMonth) {
                    CategoryView(viewModel)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─── 헤더 ───────────────────────────────────────────────

@Composable
private fun BookHeader(
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "간편장부",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onExport) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = "내보내기",
                tint = TextPrimary
            )
        }
    }
}

// ─── 요약 카드 ──────────────────────────────────────────

@Composable
private fun SummaryCard(
    netProfit: Long,
    totalIncome: Long,
    totalExpense: Long,
    totalAsset: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = Color(0x123629B7))
            .background(Color.White, RoundedCornerShape(15.dp))
            .padding(20.dp)
    ) {
        Column {
            Text("이번 달 순이익", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatAmount(netProfit, showSign = true),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("원", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 수입 / 비용
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryBox("수입", totalIncome, IncomeBg, IncomeText, Modifier.weight(1f))
                SummaryBox("비용", -totalExpense, ExpenseBg, ExpenseText, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 자산 증감
            SummaryBox("자산 증감", totalAsset, AssetBg, AssetText, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryBox(
    label: String,
    amount: Long,
    bgColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatAmount(amount, showSign = true),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

// ─── 미확인 배너 ────────────────────────────────────────

@Composable
private fun UnconfirmedBanner(count: Int, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(Surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 카운트 뱃지
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LogoPurple),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "미확인 내역이 있어요",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandPurple,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "확인하기 ›",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LogoPurple
        )
    }
}

// ─── 월 드롭다운 + 필터 ────────────────────────────────

@Composable
private fun MonthFilterRow(
    selectedYear: Int = 2025,
    selectedMonth: Int = 3,
    onYearSelected: (Int) -> Unit = {},
    onMonthSelected: (Int) -> Unit = {},
    onFilterClick: () -> Unit = {}
) {
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 연도 드롭다운
            Box {
                OutlinedButton(
                    onClick = { yearExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${selectedYear}년",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▾", fontSize = 10.sp, color = TextPrimary)
                }

                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false },
                    containerColor = Color.White
                ) {
                    (2026 downTo 2020).forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${year}년",
                                    fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                    color = if (year == selectedYear) BrandPurple else TextPrimary
                                )
                            },
                            onClick = {
                                onYearSelected(year)
                                yearExpanded = false
                            }
                        )
                    }
                }
            }

            // 월 드롭다운
            Box {
                OutlinedButton(
                    onClick = { monthExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${selectedMonth}월",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▾", fontSize = 10.sp, color = TextPrimary)
                }

                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false },
                    containerColor = Color.White
                ) {
                    (1..12).forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${month}월",
                                    fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                    color = if (month == selectedMonth) BrandPurple else TextPrimary
                                )
                            },
                            onClick = {
                                onMonthSelected(month)
                                monthExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 필터 아이콘
        OutlinedIconButton(
            onClick = onFilterClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Text("☰", fontSize = 16.sp, color = TextPrimary)
        }
    }
}

// ─── 탭 ─────────────────────────────────────────────────

@Composable
private fun BookTabs(
    selectedTab: BookTab,
    onTabSelected: (BookTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        Row {
            BookTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(37.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandPurple else Color.Transparent)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else Color(0xFF989898)
                    )
                }
            }
        }
    }
}

// ─── 필터 칩 ────────────────────────────────────────────

@Composable
private fun FilterChips(
    selectedFilter: EntryFilter,
    onFilterSelected: (EntryFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EntryFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isSelected) Surface else Color(0xFFF5F5F5)
                    )
                    .then(
                        if (isSelected)
                            Modifier.background(Surface, RoundedCornerShape(15.dp))
                        else Modifier
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) BrandPurple else Color(0xFF989898)
                )
            }
        }
    }
}

// ─── 거래 아이템 ────────────────────────────────────────

@Composable
private fun TransactionItem(
    entry: BookEntryResponse,
    onClick: () -> Unit
) {
    val amount = when (entry.entryType) {
        "INCOME" -> entry.incomeAmount
        "EXPENSE" -> entry.expenseAmount
        "ASSET" -> entry.fixedAssetAmount
        else -> 0L
    }
    val amountColor = when (entry.entryType) {
        "INCOME" -> BrandPurple
        "ASSET" -> TextSecondary
        else -> TextPrimary
    }
    val amountPrefix = when (entry.entryType) {
        "INCOME" -> "+"
        "ASSET" -> "+"
        else -> "-"
    }
    val iconBg = if (entry.entryType == "INCOME") Surface else Color(0xFFF5F5F5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (entry.entryType == "INCOME") "₩" else "📋",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 가맹점명 + 날짜/세목/설명
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.merchantName ?: entry.description ?: "거래",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                // 미확인 표시
                if (!entry.confirmed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFAF2A))
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                val dateStr = entry.entryDate.takeLast(5).replace("-", ".")
                Text(dateStr, fontSize = 12.sp, color = Disabled)
                if (entry.categoryName != null) {
                    Text(" · ", fontSize = 12.sp, color = Disabled)
                    Text(entry.categoryName!!, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LogoPurple)
                }
            }
        }

        // 금액
        Text(
            text = "$amountPrefix${formatAmount(amount)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Disabled,
            modifier = Modifier.size(14.dp)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = Surface,
        thickness = 1.dp
    )
}

// ─── 세목별 뷰 ─────────────────────────────────────────

@Composable
private fun CategoryView(viewModel: BookEntryViewModel) {
    val incomeCategories = viewModel.getIncomeByCategory()
    val expenseCategories = viewModel.getExpenseByCategory()
    val totalIncome = viewModel.getTotalIncome()
    val totalExpense = viewModel.getTotalExpense()

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // 수입 섹션
        if (incomeCategories.isNotEmpty()) {
            CategorySection(
                title = "수입",
                total = totalIncome,
                totalColor = IncomeValue,
                categories = incomeCategories,
                barColor = IncomeValue
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 비용 섹션
        if (expenseCategories.isNotEmpty()) {
            CategorySection(
                title = "비용",
                total = totalExpense,
                totalColor = ExpenseValue,
                categories = expenseCategories,
                barColor = ExpenseValue
            )
        }

        if (incomeCategories.isEmpty() && expenseCategories.isEmpty()) {
            Text(
                text = "세목별 데이터가 없습니다",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    total: Long,
    totalColor: Color,
    categories: List<Pair<String, Long>>,
    barColor: Color
) {
    // 헤더
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            text = "${if (title == "수입") "+" else "-"}${formatAmount(total)}원",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = totalColor
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 프로그레스 바 (세그먼트)
    val categoryColors = if (title == "수입") listOf(
        Color(0xFF3DBDA2), Color(0xFF5ED4BA), Color(0xFF88E0CE),
        Color(0xFF2A9D84), Color(0xFF1F8A73), Color(0xFF6CC9B3), Color(0xFFB0E8DA)
    ) else listOf(
        Color(0xFFE8475A), Color(0xFFF5A623), Color(0xFF5655B9),
        Color(0xFF4A90D9), Color(0xFFD4A0E8), Color(0xFFFF7E91), Color(0xFFCACACA)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        categories.forEachIndexed { index, (_, amount) ->
            val fraction = if (total > 0) amount.toFloat() / total else 0f
            Box(
                modifier = Modifier
                    .weight(fraction.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(categoryColors.getOrElse(index) { barColor })
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 카테고리 아이템들
    categories.forEachIndexed { index, (name, amount) ->
        val percentage = if (total > 0) (amount * 100 / total) else 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(categoryColors.getOrElse(index) { barColor })
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                modifier = Modifier.weight(1f))
            Text("${formatAmount(amount)}원", fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("$percentage%", fontSize = 12.sp, color = Color(0xFF989898),
                modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Disabled,
                modifier = Modifier.size(14.dp)
            )
        }
        HorizontalDivider(color = Surface, thickness = 1.dp)
    }
}

// ─── 유틸 ───────────────────────────────────────────────

private fun formatAmount(amount: Long, showSign: Boolean = false): String {
    val absAmount = kotlin.math.abs(amount)
    val formatted = NumberFormat.getNumberInstance(Locale.KOREA).format(absAmount)
    return when {
        showSign && amount > 0 -> "+$formatted"
        showSign && amount < 0 -> "-$formatted"
        else -> formatted
    }
}
