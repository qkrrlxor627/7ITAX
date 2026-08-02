# 7iTAX 작업 기록

## 2026-06-05: 사용 불가 외부 API → Mock/Stub 전환

### 배경
SSAFY 교육 과정 종료 후 외부 API(OAuth, SMS, FCM, AI)에 접근 불가하여 로컬 구동 불가 상태.
각 외부 API를 Mock/Stub으로 대체하여 앱이 정상 동작하도록 전환.

### 작업 내역

#### 1. Solapi SMS → MockSmsSender 생성
- **신규**: `BE/src/main/java/com/ssafy/tax7i/sms/service/MockSmsSender.java`
  - `SmsSender` 인터페이스 구현
  - 실제 SMS 발송 대신 콘솔 로그로 OTP 출력
  - `sms.mock.enabled=true`일 때 활성화
- **수정**: `BE/src/main/java/com/ssafy/tax7i/sms/service/SolapiSmsSender.java`
  - `@ConditionalOnProperty(name = "sms.mock.enabled", havingValue = "false", matchIfMissing = true)` 추가
  - Mock 모드가 아닐 때만 실제 Solapi 구현체 활성화

#### 2. Anthropic Claude API → MockAiServiceClient 생성
- **신규**: `BE/src/main/java/com/ssafy/tax7i/ai/client/MockAiServiceClient.java`
  - `AiServiceClient` 상속, 모든 메서드 오버라이드
  - `chat()` → "현재 AI 서비스가 Mock 모드입니다" 고정 응답
  - `classifyTransaction()` → "기타경비", confidence 0.5 기본 분류
  - `getChatHistory()` → 빈 히스토리
  - `ai.mock.enabled=true`일 때 활성화
- **수정**: `BE/src/main/java/com/ssafy/tax7i/ai/client/AiServiceClient.java`
  - `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false", matchIfMissing = true)` 추가

#### 3. SSAFY OAuth 설정 정리
- **수정**: `BE/src/main/resources/application.yaml`
  - `ssafy.oauth` 설정 블록 주석처리 (실제 호출 코드가 없어 설정만 정리)
  - 인증은 이미 `NiceIdentityMockService` + PIN 기반으로 동작

#### 4. Firebase FCM — 변경 없음
- 이미 `fcm.enabled: ${FCM_ENABLED:false}`로 비활성화 기본값
- `FcmService`가 `Optional<FirebaseMessaging>` 사용하여 graceful degradation 구현됨
- 별도 수정 불필요

#### 5. application-local.yaml 생성
- **신규**: `BE/src/main/resources/application-local.yaml`
  - `app.test-login.enabled=true` (SMS 본인인증 우회)
  - `sms.mock.enabled=true` (MockSmsSender 활성화)
  - `ai.mock.enabled=true` (MockAiServiceClient 활성화)
  - `fcm.enabled=false`
  - DB: localhost PostgreSQL (ssafy/ssafy)
  - Redis: localhost (password: ssafy)
  - 로컬 전용 AES/JWT 시크릿

#### 6. Android API URL 로컬 전환
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/util/Constants.kt`
  - DEBUG 모드 API_BASE_URL: `https://j14c203.p.ssafy.io/api/` → `http://10.0.2.2:8080/api/`
  - DEBUG 모드 WEBVIEW_BASE_URL: `https://j14c203.p.ssafy.io` → `http://10.0.2.2:3000`
  - RELEASE 모드는 기존 URL 유지

#### 7. Android 기존 빌드 에러 수정 (기존 코드 불일치)
- **신규**: `FE/app/src/main/java/com/ssafy/seveniTax/data/repository/ClassificationRepository.kt`
  - 누락된 인터페이스 추가 (Hilt DI 바인딩 실패 원인)
- **신규**: `FE/app/src/main/java/com/ssafy/seveniTax/data/repository/ClassificationRepositoryImpl.kt`
  - 누락된 구현체 추가
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/ui/main/MainScreen.kt`
  - `onQrPaymentClick` 파라미터 제거 (HomeScreen에 해당 파라미터 없음)
  - `when` 분기에 `BottomTab.AI` 추가 (exhaustive 에러)
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/ui/navigation/NavGraph.kt`
  - `ClassificationResultScreen`, `BulkClassificationLoadingScreen`에서 `classificationViewModel` 파라미터 제거 (해당 Composable에 없는 파라미터)
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/viewmodel/ClassificationViewModel.kt`
  - `confidenceScore` → `confidence` (ClassificationResponse 필드명 불일치)
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/ui/book/ExportScreens.kt`
  - `exportVatPdf` → `exportVat`, `exportIncomeTaxPdf` → `exportIncomeTax`, `exportLocalTaxPdf` → `exportLocalTax`, `exportLocalTaxExcel` → `exportLocalTax` (ExportViewModel 실제 함수명에 맞춤)
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/ui/main/BottomTabBar.kt`
  - `ic_ai_robot` → `ic_ai_sparkle` (누락된 리소스)
- **수정**: `FE/app/src/main/java/com/ssafy/seveniTax/ui/ai/AiScreen.kt`
  - `ic_ai_robot` → `ic_ai_sparkle` (누락된 리소스)

### 빌드 결과
- **BE**: BUILD SUCCESSFUL (Java 17, Spring Boot 3.5)
- **FE**: BUILD SUCCESSFUL (Android debug APK)

### 로컬 구동 방법
```bash
# 1. DB/Redis 구동
docker-compose up -d postgres redis

# 2. 백엔드 구동 (local 프로필)
cd BE
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

# 3. Android 앱 실행 (Android Studio에서 DEBUG 모드)
```

### 변경된 파일 목록
| 파일 | 변경 유형 |
|------|----------|
| `BE/src/main/java/com/ssafy/tax7i/sms/service/MockSmsSender.java` | 신규 |
| `BE/src/main/java/com/ssafy/tax7i/sms/service/SolapiSmsSender.java` | 수정 |
| `BE/src/main/java/com/ssafy/tax7i/ai/client/MockAiServiceClient.java` | 신규 |
| `BE/src/main/java/com/ssafy/tax7i/ai/client/AiServiceClient.java` | 수정 |
| `BE/src/main/resources/application.yaml` | 수정 |
| `BE/src/main/resources/application-local.yaml` | 신규 |
| `FE/app/src/main/java/com/ssafy/seveniTax/util/Constants.kt` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/data/repository/ClassificationRepository.kt` | 신규 |
| `FE/app/src/main/java/com/ssafy/seveniTax/data/repository/ClassificationRepositoryImpl.kt` | 신규 |
| `FE/app/src/main/java/com/ssafy/seveniTax/ui/main/MainScreen.kt` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/ui/navigation/NavGraph.kt` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/viewmodel/ClassificationViewModel.kt` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/ui/book/ExportScreens.kt` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/ui/main/BottomTabBar.kt` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/ui/ai/AiScreen.kt` | 수정 |
