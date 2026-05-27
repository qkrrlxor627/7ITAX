package com.ssafy.seveniTax.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.data.model.tax.TaxDeadline
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel
import com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel.Companion.classifyTax
import com.ssafy.seveniTax.viewmodel.TaxFilter
import com.ssafy.seveniTax.viewmodel.TaxType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun TaxCalendarScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: TaxCalendarViewModel = hiltViewModel()
) {
    val deadlines by viewModel.deadlines.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val urgentDeadline = viewModel.getMostUrgentDeadline()
    val monthDeadlines = viewModel.getDeadlinesForMonth(currentMonth)
    val estimatedVat by viewModel.estimatedVat.collectAsState()
    val estimatedIncomeTax by viewModel.estimatedIncomeTax.collectAsState()
    val estimatedLocalTax by viewModel.estimatedLocalTax.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { innerPadding ->
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color.White)
    ) {
        // 헤더
        CalendarHeader(
            onBack = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 임박 일정 배너 → 클릭 시 상세 이동
            if (urgentDeadline != null && selectedFilter == TaxFilter.ALL) {
                UrgentBanner(
                    deadline = urgentDeadline,
                    onClick = {
                        navController.navigate(
                            Route.TaxCalendarDetail.create(
                                taxName = urgentDeadline.taxName,
                                deadline = urgentDeadline.deadline,
                                dDay = urgentDeadline.dDay,
                                description = urgentDeadline.description
                            )
                        )
                    }
                )
            }

            // 필터에 따라 제목 변경
            if (selectedFilter != TaxFilter.ALL) {
                // 연도 + 세금 타입 제목
                Text(
                    text = "${currentMonth.year}년 ${selectedFilter.label} 일정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                HorizontalDivider(color = Surface, thickness = 1.dp)
            } else {
                // 월 네비게이션 (전체 필터일 때만)
                MonthNavigation(
                    currentMonth = currentMonth,
                    onPrevious = { viewModel.goToPreviousMonth() },
                    onNext = { viewModel.goToNextMonth() }
                )
            }

            // 필터 탭
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.selectFilter(it) }
            )

            if (selectedFilter == TaxFilter.ALL) {
                // ─── 전체: 캘린더 그리드 모드 ───
                CalendarGrid(
                    yearMonth = currentMonth,
                    deadlines = viewModel.getFilteredDeadlines()
                )

                CalendarLegend()

                HorizontalDivider(color = Surface, thickness = 1.dp)

                MonthScheduleSection(monthDeadlines, navController)

                // ─── 예상 납부액 요약 ───
                TaxEstimationSummary(
                    estimatedIncomeTax = estimatedIncomeTax,
                    estimatedLocalTax = estimatedLocalTax
                )
            } else {
                // ─── 특정 세금: 연간 타임라인 모드 ───
                AnnualTimelineView(
                    filter = selectedFilter,
                    deadlines = viewModel.getFilteredDeadlines(),
                    year = currentMonth.year,
                    navController = navController,
                    estimatedVat = estimatedVat,
                    estimatedIncomeTax = estimatedIncomeTax,
                    estimatedLocalTax = estimatedLocalTax
                )
            }
        }
    }
    }
}

// ─── 헤더 ───────────────────────────────────────────────

@Composable
private fun CalendarHeader(
    onBack: () -> Unit
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
            text = "세금 캘린더",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.weight(1f))

        // 뒤로가기 버튼과 대칭을 위한 빈 공간
        Spacer(modifier = Modifier.size(48.dp))
    }
}

// ─── 임박 일정 배너 ─────────────────────────────────────

@Composable
private fun UrgentBanner(deadline: TaxDeadline, onClick: () -> Unit = {}) {
    val ddayColor = getDdayColor(deadline.dDay)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(AlertBannerBg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "가장 임박한 일정",
                    fontSize = 13.sp,
                    color = ddayColor,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = deadline.taxName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .background(ddayColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (deadline.dDay == 0) "D-Day" else "D-${deadline.dDay}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─── 월 네비게이션 ──────────────────────────────────────

@Composable
private fun MonthNavigation(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "이전 달",
                tint = TextSecondary
            )
        }

        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "다음 달",
                tint = TextSecondary
            )
        }
    }

    HorizontalDivider(color = Surface, thickness = 1.dp)
}

// ─── 필터 탭 ────────────────────────────────────────────

@Composable
private fun FilterTabs(
    selectedFilter: TaxFilter,
    onFilterSelected: (TaxFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TaxFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) BrandPurple else Color.White)
                    .then(
                        if (!isSelected) Modifier.border(
                            1.dp, Disabled, RoundedCornerShape(20.dp)
                        ) else Modifier
                    )
                    .clickable { onFilterSelected(filter) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                    if (isSelected && filter == TaxFilter.ALL) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LogoOrange)
                        )
                    }
                }
            }
        }
    }
}

// ─── 캘린더 그리드 ──────────────────────────────────────

private data class TaxPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: TaxType
)

private fun buildTaxPeriods(deadlines: List<TaxDeadline>): List<TaxPeriod> {
    return deadlines.mapNotNull { deadline ->
        val endDate = try { LocalDate.parse(deadline.deadline) } catch (_: Exception) { return@mapNotNull null }
        val type = classifyTax(deadline)
        val name = deadline.taxName
        val year = endDate.year

        val startDate = when {
            name.contains("1기") && name.contains("예정") -> LocalDate.of(year, 4, 1)
            name.contains("1기") && name.contains("확정") -> LocalDate.of(year, 7, 1)
            name.contains("2기") && name.contains("예정") -> LocalDate.of(year, 10, 1)
            name.contains("2기") && name.contains("확정") -> LocalDate.of(year, 1, 1)
            name.contains("종합소득세") -> LocalDate.of(year, 5, 1)
            name.contains("지방소득세") -> LocalDate.of(year, 5, 1)
            name.contains("중간예납") -> LocalDate.of(year, 11, 1)
            else -> endDate.withDayOfMonth(1)
        }
        TaxPeriod(startDate, endDate, type)
    }
}

// 셀별 바 상태
private enum class BarSegment { NONE, START, MIDDLE, END, SINGLE }

private fun getBarSegment(date: LocalDate, period: TaxPeriod): BarSegment {
    if (date < period.startDate || date > period.endDate) return BarSegment.NONE
    if (period.startDate == period.endDate && date == period.startDate) return BarSegment.SINGLE
    if (date == period.startDate) return BarSegment.START
    if (date == period.endDate) return BarSegment.END
    return BarSegment.MIDDLE
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    deadlines: List<TaxDeadline>
) {
    val today = LocalDate.now()
    val firstDay = yearMonth.atDay(1)
    val startOffset = when (firstDay.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        else -> firstDay.dayOfWeek.value
    }
    val daysInMonth = yearMonth.lengthOfMonth()
    val taxPeriods = buildTaxPeriods(deadlines)
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()
    val visiblePeriods = taxPeriods.filter { it.startDate <= monthEnd && it.endDate >= monthStart }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 요일 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
            dayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (index) {
                        0 -> CalendarSunday
                        6 -> CalendarSaturday
                        else -> TextSecondary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            // 날짜 숫자 행
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0..6) {
                    val day = row * 7 + col - startOffset + 1
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val date = yearMonth.atDay(day)
                            val isToday = date == today
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(TaxToday),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Text(
                                    "$day", fontSize = 14.sp,
                                    color = when (col) { 0 -> CalendarSunday; 6 -> CalendarSaturday; else -> TextPrimary }
                                )
                            }
                        }
                    }
                }
            }

            // 바+도트 행: 각 period를 한 줄씩 (● ─── ●)
            val periodsInWeek = visiblePeriods.filter { period ->
                val wfd = maxOf(row * 7 - startOffset + 1, 1)
                val wld = minOf((row + 1) * 7 - startOffset, daysInMonth)
                if (wfd > daysInMonth) false
                else {
                    val ws = yearMonth.atDay(maxOf(wfd, 1))
                    val we = yearMonth.atDay(minOf(wld, daysInMonth))
                    period.startDate <= we && period.endDate >= ws
                }
            }

            if (periodsInWeek.isNotEmpty()) {
                periodsInWeek.forEach { period ->
                    val color = getTaxColor(period.type)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..6) {
                            val day = row * 7 + col - startOffset + 1
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..daysInMonth) {
                                    val date = yearMonth.atDay(day)
                                    val segment = getBarSegment(date, period)
                                    when (segment) {
                                        BarSegment.START -> {
                                            // 바: 중앙→오른쪽
                                            Box(
                                                modifier = Modifier
                                                    .height(3.dp)
                                                    .fillMaxWidth(0.5f)
                                                    .align(Alignment.CenterEnd)
                                                    .background(color.copy(alpha = 0.35f))
                                            )
                                            // 도트: 중앙
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                        BarSegment.END -> {
                                            // 바: 왼쪽→중앙
                                            Box(
                                                modifier = Modifier
                                                    .height(3.dp)
                                                    .fillMaxWidth(0.5f)
                                                    .align(Alignment.CenterStart)
                                                    .background(color.copy(alpha = 0.35f))
                                            )
                                            // 도트: 중앙
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                        BarSegment.MIDDLE -> {
                                            Box(
                                                modifier = Modifier
                                                    .height(3.dp)
                                                    .fillMaxWidth()
                                                    .background(color.copy(alpha = 0.35f))
                                            )
                                        }
                                        BarSegment.SINGLE -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                        BarSegment.NONE -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

// ─── 범례 ───────────────────────────────────────────────

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("오늘", TaxToday)
        Spacer(modifier = Modifier.width(20.dp))
        LegendItem("부가세", TaxVat)
        Spacer(modifier = Modifier.width(20.dp))
        LegendItem("소득세", TaxIncome)
        Spacer(modifier = Modifier.width(20.dp))
        LegendItem("지방세", TaxLocal)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

// ─── 이번 달 일정 섹션 ──────────────────────────────────

@Composable
private fun MonthScheduleSection(deadlines: List<TaxDeadline>, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "이번 달 일정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (deadlines.isEmpty()) {
            Text(
                text = "이번 달 세금 일정이 없습니다",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            deadlines.forEach { deadline ->
                ScheduleItem(deadline) {
                    navController.navigate(
                        Route.TaxCalendarDetail.create(
                            taxName = deadline.taxName,
                            deadline = deadline.deadline,
                            dDay = deadline.dDay,
                            description = deadline.description
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScheduleItem(deadline: TaxDeadline, onClick: () -> Unit) {
    val dateFormatted = try {
        val date = LocalDate.parse(deadline.deadline)
        String.format("%02d.%02d", date.monthValue, date.dayOfMonth)
    } catch (e: Exception) {
        deadline.deadline
    }
    val ddayColor = getDdayColor(deadline.dDay)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 날짜
        Text(
            text = dateFormatted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ddayColor,
            modifier = Modifier.width(56.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 세금명
        Text(
            text = deadline.taxName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // D-day 배지
        Box(
            modifier = Modifier
                .background(ddayColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (deadline.dDay == 0) "D-Day" else "D-${deadline.dDay}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    HorizontalDivider(color = Surface, thickness = 1.dp)
}

private fun getFilingPeriod(deadline: TaxDeadline): String? {
    val name = deadline.taxName
    val deadlineDate = try { LocalDate.parse(deadline.deadline) } catch (e: Exception) { return null }
    val year = deadlineDate.year

    return when {
        name.contains("부가") && name.contains("1기") -> "$year.01.01 ~ $year.06.30"
        name.contains("부가") && name.contains("2기") -> "$year.07.01 ~ $year.12.31"
        name.contains("부가") && deadlineDate.monthValue <= 6 -> "$year.01.01 ~ $year.06.30"
        name.contains("부가") -> "$year.07.01 ~ $year.12.31"
        name.contains("종합소득세") -> "$year.05.01 ~ $year.05.31"
        name.contains("지방소득세") -> "$year.05.01 ~ $year.05.31"
        else -> null
    }
}

// ─── 연간 타임라인 뷰 (필터 선택 시) ────────────────────

@Composable
private fun AnnualTimelineView(
    filter: TaxFilter,
    deadlines: List<TaxDeadline>,
    year: Int,
    navController: NavController,
    estimatedVat: Long = 0,
    estimatedIncomeTax: Long = 0,
    estimatedLocalTax: Long = 0
) {
    val today = LocalDate.now()
    val taxColor = when (filter) {
        TaxFilter.VAT -> TaxVat
        TaxFilter.INCOME -> TaxIncome
        TaxFilter.LOCAL -> TaxLocal
        else -> Accent
    }

    // 연간 일정 정적 데이터 (API 데이터 + 보충)
    val annualSchedule = getAnnualSchedule(filter, year)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "${filter.label} 일정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${year}년 연간 일정",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 타임라인
        annualSchedule.forEachIndexed { index, item ->
            val itemDate = try {
                LocalDate.parse(item.date)
            } catch (e: Exception) {
                today
            }
            val isPast = itemDate.isBefore(today)
            val isNext = !isPast && (index == 0 || try {
                LocalDate.parse(annualSchedule[index - 1].date).isBefore(today)
            } catch (e: Exception) {
                false
            })
            val isLast = index == annualSchedule.size - 1

            TimelineItem(
                dateLabel = "%02d.%02d".format(itemDate.monthValue, itemDate.dayOfMonth),
                title = item.title,
                subtitle = item.subtitle,
                isPast = isPast,
                isNext = isNext,
                isLast = isLast,
                dDay = item.dDay,
                dotColor = taxColor,
                onClick = {
                    // 서버 deadlines에서 매칭 시도, 없으면 타임라인 데이터로 직접 이동
                    val matchingDeadline = deadlines.find {
                        it.taxName.contains(item.title) || item.title.contains(it.taxName.take(4))
                    }
                    val taxName = matchingDeadline?.taxName
                        ?: "${filter.label} ${item.title}"
                    val deadline = matchingDeadline?.deadline ?: item.date
                    val dDay = matchingDeadline?.dDay
                        ?: item.dDay ?: 0
                    val description = matchingDeadline?.description ?: item.subtitle

                    navController.navigate(
                        Route.TaxCalendarDetail.create(
                            taxName = taxName,
                            deadline = deadline,
                            dDay = dDay,
                            description = description
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 예상 납부액 카드
        EstimateCard(filter = filter, estimatedVat = estimatedVat, estimatedIncomeTax = estimatedIncomeTax, estimatedLocalTax = estimatedLocalTax)
    }
}

private data class AnnualScheduleItem(
    val date: String,
    val title: String,
    val subtitle: String,
    val dDay: Int?    // null = 완료
)

private fun getAnnualSchedule(filter: TaxFilter, year: Int): List<AnnualScheduleItem> {
    val today = LocalDate.now()
    return when (filter) {
        TaxFilter.VAT -> listOf(
            AnnualScheduleItem("$year-01-25", "2기 확정신고", "${year - 1}.07~12월분",
                daysBetween(today, LocalDate.of(year, 1, 25))),
            AnnualScheduleItem("$year-04-25", "1기 예정고지 납부", "고지서 기준 납부",
                daysBetween(today, LocalDate.of(year, 4, 25))),
            AnnualScheduleItem("$year-07-25", "1기 확정신고", "$year.01~06월분",
                daysBetween(today, LocalDate.of(year, 7, 25))),
            AnnualScheduleItem("$year-10-25", "2기 예정고지 납부", "고지서 기준 납부",
                daysBetween(today, LocalDate.of(year, 10, 25)))
        )
        TaxFilter.INCOME -> listOf(
            AnnualScheduleItem("$year-05-31", "종합소득세 확정 신고 및 납부", "${year - 1}년 귀속",
                daysBetween(today, LocalDate.of(year, 5, 31))),
            AnnualScheduleItem("$year-11-30", "중간예납", "중간예납세액 납부",
                daysBetween(today, LocalDate.of(year, 11, 30)))
        )
        TaxFilter.LOCAL -> listOf(
            AnnualScheduleItem("$year-05-31", "지방소득세 신고 및 납부", "종합소득분",
                daysBetween(today, LocalDate.of(year, 5, 31))),
            AnnualScheduleItem("$year-08-31", "주민세 납부", "균등분 납부",
                daysBetween(today, LocalDate.of(year, 8, 31)))
        )
        else -> emptyList()
    }
}

private fun daysBetween(from: LocalDate, to: LocalDate): Int? {
    val days = java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt()
    return if (days < 0) null else days
}

@Composable
private fun TimelineItem(
    dateLabel: String,
    title: String,
    subtitle: String,
    isPast: Boolean,
    isNext: Boolean,
    isLast: Boolean,
    dDay: Int?,
    dotColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // 타임라인 도트 + 라인
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPast) dotColor.copy(alpha = 0.4f)
                        else if (isNext) dotColor
                        else dotColor.copy(alpha = 0.6f)
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(72.dp)
                        .background(Disabled.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 날짜
        Text(
            text = dateLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isNext) dotColor else if (isPast) TextSecondary else TextPrimary,
            modifier = Modifier.width(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 내용
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPast) TextSecondary else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 배지
        if (dDay == null) {
            // 완료
            Box(
                modifier = Modifier
                    .background(Disabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "완료",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        } else if (isNext) {
            val badgeColor = getDdayColor(dDay)
            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (dDay == 0) "D-Day" else "D-$dDay",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .then(
                        Modifier.background(
                            Disabled.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "D-$dDay",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }

    if (!isLast) {
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ─── 예상 납부액 카드 ───────────────────────────────────

@Composable
private fun EstimateCard(
    filter: TaxFilter,
    estimatedVat: Long = 0,
    estimatedIncomeTax: Long = 0,
    estimatedLocalTax: Long = 0
) {
    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
    val (label, amount) = when (filter) {
        TaxFilter.VAT -> "부가가치세 예상 납부액" to fmt.format(estimatedVat)
        TaxFilter.INCOME -> "종합소득세 예상 납부액" to fmt.format(estimatedIncomeTax)
        TaxFilter.LOCAL -> "지방소득세 예상 납부액" to fmt.format(estimatedLocalTax)
        else -> "" to "0"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${amount}원",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "장부 데이터 기반 자동 계산",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

// ─── 유틸 ───────────────────────────────────────────────

private fun getTaxColor(type: TaxType): Color {
    return when (type) {
        TaxType.VAT -> TaxVat
        TaxType.INCOME -> TaxIncome
        TaxType.LOCAL -> TaxLocal
    }
}

private fun getDdayColor(dDay: Int): Color {
    return when {
        dDay <= 3 -> DdayError
        dDay <= 7 -> DdayWarning
        else -> DdayNormal
    }
}

// ─── 예상 납부액 요약 카드 (전체 필터) ─────────────────

@Composable
private fun TaxEstimationSummary(
    estimatedIncomeTax: Long,
    estimatedLocalTax: Long
) {
    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "예상 납부액",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "장부 데이터 기반 자동 계산",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 소득세 카드
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(TaxIncome.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TaxIncome)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "종합소득세",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${fmt.format(estimatedIncomeTax)}원",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // 지방세 카드
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(TaxLocal.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TaxLocal)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "지방소득세",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${fmt.format(estimatedLocalTax)}원",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
