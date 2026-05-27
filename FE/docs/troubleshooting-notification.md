# 알림 트러블슈팅

## 1. FCM 토큰 서버 전송 미구현

### 증상
서버에서 특정 사용자에게 푸시 알림을 보낼 수 없음

### 원인
`TaxFirebaseMessagingService.onNewToken()`에서 토큰을 서버에 전송하는 코드가 TODO 상태

### 현재 상태
- BE에서 FCM 토큰 저장 API 구현 예정
- API 나오면 FE에서 `onNewToken`에서 호출만 추가하면 됨

### 관련 파일
- `TaxFirebaseMessagingService.kt:13`

---

## 2. 결제 완료 알림 미구현 → 구현 완료

### 증상
사업자카드로 결제 완료해도 알림 안 옴

### 원인
FCM 수신에서 `"classification"`, `"tax_calendar"` 두 타입만 처리. `"payment"` 타입 없음.

### 해결
- `NotificationHelper`에 결제 알림 채널(`payment_channel`) + `showPaymentNotification()` 추가
- `TaxFirebaseMessagingService`에 `"payment"` 타입 수신 처리 추가
- 결제 완료/실패 구분하여 알림 표시

### 관련 파일
- `NotificationHelper.kt` — `PAYMENT_CHANNEL_ID`, `showPaymentNotification()`
- `TaxFirebaseMessagingService.kt` — `handlePaymentNotification()`

---

## 3. SSE 실시간 구독 미구현 → 폴링으로 대체

### 증상
QR 결제 후 가맹점이 승인해도 앱에서 실시간 반영 안 됨

### 원인
BE에 `/payments/qr/token/{token}/events` SSE 엔드포인트 있지만 FE에서 구독 코드 없음

### 해결
SSE 대신 2초 간격 폴링으로 대체:
- `PaymentViewModel.startPollingStatus()` — `GET /payments/qr/token/{token}/status` 2초마다 호출
- `CAPTURED` 또는 `CANCELLED` 감지 시 폴링 중단 + 팝업 표시
- QR 토큰 생성 후 자동 폴링 시작

### SSE vs 폴링
| 항목 | SSE | 폴링 |
|------|-----|------|
| 실시간성 | 즉시 | 최대 2초 지연 |
| 서버 부하 | 낮음 | 높음 |
| 구현 복잡도 | OkHttp EventSource 필요 | 간단 |
| 안정성 | 연결 끊김 처리 필요 | 안정적 |

MVP에서는 폴링으로 충분. SSE는 추후 최적화 시 전환.

### 관련 파일
- `PaymentViewModel.kt` — `startPollingStatus()`
- `QrPaymentScreen.kt` — 폴링 LaunchedEffect

---

## 4. 알림 채널 현황

| 채널 ID | 이름 | 용도 | 상태 |
|---------|------|------|------|
| `classification_channel` | 세목 분류 알림 | AI 경비 자동분류 결과 | 구현 완료 |
| `tax_calendar_channel` | 세금 캘린더 알림 | 마감일 리마인드 | 구현 완료 |
| `payment_channel` | 결제 알림 | QR 결제 완료/실패 | 구현 완료 |

---

## 5. FCM 메시지 타입 현황

| type | 처리 | 알림 내용 |
|------|------|----------|
| `classification` | 구현 완료 | "결제가 확인되었어요" + 가맹점/금액/AI 추천 경비 |
| `tax_calendar` | 구현 완료 | "세금 D-Day" + 마감일 안내 |
| `payment` | 구현 완료 | "결제 완료/실패" + 가맹점/금액 |
| 기타 | 기본 처리 | notification body 그대로 표시 |

---

## 6. 알림 탭 시 화면 이동

| 알림 타입 | 탭 시 이동 |
|-----------|-----------|
| classification | AI 로딩 → 경비 분류 결과 화면 |
| tax_calendar | 세금 캘린더 화면 |
| payment | 홈 화면 |

### 앱 상태별 동작
- **앱 종료 상태** → PIN 인증 → 홈 → 목적 화면 이동
- **앱 실행 중** → `onNewIntent`로 바로 목적 화면 이동

### 관련 파일
- `MainActivity.kt` — `onNewIntent()`, `pendingNavigateTo`
- `NavGraph.kt` — Main composable에서 `pendingNavigateTo` 처리

---

*작성일: 2026-03-27*
