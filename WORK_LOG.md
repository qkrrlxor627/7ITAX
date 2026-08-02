# 7iTAX 작업 기록

> 작업을 완료하거나 저장소에 영향을 주는 행동을 했을 때 이 문서에 최신 항목을 맨 위에 추가한다.
> 형식: `## YYYY-MM-DD: 제목` → 배경 / 작업 내역 / 결과 / 변경된 파일 목록.

---

## 2026-08-02: PR #2 머지 및 머지 후 검증

### 배경
`feat/local-dev-update` 브랜치로 올린 작업(아래 항목)에 대해 사용자가 PR #2를 생성·머지했다.
머지 결과가 의도대로 반영됐는지 점검한 기록.

### 점검 내역
- **머지 커밋**: `ab24bca` (`66ab757` + `6cf2192` 2-부모 머지 커밋). `main`이 `66ab757` → `ab24bca`로 이동
- **트리 동일성**: `git diff ab24bca 6cf2192` 결과 없음 → 머지 과정에서 내용 변형·충돌 해결 없이 그대로 반영됨
- **커밋 6개 전부 `main`에 포함** 확인 (`d778373`, `a1672c6`, `c65530b`, `f40120d`, `c0b813b`, `6cf2192`)
- **신규 파일 존재 확인**: MockAiServiceClient, MockSmsSender, AuditLogAspect,
  AdditionalAuthLoginRequest, ClassificationRepository, `monitoring/`, `monitoring.md`, `WORK_LOG.md`
- **테스트 재실행** (머지된 `main`에서 `cleanTest test` 강제 실행): 308개 통과, 실패 0 / 오류 0 / 스킵 1
- **위생 점검**: `.claude-screenshots/` 미추적 확인, 시크릿 스캔 결과 하드코딩된 키 없음

### 발견 사항
- **`docs/samples/`는 테스트 산출물이다.** `BE/src/test/java/com/ssafy/tax7i/export/SampleExportGenerator.java:19`가
  `../docs/samples`에 파일을 직접 쓰기 때문에, `./gradlew test`를 돌릴 때마다 워킹 트리가 더러워진다.
  최초에 이 파일들이 "수정됨"으로 보였던 것도 같은 이유. 커밋에서 제외한 판단이 결과적으로 맞았다.
  → 개선하려면 출력 경로를 빌드 디렉터리로 바꾸거나, 샘플 생성을 별도 태스크로 분리할 것

### 남은 작업
- **`feat/local-dev-update` 브랜치 정리** — PR #2 머지 완료로 역할이 끝났다. 로컬·원격 모두 삭제 가능
- `application-local.yaml` 미커밋 이슈는 아래 항목 참조 (여전히 유효)

---

## 2026-08-02: 미커밋 로컬 작업 GitHub 반영 (`feat/local-dev-update`)

### 배경
클론 시점(`649afe2`) 이후 진행한 작업이 커밋되지 않은 채 워킹 트리에 50개 변경으로 쌓여 있었다.
그 사이 원격 `main`은 PR #1(`feat/local-runnable`)이 머지되어 `66ab757`로 앞서 있는 상태였다.
기존 원격 내용을 덮어쓰지 않고("기존 값 보존 + 신규 추가") 로컬 작업을 반영하는 것이 목표.

### 작업 내역

#### 1. 원격 상태 확인 (선행)
- `origin/main`이 로컬보다 4개 커밋 앞섬 — 결제 전용 PIN(`payPinHash`), AI 규칙기반 폴백, 로컬 실행 복구 등
- PR #1은 **이미 머지 완료** 상태 → 중복 작업 없음을 확인
- 양쪽이 함께 수정한 파일 5개 식별: `User.java`, `SecurityConfig.java`, `SolapiSmsSender.java`, `application.yaml`, `docker-compose.yml`

#### 2. 브랜치 전략
- `feat/local-dev-update` 브랜치를 `649afe2`에서 생성 → 주제별 커밋 5개 작성 → `origin/main`에 rebase
- `main` 직접 푸시 및 force push 없음. 원격 `main`(`66ab757`)과 `feat/local-runnable`(`72789ee`)은 변경되지 않음

#### 3. 커밋 분리 (5개)
| 커밋 | 내용 |
|------|------|
| `d778373` | 외부 API Mock 구현으로 로컬 단독 구동 지원 |
| `a1672c6` | 추가인증 기반 로그인 플로우 도입 |
| `c65530b` | AOP 기반 감사 로그 추가 |
| `f40120d` | Prometheus/Grafana/Alertmanager 모니터링 스택 추가 |
| `c0b813b` | 세목분류 리포지토리 분리 및 작업 문서 추가 |

#### 4. rebase 충돌 해결 (2건)
- **`User.java`** — 원격의 `setupPayPin()`/`hasPayPin()`과 로컬의 `registerDevice()`가 인접 위치에서 충돌.
  양쪽 메서드를 **모두 유지**. 필드 `payPinHash`(51행), `deviceId`(56행) 모두 선언 확인
- **`application.yaml`** — 로컬 작업본은 `ssafy.oauth` 블록을 전체 주석 처리했으나,
  원격이 도입한 기본값 방식(`${SSAFY_CLIENT_ID:}`)을 **채택**. 설정 정보를 보존하면서 동일하게 로컬 기동 가능.
  모니터링용 `management` 블록(Actuator/Prometheus 노출)만 추가로 병합
- 나머지 3개 파일(`SecurityConfig.java`, `SolapiSmsSender.java`, `docker-compose.yml`)은 자동 병합

#### 5. 저장소에서 제외한 항목
- **`.claude-screenshots/`** — 에이전트 작업용 스크린샷·UI 덤프 35개(3MB). `.gitignore`의 `# personal` 블록에 규칙 추가.
  기존 `.claude` 패턴은 `.claude-screenshots`를 매칭하지 않아 별도 규칙이 필요했음
- **`docs/samples/` 재생성본** — CSV 4개는 `origin/main`의 파일과 blob 해시까지 동일,
  PDF/XLSX 3개는 재생성으로 바이트만 다름. 커밋 시 의미 없는 바이너리 diff만 남아 제외

#### 6. 모니터링 스택 반영
- `monitoring/` 설정(prometheus, alertmanager, grafana provisioning) + `monitoring.md` 커밋
- `docker-compose.yml`에 prometheus/alertmanager/grafana/webhook-logger 서비스 추가 (+62줄, 전부 추가형)
- `BE/build.gradle`에 `spring-boot-starter-actuator`, `micrometer-registry-prometheus` 의존성 추가

### 결과
- **BE 테스트**: 308개 전부 통과 (실패 0, 오류 0, 스킵 1)
  - 변경 영역 커버: `AuthControllerTest`(12), `AuthServiceTest`(17), `PayPinServiceTest`(8),
    `PaymentControllerTest`(9), `TransferControllerTest`(8), `TaxClassificationAiFallbackTest`(4)
- **FE**: `testDebugUnitTest` BUILD SUCCESSFUL — 수정한 Compose 화면·ViewModel·리포지토리 전부 컴파일 확인
  (단, 테스트 소스는 템플릿 예제 2개뿐이라 실질 커버리지는 없음)
- **AI**: 이 브랜치가 `ai/`를 변경하지 않아 미실행 (pytest 미설치)
- 시크릿 스캔: 커밋 diff에 실제 키/비밀번호 없음 — 설정은 모두 `${ENV_VAR}` 참조
- 푸시 완료: `origin/feat/local-dev-update` = `c0b813b` (로컬 HEAD와 일치)

### 남은 작업 / 알아둘 점
- **PR 미생성** — `gh` CLI가 이 환경에 없어 브랜치 푸시까지만 완료.
  https://github.com/qkrrlxor627/7ITAX/pull/new/feat/local-dev-update 에서 생성 필요
- **`application-local.yaml`은 커밋되지 않음** — `BE/.gitignore:44`로 제외돼 있어
  저장소를 클론한 사람에게는 따라오지 않는다. 로컬 프로필 구동 시 직접 작성해야 함
- **커밋 단위 미세 불일치** — Actuator 의존성 2줄과 `application.yaml`의 `management:` 블록은
  성격상 모니터링 커밋(`f40120d`)에 속하지만 첫 커밋(`d778373`)에 포함됨. 최종 트리는 동일

### 변경된 파일 목록
| 파일 | 변경 유형 |
|------|----------|
| `BE/src/main/java/com/ssafy/tax7i/auth/dto/AdditionalAuthLoginRequest.java` | 신규 |
| `BE/src/main/java/com/ssafy/tax7i/global/audit/AuditLogAspect.java` | 신규 |
| `BE/src/main/java/com/ssafy/tax7i/global/audit/Auditable.java` | 신규 |
| `BE/src/main/java/com/ssafy/tax7i/auth/domain/User.java` | 수정(충돌 병합) |
| `BE/src/main/resources/application.yaml` | 수정(충돌 병합) |
| `BE/src/main/java/com/ssafy/tax7i/auth/controller/AuthController.java` | 수정 |
| `BE/src/main/java/com/ssafy/tax7i/auth/service/AuthService.java` | 수정 |
| `BE/src/main/java/com/ssafy/tax7i/config/SecurityConfig.java` | 수정 |
| `BE/src/main/java/com/ssafy/tax7i/payment/controller/PaymentController.java` | 수정 |
| `BE/src/main/java/com/ssafy/tax7i/transfer/controller/TransferController.java` | 수정 |
| `BE/build.gradle` | 수정 |
| `FE/app/src/main/java/com/ssafy/seveniTax/data/repository/ClassificationRepository.kt` | 신규 |
| `FE/app/src/main/java/com/ssafy/seveniTax/data/repository/ClassificationRepositoryImpl.kt` | 신규 |
| `FE/webview/pnpm-lock.yaml` | 신규 |
| `monitoring/` (prometheus·alertmanager·grafana 설정 6개) | 신규 |
| `monitoring.md`, `todoerror.md`, `wehave0702.md` | 신규 |
| `docker-compose.yml` | 수정 |
| `.gitignore` | 수정 |

---

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
