# 세금 캘린더 트러블슈팅 문서

## 리팩토링 일자: 2026-03-25

---

### 이슈 1: FilterTabs 외곽선 처리 버그

**파일**: `TaxCalendarScreen.kt` - `FilterTabs()`

**증상**: 비선택 필터 탭에 불필요한 중복 `background` modifier가 3겹으로 적용되어 있고, 외곽선을 시뮬레이션하기 위해 `matchParentSize` Box를 2개 겹쳐 놓음. 성능 낭비 + 외곽선이 제대로 안 보이는 경우 발생.

**원인**: `Modifier.border()`를 사용하지 않고 배경색 레이어링으로 외곽선을 구현하려 함.

**수정**:
```kotlin
// Before (문제)
.background(if (isSelected) BrandPurple else Color.White)
.then(if (!isSelected) Modifier.background(Color.White, ...) else Modifier)
.then(if (!isSelected) Modifier.background(Color.Transparent) else Modifier)
// + matchParentSize Box 2개

// After (수정)
.background(if (isSelected) BrandPurple else Color.White)
.then(if (!isSelected) Modifier.border(1.dp, Disabled, RoundedCornerShape(20.dp)) else Modifier)
```

---

### 이슈 2: ScheduleItem dead code

**파일**: `TaxCalendarScreen.kt` - `ScheduleItem()`

**증상**: `getFilingPeriod(deadline)` 호출 결과를 `filingPeriod` 변수에 저장하지만, 이전 리팩토링에서 UI 표시 코드가 제거된 후 변수만 남아 있음.

**수정**: 미사용 변수 및 호출 제거.

---

### 이슈 3: TimelineItem 배지 조건 중복

**파일**: `TaxCalendarScreen.kt` - `TimelineItem()`

**증상**:
```kotlin
if (dDay == null) { ... }
else if (isNext && dDay != null) { ... }  // dDay != null은 항상 true
```
`dDay == null`이 이미 첫 조건에서 처리되었으므로 `dDay != null`은 항상 참.

**수정**: `else if (isNext && dDay != null)` → `else if (isNext)`

---

### 이슈 4: getDotsForDate 미사용 함수

**파일**: `TaxCalendarScreen.kt`

**증상**: 캘린더 그리드가 바+도트 방식으로 리팩토링되면서 `getDotsForDate()` 함수가 더 이상 호출되지 않지만 코드에 남아있음.

**수정**: 함수 제거.

---

### 이슈 5 (검토 후 유지): DdayNormal 색상

**파일**: `Color.kt`

**내용**: DdayNormal이 보라색(`0xFF5655B9`)으로 설정됨. 와이어프레임에서는 핑크로 보이나, 현재 앱 디자인에서 보라색이 브랜드 컬러와 일관성이 있어 유지하기로 결정.

---

## 캘린더 코드 흐름 정리

### 화면 구성
```
TaxCalendarScreen (메인)
├── CalendarHeader (헤더 + 알림 아이콘)
├── UrgentBanner (임박 일정 배너, ALL 필터 + 일정 있을 때만)
├── MonthNavigation (< 2026년 3월 >, ALL 필터일 때)
├── FilterTabs (전체 / 부가세 / 소득세 / 지방세)
├── [ALL 필터]
│   ├── CalendarGrid (달력 그리드 + 기간 바/도트)
│   ├── CalendarLegend (범례)
│   └── MonthScheduleSection (이번 달 일정 리스트)
└── [특정 세금 필터]
    └── AnnualTimelineView (연간 타임라인 + 예상 납부액)

TaxCalendarDetailScreen (상세)
├── DetailTopSection (세금명 + D-day 배지)
├── InfoCard (세금종류/신고유형/기간/마감일/세무서)
├── ChecklistCard (준비 체크리스트 + 프로그레스)
├── ReminderCard (리마인드 알림 설정)
└── BottomButtons (AI 가이드 / 홈택스 신고)

NotificationSettingsScreen (알림 설정)
├── MainToggle (캘린더 알림 ON/OFF)
├── RemindTimingSection (D-7/D-3/D-1/당일 칩)
├── TaxTypeSection (부가세/소득세/지방세 토글)
└── NotificationMethodSection (푸시/카카오톡)
```

### 데이터 흐름
```
TaxCalendarViewModel
├── _deadlines: 목 데이터 7건 (부가세 4 + 소득세 2 + 지방세 1)
├── _selectedFilter: ALL/VAT/INCOME/LOCAL
├── _currentMonth: YearMonth (월 네비게이션)
├── getFilteredDeadlines() → 필터 적용된 리스트
├── getDeadlinesForMonth() → 특정 월 리스트
└── getMostUrgentDeadline() → D-day 가장 작은 항목
```

### 세금 분류 규칙 (classifyTax)
```
"지방" 포함 → LOCAL (우선 체크)
"부가" 포함 → VAT
"소득" 포함 → INCOME
기타 → INCOME (기본값)
```

### D-Day 색상 규칙
```
D-3 이하 → DdayError (빨강 #FF4267)
D-7 이하 → DdayWarning (오렌지 #FB6B18)
D-8 이상 → DdayNormal (보라 #5655B9)
```
