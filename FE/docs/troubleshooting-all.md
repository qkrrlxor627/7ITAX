# 전체 트러블슈팅 문서

## 1. Auth - verify-identity 500 에러

### 증상
`POST /api/auth/verify-identity` 호출 시 500 에러

### 원인
curl 테스트 시 birthDate를 `"19900101"` 형식으로 보냄. BE `LocalDate.parse()`는 ISO 형식(`"1990-01-01"`)만 파싱 가능.

### 해결
FE 앱 코드(`AuthViewModel.buildBirthDate()`)는 이미 `"1990-01-01"` ISO 형식으로 변환 중이라 앱에서는 문제없음. curl 테스트 시 형식만 맞추면 해결.

### 관련 파일
- `AuthViewModel.kt:296` — `buildBirthDate()`

---

## 2. Auth - Invalid UTF-8 middle byte 에러

### 증상
BE 로그에 `Invalid UTF-8 middle byte 0xd7` 에러

### 원인
FE API 요청 시 `Content-Type` 헤더에 charset 미명시

### 해결
`ApiClient.kt` OkHttp Interceptor에 `Content-Type: application/json; charset=UTF-8` 헤더 명시 추가

### 관련 파일
- `ApiClient.kt:33`

---

## 3. 카드 등록 - 서버 저장 안 됨

### 증상
카드 등록 후 앱 재시작하면 카드 사라짐

### 원인
FE ↔ BE API 스펙 불일치. FE가 사용자 카드번호를 직접 보냈으나, BE는 SSAFY 가상 금융 시스템 카드 발급 프로세스 기대.

### 해결
카드 등록 플로우 재설계:
- 계좌 선택 → 카드 상품 선택 → SMS 인증 → 카드 생성
- 계좌 없으면 자동 생성 (백그라운드)

### 상세
[troubleshooting-card-registration.md](troubleshooting-card-registration.md) 참조

---

## 4. 카드 등록 - CardType enum 불일치

### 증상
"CardType Enum에 DEBIT 값이 없음" 에러

### 원인
FE가 `"DEBIT"` / `"CREDIT"` 전송, BE enum은 `[PERSONAL, BUSINESS]`만 허용

### 해결
`DEBIT → PERSONAL`, `CREDIT → BUSINESS`로 수정

### 관련 파일
- `CardViewModel.kt:96`

---

## 5. 카드 등록 - OTP 토큰 유효하지 않음

### 증상
"OTP 인증 토큰이 유효하지 않습니다" 에러

### 원인
SMS 비용 문제로 OTP 목업 중, BE에서 실제 검증

### 해결
BE 팀이 개발환경에서 `"test-token"` 스킵 처리 (1회용, 5분 만료)

### 관련 파일
- `CardViewModel.kt:172` — `otpToken = "test-token"`

---

## 6. 카드 등록 - 출금날짜 범위 오류

### 증상
"출금날짜는 1 ~ 7 로만 입력 가능합니다" 에러

### 원인
FE에서 `withdrawalDate = "15"` 전송. SSAFY Finance API가 1~7만 허용.

### 해결
`"15"` → `"4"` 고정값으로 수정

### 관련 파일
- `CardViewModel.kt:171`

---

## 7. 카드 등록 - 두 개 생성 버그

### 증상
카드 등록 시 목록에 같은 카드 두 개 표시

### 원인
API 실패 시 인메모리 폴백으로 카드 추가 + API 성공 시 `loadCards()`로 서버 목록 추가 → 중복

### 해결
인메모리 폴백 제거. 서버 저장 실패 시 에러 다이얼로그만 표시.

### 관련 파일
- `CardViewModel.kt:162~200`

---

## 8. 네비게이션 바 겹침

### 증상
하단 버튼이 시스템 네비게이션 바(뒤로가기/홈/최근앱)와 겹침

### 원인
`navigationBarsPadding()` 누락

### 해결
전체 화면 점검 후 10개 파일에 `navigationBarsPadding()` 추가:
- UnclassifiedListScreen, MemoAddScreen, BookMemoAddScreen
- AutoClassificationScreen, ClassificationCompleteScreen, ClassificationResultScreen
- CategorySelectScreen, TaxCalendarDetailScreen

---

## 9. CategorySelect 네비게이션 크래시

### 증상
세목 변경 선택 완료 시 앱 튕김

### 원인
`Route.CategorySelect.path`에 쿼리 파라미터 플레이스홀더(`{returnTo}`, `{entryId}`)가 포함되어 네비게이션 시 파싱 실패

### 해결
`Route.CategorySelect.path` → `Route.CategorySelect.create()` 로 변경. ClassificationComplete도 동일 수정.

### 관련 파일
- `NavGraph.kt:237, 261, 418`

---

## 10. 사진 첨부 - 카메라 후 갤러리 크래시

### 증상
카메라 촬영 후 갤러리에서 사진 추가하면 앱 크래시

### 원인
갤러리/카메라 전환 시 Activity 재생성 → `remember` 상태 소멸

### 해결
`remember` → `rememberSaveable`로 변경. URI를 String으로 변환해서 저장.

### 관련 파일
- `MemoAddScreen.kt`, `BookMemoAddScreen.kt`

---

## 11. 알림 탭 시 PIN 화면 표시

### 증상
앱 실행 중 알림 탭하면 PIN 입력 화면으로 이동

### 원인
`savedInstanceState` 체크가 알림 탭 시 동작 안 함 (Activity 재생성으로 항상 null)

### 해결
`singleTop` + `onNewIntent` 방식으로 변경:
- 앱 실행 중 → `onNewIntent`로 바로 이동
- 앱 종료 상태 → PIN 인증 후 이동

### 관련 파일
- `MainActivity.kt`, `AndroidManifest.xml`

---

## 12. 리포트 연/월 상태 유실

### 증상
리포트에서 장부로 이동 후 돌아오면 선택한 연/월이 현재 날짜로 초기화

### 원인
`selectedYear`, `selectedMonth`가 `remember`로 선언 → 백스택 복원 시 초기화

### 해결
`remember` → `rememberSaveable`로 변경

### 관련 파일
- `TaxReportScreen.kt:48~50`

---

## 13. 간편장부 필터 적용 안 됨

### 증상
필터 설정 후 장부 목록에 반영 안 됨

### 원인
BookEntryListScreen과 BookFilterScreen이 각각 별도 ViewModel 인스턴스 사용

### 해결
NavGraph 레벨에서 `bookEntryViewModel` 하나를 공유하도록 변경

### 관련 파일
- `NavGraph.kt:306~312`

---

## 14. QR 결제 - amount 0 에러

### 증상
QR 토큰 생성 시 400 에러

### 원인
`amount = 0`으로 전송. BE `@Positive` 검증에 의해 0보다 커야 함.

### 해결
`amount = 0` → `amount = 1000` (테스트용 최소 금액)

### 관련 파일
- `QrPaymentScreen.kt`

---

## 15. BE API 명세 불일치

### 증상
다양한 API 호출 실패

### 원인
FE ↔ BE 필드 불일치

### 해결
| 항목 | 수정 |
|------|------|
| AccountResponse.id | `String → Long` |
| BalanceResponse.accountId | `String → Long` |
| Auth logout | Authorization 헤더 전달 추가 |
| BookEntry getEntries | 쿼리 파라미터 7개 추가 |
| CardType | `DEBIT/CREDIT → PERSONAL/BUSINESS` |

### 관련 파일
- `AccountResponse.kt`, `BalanceResponse.kt`, `AuthApi.kt`, `BookEntryApi.kt`

---

*작성일: 2026-03-27*
