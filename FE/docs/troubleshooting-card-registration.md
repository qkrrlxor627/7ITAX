# 카드 등록 트러블슈팅 (완료)

## 1. 증상

카드 등록 후 앱을 재시작하면 등록된 카드가 사라짐.

## 2. 원인 분석

카드가 삭제되는 게 아니라, **처음부터 서버에 저장이 안 됐음.**

### 흐름
```
FE 카드번호 입력 → CardViewModel.completeRegistration()
  → CardCreateRequest 생성 (withdrawalAccountNo="", otpToken="")
    → BE: @NotBlank 검증 실패 → "입력값이 올바르지 않습니다"
  → FE: API 실패 → 인메모리 폴백 (ViewModel에만 저장)
  → 앱 재시작 → ViewModel 소멸 → 카드 사라짐
```

### 핵심 원인: FE ↔ BE API 스펙 불일치

**FE가 보낸 값:**
```kotlin
CardCreateRequest(
    cardName = "일반 카드",
    cardType = "PERSONAL",
    cardUniqueNo = "1234567890123456",  // 사용자 카드번호
    withdrawalAccountNo = "",           // ← 빈 값 (@NotBlank 위반)
    withdrawalDate = "1227",
    otpToken = ""                       // ← 빈 값 (@NotBlank 위반)
)
```

**BE가 기대하는 값:**
```java
CreateCardRequest(
    cardName,              // 카드 이름
    cardType,              // PERSONAL 또는 BUSINESS
    cardUniqueNo,          // SSAFY 카드 상품 고유번호 (GET /cards/products에서 조회)
    withdrawalAccountNo,   // 출금 계좌번호 (GET /cards/accounts에서 조회)
    withdrawalDate,        // 결제일 ("1"~"7" 범위만 가능)
    otpToken               // SMS OTP 검증 토큰
)
```

### FE와 BE의 "카드 등록" 의미 차이
- **FE**: 실제 카드 정보를 입력해서 등록
- **BE**: SSAFY 가상 금융 시스템에서 카드를 발급받는 프로세스

## 3. BE가 기대하는 카드 등록 플로우

```
1. GET  /api/cards/accounts  → 출금 계좌 목록 조회 → 계좌 선택
   (계좌 없으면 POST /api/banking/accounts로 자동 생성)
2. GET  /api/cards/products  → 카드 상품 목록 조회 → 상품 선택
3. POST /api/cards           → 계좌번호 + 상품번호 + otpToken으로 카드 생성
```

## 4. 해결 과정에서 발생한 에러들

### 4-1. "입력값이 올바르지 않습니다"
- **원인**: `withdrawalAccountNo`, `otpToken` 빈 값 → @NotBlank 위반
- **해결**: 계좌 선택/상품 선택 화면 추가, API에서 값 조회 후 전달

### 4-2. CardType "DEBIT" 없음
- **원인**: FE가 `"DEBIT"` / `"CREDIT"` 보냄, BE enum은 `[PERSONAL, BUSINESS]`
- **해결**: `DEBIT → PERSONAL`, `CREDIT → BUSINESS`로 수정

### 4-3. "OTP 인증 토큰이 유효하지 않습니다"
- **원인**: SMS 비용 문제로 OTP 목업 중, BE에서 실제 검증
- **해결**: BE 팀이 개발환경에서 `"test-token"` 스킵 처리 (1회용, 5분 만료)

### 4-4. "출금날짜는 1 ~ 7 로만 입력 가능합니다"
- **원인**: FE에서 `withdrawalDate = "15"` 전송
- **해결**: `"15"` → `"4"` (고정값) 으로 수정. SSAFY Finance API가 1~7만 허용

### 4-5. "은행 연동 서비스가 일시적으로 불가합니다"
- **원인**: SSAFY Finance API 에러를 BE가 일괄 "은행 연동 불가"로 감싸서 전달
- **해결**: 수정 전 빌드로 테스트한 것이 원인. 재빌드 후 해결

### 4-6. 카드 두 개 생성 버그
- **원인**: API 실패 시 인메모리 폴백으로 카드 추가 + API 성공 시 loadCards()로 또 추가
- **해결**: 인메모리 폴백 제거, 서버 저장 실패 시 에러 다이얼로그만 표시

## 5. 최종 해결

### 5-1. 카드 등록 플로우 재설계
```
기존: 유형 선택 → 카드번호 직접 입력 → SMS 목업 → 등록
최종: 유형 선택 → 계좌 선택 → 카드 상품 선택 → SMS 인증 → 등록
```

### 5-2. API + Repository + ViewModel
- `GET /cards/accounts` — 계좌 목록 조회 추가
- `GET /cards/products` — 카드 상품 목록 조회 추가
- 계좌 없으면 `POST /banking/accounts` 자동 호출 (백그라운드)
- `completeRegistration()`에서 선택된 계좌번호 + 상품번호 사용
- `otpToken = "test-token"` (BE 개발환경 OTP 스킵)
- `withdrawalDate = "4"` (SSAFY 1~7 제한)
- 인메모리 폴백 제거

### 5-3. UI 화면 추가
- `CardAccountSelectScreen` — 서버에서 계좌 목록 조회 + 선택 (잔액 000,000,000원 형식)
- `CardProductSelectScreen` — 서버에서 카드 상품 목록 조회 + 선택 (실적/한도 포맷팅)
- 카드 등록 실패 시 에러 다이얼로그 표시

### 5-4. 카드 등록 성공 확인
- `curl GET /api/cards` 로 서버에 카드 2개 저장 확인
  - id=1: SSAFY 스마일카드 (PERSONAL, last4=8295)
  - id=3: 사업자 카드 (BUSINESS, last4=7008)

## 6. 남은 제약사항

### 6-1. test-token 1회용
- 매번 카드 등록 테스트 시 BE 팀에 토큰 재주입 요청 필요
- 운영 환경에서는 실제 SMS OTP 필요 (비용 발생)

### 6-2. SMS 인증 목업
- FE SMS 인증 화면은 아무 6자리 입력하면 통과
- BE `/api/sms/send`, `/api/sms/verify` 연동은 비용 문제로 보류

## 7. 관련 파일

### FE
- `CardViewModel.kt` — 카드 등록 로직
- `CardApi.kt` — Retrofit 인터페이스
- `CardRepository.kt` / `CardRepositoryImpl.kt` — API 호출
- `CardAccountSelectScreen.kt` — 계좌 선택 화면
- `CardProductSelectScreen.kt` — 카드 상품 선택 화면
- `CardSmsScreen.kt` — SMS 인증 + 에러 다이얼로그
- `CardCreateRequest.kt` — 요청 DTO

### BE
- `CardController.java` — 카드 API 컨트롤러
- `CreateCardRequest.java` — 요청 DTO (@NotBlank 검증)
- `CardService.java:78~107` — 카드 생성 로직 (OTP 검증 + SSAFY API 호출)

---

*작성일: 2026-03-26*
*완료일: 2026-03-27*
*상태: 완료 — 카드 등록 성공, test-token 1회용 제약 있음*
