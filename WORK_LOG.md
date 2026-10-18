# 7iTAX 작업 기록

> 작업을 완료하거나 저장소에 영향을 주는 행동을 했을 때 이 문서에 최신 항목을 맨 위에 추가한다.
> 형식: `## YYYY-MM-DD: 제목` → 배경 / 작업 내역 / 결과 / 변경된 파일 목록.

---

## 2026-10-18: 포트폴리오 질문 대비 — 세금·PDF·분류/RAG 구현 실태 조사 (코드 무수정)

### 배경
포트폴리오 문답 3건(04 직접 개발한 BE 기능, 05 PDF 한글 깨짐 해결, 06 규칙 기반 vs RAG·LLM 적용 위치)에 대해 코드 근거를 확인했다.
git 이력은 `Initial commit` + 본인 커밋뿐이라 **팀원별 기여는 저장소로 증명할 수 없다** — 아래는 "구현 여부"까지만 확인한 내용이다.

### 작업 내역
- 세금: `TaxCalculationEngine.java:32-164`(DB 누진세율 구간 → 산출세액·세액공제·경비조정), `TaxController.java:48-168`(계산·신고 CRUD·제출·납부·PDF·영수증), `VatController`, `TaxSavingService`
- PDF: `PdfService.java:38,55`(신고서·납부 영수증), `SimpleLedgerPdfExporter`, 엑셀 `VatExcelExporter`·`IncomeTaxExcelExporter`
- 한글 글꼴: `ExcelExportHelper.java:63-88`이 classpath의 `fonts/NanumGothic-{Regular,Bold}.ttf`를 `useFont(..., "NanumGothic", 400/700, subset=true)`로 등록하고,
  템플릿 3개(`templates/pdf/{simple-ledger,tax-receipt,tax-return}.html:11`)가 `font-family: 'NanumGothic'`. 리소스 누락 시 예외(`:84`). 증상 발견 시점은 기록 없음
- DB: `db/init/01_schema.sql`(CREATE TABLE 22개), `02_seed_data.sql`, `db/migration/V2·V3`. **Flyway 미도입** — `build.gradle`에 의존성 없음, `application.yaml:22` `ddl-auto: update`
- 분류: `TaxClassificationService.java:52-145` 학습 이력 → MCC Tier A → 금액조건 → 가맹점 키워드 → (룰 없음/미분류 시) AI → 기본값.
  AI 결과 점수 상한 89로 CONFIRMED 방지(`AiClassificationService.java:43`)
- AI: fine-tuned 모델 경로 `ai/models/tax_classifier`(`tax_classifier_service.py:18`)가 **저장소에 없음**(`ls ai/models` 실패) → 실제로는 `rule_based_classifier.py` 키워드 폴백(20세목)이 동작.
  분류 근거 설명은 RAG+LLM(`explanation_service.py`), 챗봇은 임베딩 의도분류 → BM25+벡터(bge-m3/Chroma) → bge-reranker-v2-m3 → 모델 티어 선택(`chat_service.py:74-150`)

### 결과
- 04 답변 근거: 세금 계산·신고·납부, DB 스키마, 증빙 PDF 모두 구현 확인. 단 "Flyway로 관리"는 사실과 다름
- 05: 원인은 openhtmltopdf 기본 PDF 글꼴에 한글 글리프 없음 + 서버/컨테이너에 한글 시스템 글꼴 부재로 추정(코드에는 수정 결과만 존재)
- 06: 구현 = MCC/키워드 규칙 분류, AI 추천 분류(현재 키워드 폴백), RAG 근거 설명, RAG 챗봇. 설계만 = fine-tuned 분류 모델, 개인화(INT-1)

### 확인했으나 이번 범위에서 제외한 항목
- `SampleExportGenerator`로 실제 PDF를 생성해 한글 출력을 눈으로 검증하는 작업은 하지 않았다
- 작업 트리의 기존 미커밋 변경(`AuthService`, `SecurityConfig`, `TestUserProvisioner` 등)은 이 세션 이전 것으로, 이번 항목과 무관하다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `WORK_LOG.md` | 이 항목 추가 |

---

## 2026-10-18: Swagger 도입 + 간편 로그인(BE·FE) + 앱 QR 결제 "만료" 오표시 수정 + 에뮬레이터 실검증

### 배경
사용자 요청: BE에 Swagger가 없고, 본인인증·PIN·SMS 흐름 때문에 로컬·에뮬레이터에서 사람을 바꿔 가며 확인하기 어렵다 →
"로그인을 단순화해 사람 간 구분만 되게". 에뮬레이터 확인 중 앱 QR 결제가 성공해도 "만료"로 뜨는 버그와 FE 빌드 불가 문제를 함께 발견했다.

### 작업 내역
- **Swagger**: `springdoc-openapi-starter-webmvc-ui:2.8.9`, `OpenApiConfig`(JWT bearer Authorize), `SecurityConfig`에 `/swagger-ui/**`·`/v3/api-docs/**` permitAll,
  `application.yaml` `springdoc.*.enabled: ${SWAGGER_ENABLED:true}`(`.env.example`에 `SWAGGER_ENABLED=true`, 운영 false)
- **BE 간편 로그인** `POST /api/auth/simple-login {phoneNumber, name?, cardType?}` — `app.test-login.enabled`(TEST_LOGIN_ENABLED)일 때만.
  전화번호가 같은 사용자가 있으면 그 사용자(본인인증 가입자 포함), 없으면 ci `test-ci-{번호}`로 생성 + 카드가 없으면 계좌(500만원)·카드(기본 BUSINESS) 발급
  - 시드 로직을 `auth/service/TestUserProvisioner`로 추출(`findOrCreate`, `provisionCardIfAbsent`), `CardDataInitializer`는 이를 호출(동작 동일)
- **FE 간편 로그인**: `BuildConfig.SIMPLE_LOGIN`(debug true / release false), `SimpleLoginScreen`(시드 2명 바로가기 + 번호·이름 입력, 마지막 번호 자동 입력),
  Splash가 DEBUG면 이 화면으로. `AuthRepository.simpleLogin`이 세션을 비우고 토큰·사용자 정보 저장 후, **페이 가입 플래그를 서버 결제 PIN 등록 여부로 복원**
  (로컬 플래그만 쓰면 로그인할 때마다 페이 재가입 + 계좌가 계속 늘어남)
- **QR 결제 오표시 수정**: `confirmQrPayment`가 토큰을 `getAndDelete`로 소비 → 결제자 앱의 상태 폴링(`GET /qr/token/{t}/status`)이 `QR_TOKEN_EXPIRED`(404/410)를 받아
  `PaymentViewModel.startPollingStatus`가 "QR 토큰이 만료되었습니다"로 표시(서버는 CAPTURED). 소비 시 `qr-pay-done:{token}`(TTL 5분)을 남기고 상태 조회만 이를 본다
  (확정 경로는 원 토큰만 보므로 중복 결제 방어 유지)
- **FE 빌드 불가 수정**: 루트 `.gitignore`의 `*.jar`가 `FE/gradle/wrapper/gradle-wrapper.jar`를 막아 클론본에서 `./gradlew`가 `Unable to access jarfile`.
  `!**/gradle/wrapper/gradle-wrapper.jar` 예외 추가 + BE의 wrapper jar 복사(wrapper jar는 properties의 배포판 9.3.1을 받는다)
- `scripts/confirm_latest_qr.py`: 에뮬레이터 1대 확인용 가맹점 역할(Redis 최신 `qr-pay:*` 토큰을 확정)
- 테스트 6개 추가: `AuthServiceTest` simpleLogin 3, `AuthControllerTest` 200/400 2, `PaymentServiceTest` 토큰 소비 후 상태 CAPTURED 1(+ `confirmQrPayment_성공`에 done 키 검증)

### 결과
- `./gradlew test` **324 통과 / 0 실패 / skipped 1**(41 클래스)
- Swagger: `/swagger-ui/index.html` 200, `/v3/api-docs` 200 — paths 85, `/api/auth/simple-login` 포함, securitySchemes `bearerAuth`
  - 첫 `docker compose up -d --build backend`는 `./gradlew dependencies` 단계에서 일시적으로 실패해 **예전 컨테이너가 계속 떠 있었다**(swagger 403). 재빌드로 정상화. 빌드 실패가 `up -d` 출력에 묻히므로 이미지 생성 시각을 확인할 것
- 간편 로그인(API): 01022222222 → userId 2 이싸피 기존 BUSINESS 카드 / 01033333333+"박싸피" → 신규 36, BUSINESS 카드, 잔액 5,000,000 / 재로그인 → 같은 36·new=false / 01044444444+PERSONAL → 신규 37 PERSONAL
- 빈 DB 시드(리팩터링 검증): 같은 postgres에 `tax7i_fresh` DB를 만들고 이미지로 임시 백엔드(18080) 기동 → 김싸피 PERSONAL·이싸피 BUSINESS 발급 로그,
  `smoke_payment_ledger.py http://localhost:18080` → `OK: paymentId=1 ... bookEntryId=1`. 임시 컨테이너·DB 삭제
- **에뮬레이터 실검증**(Nexus_5_API_29, 커맨드라인 빌드 `assembleDebug` 10분 35초, JDK 23으로 Gradle 구동): 간편 로그인 화면 → 이싸피 바로가기 → 홈 "안녕하세요, 이싸피님".
  새 번호 01055555555 → "사용자5555" 신규, 홈 수입·비용 0(사용자 분리 확인) → 페이 가입(약관·결제 PIN, 외부 인증 없음) → QR 결제 탭 →
  수정 전: 서버 CAPTURED(paymentId 57)인데 앱 "결제 실패 — QR 토큰이 만료되었습니다" / 수정 후: paymentId 58 → 앱 증빙 추가 화면으로 진행 → 홈 미분류 경비 2건·비용 -2,000.
  `confirm_latest_qr.py` → paymentId 59 CAPTURED

### 확인했으나 이번 범위에서 제외한 항목
- 앱 QR 결제 금액·가맹점이 고정(`QrPaymentScreen.kt` 1,000원·첫 가맹점) — CLAUDE.md 미해결 항목에 추가
- SMS가 필요한 앱 경로(카드 직접 등록 OTP, 신규 기기 추가 인증)는 `SOLAPI_*` 없으면 실패한다(`sms.mock.enabled` 설정 키가 yaml에 없음). 간편 로그인이 카드를 자동 발급하므로 이번엔 우회
- Android Studio가 2020 버전(build 202)이라 이 프로젝트(compileSdk 36)를 열 수 없다 → 커맨드라인 빌드로 대체
- FE 자동 테스트는 여전히 없음(에뮬레이터 수동 확인)

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/build.gradle`, `BE/.../config/OpenApiConfig.java`(신규), `config/SecurityConfig.java`, `resources/application.yaml`, `.env.example` | Swagger |
| `BE/.../auth/service/TestUserProvisioner.java`(신규), `auth/service/AuthService.java`, `auth/controller/AuthController.java`, `auth/dto/SimpleLogin{Request,Response}.java`(신규), `config/CardDataInitializer.java` | 간편 로그인·시드 리팩터링 |
| `BE/.../payment/service/PaymentService.java` | QR 상태 조회 done 키 |
| `BE/src/test/.../auth/*Test.java`, `payment/service/PaymentServiceTest.java` | 테스트 6개 |
| `FE/app/build.gradle.kts`, `FE/.../ui/auth/SimpleLoginScreen.kt`(신규), `ui/auth/SplashScreen.kt`, `ui/navigation/{Route,NavGraph}.kt`, `data/remote/AuthApi.kt`, `data/repository/AuthRepository{,Impl}.kt`, `data/model/auth/SimpleLogin{Request,Response}.kt`(신규), `viewmodel/AuthViewModel.kt` | FE 간편 로그인 |
| `FE/gradle/wrapper/gradle-wrapper.jar`(신규), `.gitignore` | FE 빌드 가능 |
| `scripts/confirm_latest_qr.py`(신규) | 가맹점 역할 |
| `CLAUDE.md`, `261018.md`, `WORK_LOG.md` | 갱신 |

---

## 2026-10-18: 결제 취소 버그 2건(BUG-A·B) + AI 챗봇 503 수정

### 배경
바로 아래 항목의 시나리오 점검에서 나온 실패 3건(S3·S9·S10)을 사용자 지시로 모두 수정했다. 브랜치 `fix/payment-cancel-and-chatbot`.

### 작업 내역
- **BUG-B(부분취소 전액 환불)**: `CardLedgerTransaction`에 `cancelledAmount`(nullable) + `cancel(long)`(남은 금액 상한, 0이면 CANCELLED) + `getRemainingBalance()`.
  `SsafyCreditCardClient.cancelTransaction(..., cancelAmount)` 신설(취소액만 환불), `deleteTransaction`은 이에 위임. 청구서 월합계는 남은 금액 기준.
  `PaymentService.cancel`이 `deleteTransaction` 대신 `cancelTransaction` 호출
- **BUG-A(취소 장부 잔존)**: `PaymentCancelledEvent`에 `remainingAmount` 추가, `BookEntryEventListener.handlePaymentCancelled` →
  `BookEntryService.applyPaymentCancel`(전액이면 삭제, 부분이면 `BookEntry.applyPartialCancel`로 금액·부가세 비율 조정, `entertainmentUsed` 캐시 무효화)
- **챗봇**: 메인 모델 기본값 `claude-opus-4-8` → `claude-sonnet-4-6`(`ai/app/core/config.py`, `docker-compose.yml`, `ai/.env.example`, `ai/README.md`, `exec/포팅_매뉴얼.md`).
  `ai/app/services/`(보호 경로)는 무수정. BE `ai.read-timeout` 10 → `${AI_READ_TIMEOUT:120}`(`.env.example`에 키 추가), FE `ApiClient.kt` readTimeout 30 → 120초
- 테스트 8개 추가: `CardLedgerTransactionTest`(3), `PaymentServiceTest` 부분취소·이벤트 검증(1, 기존 `cancel_성공`은 `cancelTransaction` 스텁으로 갱신), `BookEntryServiceTest`(3), `BookEntryEventListenerTest`(1)
- `CLAUDE.md` 테스트 기준선 310 → 318, `261018.md` 8절 추가

### 결과
- 모델 선택 근거(AI 컨테이너에서 raw API 호출, `temperature=0.7`): opus-4-8 **400** `temperature is deprecated`, opus-4-6·sonnet-4-6·haiku-4-5 200.
  opus-4-6은 비교·계산형 질문이 29.3 / 90.5초 / `tenacity.RetryError[LLMTimeoutError]`(AI 내부 LLM 타임아웃 30초 초과), sonnet-4-6은 17.7 / 16.2 / 20.5초
- BE 타임아웃: 수정 전 `Read timed out`(10초), 수정 후 BE 경유 25.6초(haiku)·20.2초(sonnet) 응답 성공
- `./gradlew test` **318 통과 / 0 실패 / skipped 1**(41 클래스). `./scripts/verify_stack.sh` → `OK: backend healthy + 결제→장부 스모크 통과`. `docker compose config -q` 통과
- E2E 시나리오 **10/10 PASS**: S3 전액 취소 → 환불 33,000·장부 GET 404, S9 33,000원 중 11,000 부분취소 → 환불 11,000·결제 CAPTURED·장부 expense 22,000/vat 2,000 → 잔액 취소 → 누적 33,000·CANCELLED·장부 404, S10 챗봇 통과

### 확인했으나 이번 범위에서 제외한 항목
- 확정 직후(장부 생성 비동기 리스너 완료 전) 취소 시 경합: 취소 리스너가 장부를 못 찾고 뒤늦게 생성된 장부가 남을 수 있다
- 카드 거래 내역 API(`getTransactionHistory`)는 원 결제금액을 그대로 보여 준다(부분취소액 미표시)
- FE 타임아웃 변경은 Android 빌드 미확인. `chat_service.py`의 `temperature`·30초 타임아웃 자체는 보호 경로라 그대로
- 스모크 취소 경로 추가, 401/403, JWT `jti`, INT-1

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/.../card/entity/CardLedgerTransaction.java` | 누적 취소액·부분취소 |
| `BE/.../banking/client/SsafyCreditCardClient.java` | `cancelTransaction` 신설, 청구서 남은 금액 |
| `BE/.../payment/service/PaymentService.java`, `payment/event/PaymentCancelledEvent.java` | 부분취소 원장 호출, `remainingAmount` |
| `BE/.../bookentry/entity/BookEntry.java`, `service/BookEntryService.java`, `event/BookEntryEventListener.java` | 취소 장부 반영 |
| `BE/src/main/resources/application.yaml`, `.env.example` | `AI_READ_TIMEOUT` |
| `BE/src/test/...` (4개, 1개 신규) | 테스트 8개 |
| `ai/app/core/config.py`, `ai/.env.example`, `ai/README.md`, `docker-compose.yml`, `exec/포팅_매뉴얼.md` | 메인 모델 sonnet-4-6 |
| `FE/.../data/remote/ApiClient.kt` | readTimeout 120초 |
| `CLAUDE.md`, `261018.md`, `WORK_LOG.md` | 갱신 |

---

## 2026-10-18: 카드 직접 결제 read-only 트랜잭션 버그 수정 + 대표 기능 시나리오 E2E 점검

### 배경
PRE_CICD 0~9단계 머지(`224c7d5`) 뒤 `CardService.java` 수정이 미커밋으로 남아 있었다. 이를 마무리하고, CI/CD 전 완성도를 실제 스택에서 대표 시나리오로 확인했다. 요약 문서는 `261018.md`.

### 작업 내역
- `CardService.payment()`/`cancelPayment()`에 `@Transactional` — 클래스 기본값 `readOnly = true`에 `SsafyCreditCardClient`의 `@Transactional`(REQUIRED)이 합류해 출금계좌 `PESSIMISTIC_WRITE` 조회가 실패하던 문제
- compose 스택 대상 E2E 시나리오 9개 실행(스크립트는 세션 scratchpad, 저장소 미반영)
- `261018.md` 신규: 진행 상태·DoD 판정·시나리오 결과·신규 버그·남은 일

### 결과
- 수정 전 이미지(`git stash` 후 `docker compose up -d --build backend`): `POST /api/cards/2/payment` → **HTTP 500**, 로그 `ERROR: cannot execute SELECT FOR NO KEY UPDATE in a read-only transaction`.
  수정 후: 12,000원 차감 → `/transactions` 노출 → 취소 후 원복. H2 테스트에서는 재현되지 않는다
- DoD: `./gradlew test` **310 통과 / 0 실패 / skipped 1**(40 클래스), `./scripts/verify_stack.sh` exit 0, `docker compose config -q` 통과
- GitHub Actions(공개 API 조회): PR #4·main push 모두 success — be-test 147초, ai-test 87초, stack-smoke 124초(8단계 `[측정: ___]` 값)
- BE 시나리오 S1~S9 **7 PASS / 2 FAIL**: 인증·사업자 결제→장부→세목 수정·확정·카드 직접 결제·개인카드 장부 미생성·P2P·세액 추정/신고서·내보내기 통과
- **신규 버그 BUG-A**: 결제 취소 시 장부가 남는다. `PaymentCancelledEvent`(`PaymentService.java:159`) 리스너 0건. 취소된 payment #2·#36의 장부가 미확정 경비 5,000·33,000원으로 잔존
- **신규 버그 BUG-B**: 부분 취소가 전액 환불된다. `PaymentService.java:150-153`이 취소 금액과 무관하게 `deleteTransaction`(원장 거래 전체 취소) 호출. 30,000원 결제 → 10,000원 부분취소 → 계좌 +30,000, 결제 CAPTURED·`cancelledAmount=10000`
- **신규 버그 BUG-C(AI 챗봇)**: `docker compose up -d --build ai` → healthy, `/api/v1/health/ready` 200인데 `POST /api/chatbot` → **503** `AI_SERVICE_UNAVAILABLE`.
  AI 로그 `anthropic.BadRequestError 400 '`temperature` is deprecated for this model.'` — `ai/app/services/chat_service.py:46-49`가 `claude-opus-4-8`에 `temperature=0.7` 전달.
  같은 로그에 INT-1 재현(`Illegal header value b'Bearer '` → 개인화 생략). AI 로직 보호 경로라 미수정
- 시나리오 최종 **7 PASS / 3 FAIL**(S10 챗봇 포함)
- 동작 차이 기록: 미인증 요청은 401이 아니라 **403**(`AuthenticationEntryPoint` 미설정, QR pay 미인증도 403 — 2026-08-04 구역 [4]의 "401이 정상" 서술과 다름). JWT에 `jti`가 없어 같은 초 재로그인 시 동일 토큰 → 로그아웃 직후 재로그인 403. `/api/export/income-tax/pdf`는 간편장부 PDF 별칭(`ExportController.java:72-84`)

### 확인했으나 이번 범위에서 제외한 항목
- BUG-A·BUG-B 수정(취소 장부 처리 방식·부분 환불 원장 연산은 도메인 결정 필요), 스모크에 취소 경로 추가, 401/403 정리, `jti` 추가
- gradle test가 다시 쓴 `docs/samples/*` 6개는 `git checkout -- docs/samples`로 되돌림

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/src/main/java/com/ssafy/tax7i/card/service/CardService.java` | `payment`/`cancelPayment`에 `@Transactional` |
| `261018.md` | 신규 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 9단계 — 하네스(훅·검증 에이전트) + CLAUDE.md 갱신

### 배경
CLAUDE.md 규칙(WORK_LOG 필수, `command grep`, AI 로직 불변)이 문서에만 있어 지켜지는지 사람이 확인해야 했다(`docs/PRE_CICD.md` 9단계).

### 작업 내역
- **훅을 bash+jq 대신 Python으로 작성**(사용자 결정). 이 PC에 `jq`가 없고, 지시서 866행 경고대로 bash 접두어 비교는 `C:\...` 경로를 놓친다.
  `.claude/hooks/{_common,protect_paths,guard_bash,compile_check,check_done}.py` — 지시서 910-999행 bash 로직을 이식
  - 경로: `os.path.relpath`로 프로젝트 기준 상대경로(대소문자 무시 비교, 원래 대소문자 유지) 후 `\`→`/`
  - 입출력: stdin을 UTF-8로 디코드, 차단 메시지를 UTF-8로 출력(Windows 기본 cp949로 읽으면 한글 경로에서 JSON 파싱이 실패해 **훅이 그냥 통과**한다)
  - gradle 호출 훅은 `JAVA17_HOME`이 있으면 `JAVA_HOME`으로 주입, `gradlew(.bat)`는 절대경로로 실행
  - `check_done`은 BE 테스트가 다시 쓴 `docs/samples/*`만 되돌린다(원래 수정 중이던 샘플은 유지)
  - 지시서 대비 확장: `.env` 차단을 루트뿐 아니라 하위 디렉토리(`ai/.env`)까지
- `.claude/settings.json`(공용, `python3 "${CLAUDE_PROJECT_DIR}/.claude/hooks/*.py"`), `.claude/agents/plan-verifier.md`
- `.claude/settings.local.json`(개인, 미추적): `env.JAVA17_HOME` = 이 PC의 Temurin 17 경로
- `.gitignore`: `# personal` 아래 `.claude` 전체 무시 → `.claude/*` 무시 + `settings.json`·`hooks/`·`agents/`만 추적, `__pycache__` 무시
- `CLAUDE.md`: 진행 상황, "작업 지시서" 섹션(IMPROVEMENT_PLAN 미작성 명시), 빌드·실행(verify_stack, 기준선 310, application-local 자동 로드, AES 키 교체 위험, 시드·장부 기준, `POSTGRES_HOST_PORT`), AI-6에 `/ready`, 해결된 `ssafy.oauth` 항목 삭제, 문서 지도에 `docs/PRE_CICD.md`

### 결과
- 샘플 입력 테스트(scratchpad `test_hooks.py`, Python 3.9·3.11 각각) → **26/26 통과**
  (AI 서비스 백슬래시·대소문자 혼용·한글 파일명·상대경로 차단, health.py·`.env.example`·프로젝트 밖 통과, main/HEAD:main/force push 차단, 브랜치·maintenance 푸시 통과, 맨 grep/find·파이프 grep 차단, `command grep` 통과, `down -v` 차단·CI=true 허용)
- **세션 실동작 확인**: 맨 `grep` 실행 → `PreToolUse:Bash hook error ... 차단: 이 환경에서 맨 grep/find는...`,
  `ai/app/services/__init__.py` 쓰기 → `PreToolUse:Write hook error ... 차단: ... AI 로직이다` (`git status ai/` 변경 없음)
- `compile_check`: 실제 BE 자바 경로 → gradle 컴파일 15초 exit 0, `JAVA17_HOME=C:\nope` → exit 2 + `JAVA_HOME is set to an invalid directory`
  - 처음 구현은 두 가지 이유로 gradle을 실행하지 못하고 그냥 통과했다: `normcase`가 상대경로를 소문자로 바꿔 `BE/src/` 비교 실패, `cmd /c gradlew.bat`이 cwd에서 못 찾음 → 둘 다 수정
- `check_done`: 코드 경로 변경 + WORK_LOG 미변경 → exit 2 + "아직 끝내지 말 것: ... WORK_LOG.md에 항목이 없다", 변경 없음 → exit 0,
  BE 경로 변경(`BE/_hooktest.txt`) + WORK_LOG 변경 → gradle test 실행 후 exit 0(7초). 소스 변경이 없어 test 태스크가 up-to-date였으므로
  **`docs/samples` 되돌리기 분기는 이번에 실제로 타지 않았다**(다음 BE 소스 수정 때 확인 필요)

### 확인했으나 이번 범위에서 제외한 항목
- `find_unused_java.py`를 Stop 훅에 연결할지는 오탐 빈도를 보고 결정(지시서 0단계 재발 방지 칸)

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `.claude/settings.json` | 신규 |
| `.claude/hooks/*.py` (5개) | 신규 |
| `.claude/agents/plan-verifier.md` | 신규 |
| `.gitignore` | `.claude` 규칙 세분화 |
| `CLAUDE.md` | 갱신 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 8단계 — GitHub Actions CI 워크플로

### 배경
`.github/workflows/`가 없었고(`.github/`에는 `modernize/java-upgrade`만), `Jenkinsfile`에는 테스트 스테이지가 없어 테스트가 깨져도 머지가 막히지 않았다(`docs/PRE_CICD.md` 8단계).

### 작업 내역
- `.github/workflows/ci.yml` 신규(지시서 778-843행 그대로): `pull_request`·`push`(main) 트리거, 잡 3개
  - `be-test`: JDK 17 temurin + `gradle/actions/setup-gradle@v4` + Redis 서비스 → `./gradlew test`, 실패 시 리포트 업로드
  - `ai-test`: Python 3.11 + `requirements.txt` + `pytest pytest-asyncio httpx` → `pytest -q tests/test_health.py`
  - `stack-smoke`(`needs: be-test`): `.env.example` 복사 + `openssl rand`로 CI 전용 키 생성 → `./scripts/verify_stack.sh` → `docker compose down -v`
- CI 시크릿 등록 불필요: BE 테스트는 `application-test.yaml`의 테스트 키를 쓰고, `stack-smoke`는 실행마다 임시 키를 만든다

### 결과
- 로컬에서는 YAML 파싱만 확인: `yaml.safe_load` → jobs `['be-test', 'ai-test', 'stack-smoke']`
- 실제 실행·소요 시간은 PR에서 확인한다(`[측정: ___]`은 PR 결과로 채운다)

### 확인했으나 이번 범위에서 제외한 항목
- `Jenkinsfile` 삭제(CD 문서에서 대체 확정 후), `main` 브랜치 보호 규칙의 필수 체크 지정(GitHub 웹 설정, 사용자 작업)
- `ai/requirements.txt`의 `--extra-index-url .../whl/cpu`가 있어도 pip가 PyPI의 CUDA torch를 고를 수 있다. `ai-test` 소요 시간을 보고 판단

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `.github/workflows/ci.yml` | 신규 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 7단계 — 한 번에 검증하는 스크립트

### 배경
"컨테이너로 띄워 핵심 흐름이 도는지"를 확인하는 단일 명령이 없었다(`docs/PRE_CICD.md` 7단계).
지시서는 `scripts/smoke_payment_ledger.py`를 "수정"하라고 했지만 저장소 전 이력에 없는 파일이라 새로 작성했다(사용자 결정).

### 작업 내역
- `scripts/verify_stack.sh` 신규(지시서 708-742행 그대로): `.env` 확인 → `docker compose up -d --build postgres redis backend` → healthy 대기(최대 5분) → 스모크
- `scripts/smoke_payment_ledger.py` 신규(표준 라이브러리만):
  `POST /api/auth/test-login?email=01022222222` → `GET /api/cards`에서 BUSINESS 카드 → `GET /api/cards/merchants` →
  `POST /api/payments/authorize`(BUSINESS, OFFLINE) → `POST /api/payments/{id}/capture`(CAPTURED 확인) →
  `GET /api/book-entries?size=50`에서 같은 `paymentId` 장부를 최대 30초 폴링(장부는 `@Async` AFTER_COMMIT 리스너가 만든다).
  실패하면 단계명·HTTP 코드·응답 본문을 출력하고 exit 1. docstring에 `TEST_LOGIN_ENABLED=true`, 빈 DB 시드 전제 명시
- `FE/docs/merchant-test.html:50` 기본 서버 `https://j14c203.p.ssafy.io/api` → `http://localhost:8080/api`
- 루트 `.gitattributes` 신규: `*.sh text eol=lf`(CRLF로 체크아웃되면 리눅스 CI에서 `bash\r`로 실패). `verify_stack.sh`는 `git update-index --chmod=+x`로 100755

### 결과
- 첫 실행은 스모크 실패 → 기존 버그 2건 발견·수정(바로 아래 "시드 카드 결제 확정" 항목)
- 수정 후 빈 DB에서 `./scripts/verify_stack.sh` → **exit 0, 190초**(이미지 재빌드 포함), `OK: backend healthy + 결제→장부 스모크 통과`
- 스모크 단독 재실행: `OK: paymentId=2 amount=10691 → bookEntryId=2 category=접대비`
- 실패 경로: `JWT_SECRET= ./scripts/verify_stack.sh` → **exit 1, 327초**, `FAIL: backend 상태=unhealthy (healthy가 아님)` + backend 로그 200줄
  (원인 `io.jsonwebtoken.security.WeakKeyException: The specified key byte array is 0 bits`가 로그에 포함). 셸 변수가 compose 치환에서 `.env`보다 우선해 `.env`를 건드리지 않고 재현
- `git ls-files --eol scripts/verify_stack.sh` → `i/lf w/lf attr/text eol=lf`

### 확인했으나 이번 범위에서 제외한 항목
- 실패 시 healthy 대기 루프가 `unhealthy`에서 멈추지 않고 5분을 다 기다린다(`restart: on-failure`라 상태가 `exited`로 가지 않음). 지시서 스크립트 그대로 두고 기록만 한다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `scripts/verify_stack.sh` | 신규(실행 권한) |
| `scripts/smoke_payment_ledger.py` | 신규 |
| `FE/docs/merchant-test.html` | 기본 서버 URL 변경 |
| `.gitattributes` | 신규 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: 시드 카드 결제 확정이 ACCOUNT_NOT_FOUND로 실패하던 버그 수정 + 사업자 카드 시드

### 배경
7단계 스모크(`scripts/smoke_payment_ledger.py`)를 처음 돌렸을 때 시드 유저 카드로 결제 확정이
`POST /api/payments/1/capture → HTTP 404 {"errorCode":"ACCOUNT_NOT_FOUND"}`로 실패했다. 지시서 범위 밖의 기존 버그지만 완료 정의(스모크 통과)를 막는다.

### 원인
1. **출금계좌 미저장**: `CardDataInitializer`가 계좌를 `SsafyFinanceClient.createAccount`로 직접 만들었다. 로컬 원장 전환 후 이 메서드는
   계좌번호만 발급하고 저장하지 않는다(`SsafyFinanceClient.java:47` 주석 "계좌 영속화는 AccountService가 담당한다").
   그래서 `accounts` 0행인 채로 카드가 그 계좌번호를 출금계좌로 가졌고, 결제 확정 시
   `SsafyCreditCardClient.debitWithdrawalAccount`(`findByAccountNoKeyForUpdate`)가 계좌를 못 찾았다.
   → 시드 유저는 **어떤 결제도 확정할 수 없는 상태**였다
2. **장부 대상 카드 없음**(1을 고친 뒤 드러남): 확정은 성공했지만 30초 안에 장부가 생기지 않았다. `PaymentService.java:529`가
   `PaymentCapturedEvent.purpose`에 결제의 purpose가 아니라 **카드 유형**(`card.getCardType().name()`)을 넣고,
   `BookEntryEventListener`는 BUSINESS만 장부를 만든다. 시드 카드 2장은 모두 PERSONAL이고, 사업자 카드 발급 API(`POST /api/cards`)는 OTP가 필요하다.
   이벤트·리스너 주석이 모두 "사업자 카드 결제"라 의도된 설계로 보고 **도메인 로직은 바꾸지 않았다**(사용자 결정)

### 작업 내역
- `CardDataInitializer`: 계좌 생성을 `AccountService.createAccount(userId, new AccountCreateRequest(...))`로 변경 → `accounts`에 저장(데모 잔액 5,000,000)
- `CardDataInitializer`: 두 번째 시드 유저(이싸피, `01022222222`)는 BUSINESS 카드·BUSINESS 계좌로 발급(사용자 결정)
- `Tax7iApplicationTests`에 회귀 테스트 2개: 시드 카드의 출금계좌가 `accounts`에 있다 / BUSINESS 시드 카드가 있다

### 결과
- 회귀 테스트를 수정 전 코드로 돌리면 `2 tests completed, 1 failed`, 수정 후 통과 → 버그를 잡는 테스트임을 확인
- `./gradlew test` → **tests=310 failures=0 errors=0 skipped=1** (308 + 신규 2)
- 빈 DB 기동 후 `cards`: `1|PERSONAL|test-ci-01011111111`, `2|BUSINESS|test-ci-01022222222`

### 확인했으나 이번 범위에서 제외한 항목
- 장부 생성 기준을 "카드 유형"이 아니라 "결제 purpose"로 바꿀지(결제마다 사용자가 고르는 purpose가 장부 생성에 쓰이지 않는다)는 도메인 결정이 필요해 남겨 둔다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/src/main/java/com/ssafy/tax7i/config/CardDataInitializer.java` | 계좌 저장 경로 수정, 2번 유저 사업자 카드 |
| `BE/src/test/java/com/ssafy/tax7i/Tax7iApplicationTests.java` | 회귀 테스트 2개 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 6단계 — AI readiness 엔드포인트

### 배경
`ai/app/routers/health.py`는 항상 `{"status": "ok"}`라, `ANTHROPIC_API_KEY`가 없어도 health는 green인데 chat만 실패한다(CLAUDE.md AI-6).
liveness와 readiness를 구분하지 않는다(`docs/PRE_CICD.md` 6단계).

### 작업 내역
- `ai/app/routers/health.py`를 지시서 626-645행으로 교체: `/api/v1/health/`(liveness, 기존과 같은 응답) + `/api/v1/health/ready`(키 없으면 503)
- `ai/tests/test_health.py` 신규(지시서 651-681행): health 라우터만 붙인 앱으로 3케이스
- AI 로직(`ai/app/services/**` 등)은 수정하지 않음

### 결과
- 이 PC에는 AI venv가 없다(`ai/`에 venv 없음, 시스템 python에 fastapi 없음). `requirements.txt` 전체(torch 등)는 무거워
  scratchpad에 최소 venv(`fastapi httpx pydantic-settings pytest pytest-asyncio`)를 만들어 **`--noconftest`** 로 실행:
  `pytest -q --noconftest tests/test_health.py` → **3 passed in 1.23s**
  (`tests/conftest.py`가 `app.main`·서비스를 임포트해 전체 의존성이 필요하다 — 지시서 684행. 테스트 자체는 conftest 픽스처를 쓰지 않는다)
- conftest를 포함한 실행은 8단계 CI `ai-test` 잡(전체 requirements 설치)에서 확인한다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `ai/app/routers/health.py` | `/ready` 추가 |
| `ai/tests/test_health.py` | 신규 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 5단계 — nginx SSE 프록시 설정

### 배경
`FE/nginx.conf`의 `/api/` 프록시는 nginx 기본값(응답 버퍼링 on, `proxy_read_timeout` 60s)이라 QR 결제 SSE
(`/api/payments/qr/token/{token}/events`, `/api/payments/qr/merchant-token/{token}/events`, 토큰 TTL 300s `PaymentService.java:49`)가
지연·조기 종료될 수 있다. upstream도 Jenkins 시절 컨테이너명 `tax-backend`였다(`docs/PRE_CICD.md` 5단계).

### 작업 내역
- `FE/nginx.conf`를 지시서 565-599행으로 교체: upstream `tax7i_backend { server backend:8080; }`,
  SSE 경로(`~ ^/api/payments/qr/.+/events$`)만 `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 360s`, HTTP/1.1 keep-alive

### 결과
- `docker run --rm --add-host backend:127.0.0.1 -v .../FE/nginx.conf:/etc/nginx/conf.d/default.conf:ro nginx:alpine nginx -t`
  → `syntax is ok` / `test is successful`
- `--add-host` 없이 실행하면 upstream 호스트를 못 찾아 `test failed`(지시서 602행 예상과 동일)

### 확인했으나 이번 범위에서 제외한 항목
- 실제 SSE 지연·300초 유지 측정: FE 컨테이너가 compose에 없어(IMPROVEMENT_PLAN 13번) 문법 검증까지만 했다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `FE/nginx.conf` | 교체 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 4단계 — compose `worker` → `backend` + `.env.example`

### 배경
compose의 `worker`는 `SPRING_PROFILES_ACTIVE: worker`인데 `application-worker.yaml`이 없어(`command grep -rn worker BE/src/main/resources` → 로컬 전용 파일 주석 1건뿐) 이름만 다른 backend였고,
AI 최초 기동(`start_period: 600s`)을 기다렸다. 필요한 환경변수 목록도 compose 주석·매뉴얼에 흩어져 있었다(`docs/PRE_CICD.md` 4단계).

### 작업 내역
- `docker-compose.yml`: `worker`를 지시서 463-493행 `backend`로 교체(8080 노출, `env_file: .env`(required:false), AI 의존 제거)
- `docker-compose.yml` **추가 변경**: postgres 호스트 포트를 `${POSTGRES_HOST_PORT:-5432}`로 열었다.
  이 PC에 Windows 서비스 `postgresql-x64-17`이 5432를 점유해 `Bind for 0.0.0.0:5432 failed: port is already allocated`로 기동 실패.
  기본값은 그대로 5432라 CI·다른 PC 동작은 같고, 컨테이너 간 통신(backend→postgres:5432)과 무관하다
- `.env.example` 신규(지시서 501-526행 + `POSTGRES_HOST_PORT=5432` 안내)
- `monitoring/prometheus/prometheus.yml`: `tax7i-backend` 타깃 `backend:8080`, bootRun용은 주석, `tax7i-worker` 주석 블록 삭제
- 로컬 `.env`(gitignore 대상)에 스위치 키와 `POSTGRES_HOST_PORT=5433` 추가
- **로컬 postgres 볼륨 초기화(사용자 승인)**: 기존 `7itax_postgres-data`(2026-08-03 생성, 유저 4·카드 2·결제 0·장부 0)는 이전 AES 키로 암호화돼 있어
  새 키로 기동하면 `AesEncryptor.convertToEntityAttribute` → `복호화에 실패했습니다`로 크래시 루프(RestartCount 16). `docker volume rm 7itax_postgres-data`

### 결과
- `docker compose config -q` → 출력 없음(exit 0)
- `MSYS_NO_PATHCONV=1 docker run ... promtool check config` → `SUCCESS: 5 rules found`
- 빈 DB + 임시 override(`APP_SEED_ENABLED=false`, `TEST_LOGIN_ENABLED=false`)로 `up -d postgres redis backend`:
  **33초 만에 healthy**, RestartCount 0, `users` 0건, `[CardDataInitializer]` 로그 0줄(2단계 검증), `POST /api/auth/test-login` → **403**(1단계 검증), readiness `{"status":"UP"}`
- 기본 `.env`로 재생성: healthy, `users=2 cards=2`, `[CardDataInitializer] 유저가 없어 더미 유저 2명을 생성합니다.`, test-login → **200**
- Prometheus `/api/v1/targets`: `('tax7i-backend', 'http://backend:8080/actuator/prometheus', 'up')`

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `docker-compose.yml` | `worker`→`backend`, postgres 호스트 포트 변수화 |
| `.env.example` | 신규 |
| `monitoring/prometheus/prometheus.yml` | 스크랩 타깃 변경 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 3단계 — 운영용 BE 이미지

### 배경
기존 `BE/Dockerfile`은 root 실행, 헬스체크·JVM 메모리 옵션·타임존 없음, `.dockerignore` 없음(`docs/PRE_CICD.md` 3단계).
추가로 이 PC에는 로컬 전용 `BE/src/main/resources/application-local.yaml`(AES·JWT 시크릿, gitignore 대상)이 있는데,
`.dockerignore`가 없으면 `COPY src src`로 이미지 안 jar에 들어간다.

### 작업 내역
- `BE/Dockerfile`을 지시서 405-425행으로 교체(멀티스테이지, `useradd --uid 1001 app`, readiness HEALTHCHECK, `MaxRAMPercentage=75`, `Asia/Seoul`)
- `BE/.dockerignore` 신규: 지시서 목록 + **`src/main/resources/application-local.yaml`**(지시서에 없던 항목)
- 첫 빌드는 Docker Desktop 미기동으로 실패(`open //./pipe/dockerDesktopLinuxEngine`) → 기동 후 재빌드

### 결과
- `docker build -t tax7i-backend BE` exit 0. 빌드 컨텍스트 전송 5.17MB
- `docker run --entrypoint sh tax7i-backend -c 'id -u'` → **1001**(비root), `/app`에는 `app.jar` 하나
  (`bootJar`만 실행하므로 plain jar가 생기지 않아 지시서 438행의 `jar { enabled = false }`는 불필요)
- 이미지의 jar를 꺼내 zip 목록 확인: yaml은 `BOOT-INF/classes/application.yaml`만, **`application-local.yaml` 없음**
- `docker image inspect` Healthcheck: `curl -fs http://localhost:8080/actuator/health/readiness`, interval 10s, start-period 90s, retries 5
- `healthy` 상태 확인은 compose로 DB·Redis와 함께 띄우는 4단계에서 한다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/Dockerfile` | 교체 |
| `BE/.dockerignore` | 신규 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 2단계 — 스케줄러·시드 데이터 스위치

### 배경
`SchedulingConfig`는 조건 없는 `@EnableScheduling`이라 BE 2대 구성에서 `TaxCalendarScheduler`(매일 09:00 FCM)가 중복 실행되고,
`CardDataInitializer`는 조건 없는 `CommandLineRunner`라 운영 DB에도 더미 유저 2명(`01011111111`, `01022222222`)과 카드를 만든다(`docs/PRE_CICD.md` 2단계).

### 작업 내역
- `SchedulingConfig`: `@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)`
- `CardDataInitializer`: `@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)`
- 1단계 yaml이 두 키를 `APP_SCHEDULING_ENABLED`·`APP_SEED_ENABLED`(기본 true)로 연결 → 기본 동작 불변
- 세법 기준 데이터 Initializer 4개(`TaxBracket`, `TaxParameter`, `TaxDeadline`, `BookEntryAmountBackfill`)는 운영에서도 필요하므로 불변

### 결과
- `./gradlew test` → **tests=308 failures=0 errors=0 skipped=1**
- 빈 DB에서 `APP_SEED_ENABLED=false` 기동 확인은 컨테이너 경로가 생기는 4단계에서 한다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/src/main/java/com/ssafy/tax7i/config/SchedulingConfig.java` | 조건 애너테이션 추가 |
| `BE/src/main/java/com/ssafy/tax7i/config/CardDataInitializer.java` | 조건 애너테이션 추가 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 1단계 — 설정 환경변수 외부화 + `ssafy.oauth` 죽은 설정 제거

### 배경
같은 이미지를 로컬·성능측정·운영에 쓰려면 환경마다 달라지는 값이 환경변수로 열려 있어야 한다.
`app.test-login.enabled: false`가 하드코딩돼 매뉴얼의 `TEST_LOGIN_ENABLED`가 바인딩되지 않았고,
`ssafy.oauth` 블록은 Java 소비처가 0건이었다(`docs/PRE_CICD.md` 1단계).

### 작업 내역
- `BE/src/main/resources/application.yaml`을 지시서 239-331행으로 교체. 단, 지시서 버전에서 빠진 기존 Actuator 주석 2줄은 유지
  - 신규 placeholder: `TEST_LOGIN_ENABLED`, `JPA_DDL_AUTO`, `DB_POOL_MAX`, `APP_SEED_ENABLED`, `APP_SCHEDULING_ENABLED`, `AI_MOCK_ENABLED`
  - `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s`, `server.forward-headers-strategy: native`
  - `ssafy.oauth` 블록 삭제
- `exec/포팅_매뉴얼.md` Backend 환경변수 표에 신규 5개 추가. 지시서는 `SSAFY_*` 3개 삭제도 요구하지만 표에 원래 없었다(변경 없음)
- 지난 항목에서 "디스크에 남아 있다"고 적은 삭제 디렉토리 캐시는 이후 로컬에서 지웠다(`git status`가 untracked로 표시해 실수로 커밋될 위험). 단 VS Code Java 확장이 `2026_oneday_journal/**/nb-cache`를 다시 만들어 untracked로 남는다 — 커밋하지 않음

### 결과
- `command grep -rn "ssafy.oauth\|SSAFY_CLIENT\|ssafy:" BE/src` → **0건**
- `./gradlew test` → **tests=308 failures=0 errors=0 skipped=1**
- `TEST_LOGIN_ENABLED` 런타임 확인(true→200 / 없음→403)은 컨테이너 기동이 가능한 4·7단계에서 한다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/src/main/resources/application.yaml` | 환경변수 외부화, `ssafy.oauth` 삭제, graceful shutdown |
| `exec/포팅_매뉴얼.md` | 환경변수 표 5행 추가 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 0단계 묶음 C — 로컬 원장 전환 후 남은 미사용 메서드 제거 + 탐지 스크립트

### 배경
SSAFY 금융망 HTTP 클라이언트를 DB 원장으로 바꾸면서 호출되지 않는 메서드가 남았다(`docs/PRE_CICD.md` 0단계 묶음 C).

### 작업 내역
- 재확인: `command grep -rn "\bsearchMember\b|\bregisterMember\b|\bgetMyCards\b" BE/src` → 선언부 1건씩만(호출 0건)
- 삭제: `SsafyFinanceClient.searchMember`·`registerMember`(구 `:190`,`:194`), `SsafyCreditCardClient.getMyCards`(구 `:120`)
- **연쇄 삭제**(지시서 116행 규칙): `getMyCards` 삭제 후
  - `scripts/find_unused_java.py`가 `banking/client/dto/SsafyCreditCardListResponse.java`를 새 클래스 후보로 출력 → 삭제
  - `CardRepository.findByUser_SsafyUserKeyAndDeletedFalse`는 유일한 호출부가 `getMyCards`였다(main·test 검색 0건) → 삭제.
    `AccountRepository`의 같은 이름 메서드는 `SsafyFinanceClient.java:55`가 사용 중이라 유지
- `scripts/find_unused_java.py` 신규(지시서 141-211행을 그대로 추출)

### 결과
- `./gradlew compileJava compileTestJava test --no-daemon -q` exit 0, **tests=308 failures=0 errors=0 skipped=1**
- 탐지 스크립트 결과: 삭제 전 클래스 5·메서드 23 (지시서 213행 수치와 일치) → 현재 **클래스 0·메서드 20**.
  남은 20개는 확인 필요 5개(`ConsentService.getConsents/revokeConsent`, `getVehicleNoInsuranceRatio`, `AiCategoryMapper.mapToName/mapToCode`)와
  프레임워크 호출 15개(리스너·스케줄러·AOP·JPA 컨버터·템플릿 getter·`@Recover`)
- `git diff --cached --shortstat`(BE): 4 files changed, 53 deletions(-)

### 확인했으나 이번 범위에서 제외한 항목
- 지시서 "확인 필요" 표 7개 항목(ConsentService, getVehicleNoInsuranceRatio, AiCategoryMapper, `FE/src/`, `FE/webview/`, `Jenkinsfile`, `Ssafy*` 클래스명)은 건드리지 않음

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/.../banking/client/SsafyFinanceClient.java` | 메서드 2개 삭제 |
| `BE/.../banking/client/SsafyCreditCardClient.java` | `getMyCards` 삭제 |
| `BE/.../banking/client/dto/SsafyCreditCardListResponse.java` | 삭제(연쇄) |
| `BE/.../card/repository/CardRepository.java` | 미사용 쿼리 메서드 삭제(연쇄) |
| `scripts/find_unused_java.py` | 신규 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 0단계 묶음 B — 참조 0건 BE 클래스 5개 제거

### 배경
`card/dto` DTO 4개와 미구현 보상 이벤트가 어디에서도 쓰이지 않는다(`docs/PRE_CICD.md` 0단계 묶음 B).

### 작업 내역
- 재확인: `command grep -rlw <클래스명> BE/src` → 5개 모두 자기 파일 1건만 매칭(외부 참조 0건)
- `scripts/find_unused_java.py`(지시서 141-211행)도 같은 5개를 클래스 후보로 출력
- `git rm` 대상: `CardBalanceResponse`, `CardDepositRequest`, `CardDepositResponse`, `CreateCardFromAccountRequest`, `payment/event/BookEntryCreationFailedEvent`

### 결과
- `./gradlew compileJava compileTestJava test --no-daemon -q` exit 0, 결과 XML 집계 **tests=308 failures=0 errors=0 skipped=1** (기준선 유지)
- `git diff --cached --shortstat`: 5 files changed, 52 deletions(-)
- 참고: `./gradlew test`는 `SampleExportGenerator` 테스트가 `docs/samples/*`를 다시 써서 작업 트리를 더럽힌다. 매번 `git restore docs/samples`로 되돌렸다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `BE/src/main/java/com/ssafy/tax7i/card/dto/{CardBalanceResponse,CardDepositRequest,CardDepositResponse,CreateCardFromAccountRequest}.java` | 삭제 |
| `BE/src/main/java/com/ssafy/tax7i/payment/event/BookEntryCreationFailedEvent.java` | 삭제 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 0단계 묶음 A — 서비스와 무관한 디렉토리·파일 제거

### 배경
저장소에 서비스와 무관한 연습 프로젝트, 산출물 사본, 빌드 불가 Dockerfile이 섞여 있다(`docs/PRE_CICD.md` 0단계).

### 작업 내역
- 삭제 직전 재확인(지시서 78-81행 검색, `--exclude-dir=node_modules,build,.gradle` 추가) → 5개 대상 모두 **참조 0건**
- `jieonPractice/`는 사용자 확인 후 삭제
- `git rm -r 2026_oneday_journal jieonPractice MyApplication excel Dockerfile`

### 결과
`git diff --cached --shortstat`: **128 files changed, 3146 deletions(-)**.
빌드 영향 없음(compose·매뉴얼은 `BE/Dockerfile`만 참조, 코드가 쓰는 엑셀 템플릿은 `BE/src/main/resources/templates/excel/`).

### 확인했으나 이번 범위에서 제외한 항목
- 세 안드로이드 디렉토리의 gitignore 대상 로컬 캐시(`.gradle/`, `app/build/`)는 추적 파일이 아니라 디스크에 남아 있다. 필요하면 로컬에서 직접 지운다

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `2026_oneday_journal/`, `jieonPractice/`, `MyApplication/` | 삭제 |
| `excel/` (루트), `Dockerfile` (루트) | 삭제 |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-10-18: PRE_CICD 지시서 착수 — 기준선 측정 + 문서와 실제의 차이

### 배경
`docs/PRE_CICD.md`(CI/CD 직전 완성 지시서, 0~9단계)를 브랜치 `chore/pre-cicd`에서 구현한다.
지시서 규칙 0-3에 따라 각 단계의 "현재 상태 근거"를 실제 저장소와 먼저 대조했다.

### 작업 내역
- 루트에 있던 untracked `PRE_CICD.md`를 지시서가 전제하는 경로 `docs/PRE_CICD.md`로 이동해 커밋
- JDK 17 설치: `winget install EclipseAdoptium.Temurin.17.JDK` → `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
  (시스템 `JAVA_HOME`은 JDK 25 그대로 두고 명령마다 지정)
- 기준선: `JAVA_HOME=<jdk17> ./gradlew test --no-daemon` → BUILD SUCCESSFUL 2m21s,
  결과 XML 집계 **tests=308 failures=0 errors=0 skipped=1**
  (Windows에서는 `BE/build.gradle:12-14`가 빌드 산출물을 `C:/tmp/tax7i-build`로 보낸다 — Windows 한정이라 Docker·CI 영향 없음)

### 문서와 실제의 차이 (사용자와 합의한 처리)
| 차이 | 처리 |
|---|---|
| `docs/IMPROVEMENT_PLAN.md` 없음(지시서가 여러 번 참조) | 미작성 문서로 간주. 참조는 그대로 두고 CLAUDE.md에 미작성 명시 |
| `scripts/smoke_payment_ledger.py`가 저장소 전 이력에 없음(`git log --all -- '*smoke_payment_ledger*'` 0건). 7단계는 "수정"을 전제 | 새로 작성 |
| `jq` 미설치 + Windows 경로(`C:\...`) → 9단계 bash 훅이 매칭 실패할 위험(지시서 866행 경고) | 훅을 Python으로 작성 |
| JAVA_HOME이 JDK 25, JDK 17 없음 | 위와 같이 설치 |
| `jieonPractice/` 삭제는 "사람 확인 필요" | 사용자 확인 → 삭제 |
| 로컬 전용 `BE/src/main/resources/application-local.yaml`(시크릿 포함, gitignore 대상)이 Docker 빌드 컨텍스트로 들어감 | 3단계 `BE/.dockerignore`에 추가 |

나머지 "현재 상태 근거"(SchedulingConfig·CardDataInitializer 무조건 실행, `ai.mock.enabled` 존재 `AiServiceClient.java:21`,
worker용 yaml 없음, nginx `tax-backend`, health.py 항상 ok, prometheus `host.docker.internal:8080`,
`merchant-test.html`의 죽은 URL, `.github/workflows`·`.claude/` 없음)는 문서와 일치했다.

### 결과
기준선 308개 통과 확인. 구현 착수.

### 변경된 파일 목록
| 파일 | 변경 |
|---|---|
| `docs/PRE_CICD.md` | 신규(루트 untracked 파일 이동) |
| `WORK_LOG.md` | 본 항목 추가 |

---

## 2026-08-04: 세션 마무리 — 재개 지점 기록

### 배경
문서 정리와 검증 계획 수립까지 마치고 세션을 종료한다. 다음 세션이 맥락 없이 시작해도
바로 이어갈 수 있도록 현재 상태와 시작점을 남긴다.

### 이번 세션에서 한 일 (시간순)

1. **원격 최신화** — `origin/main`이 12커밋 앞서 있어 `main`으로 전환 후 fast-forward pull (`649afe2` → `ab24bca`).
   기존 작업 브랜치 `feat/local-runnable`은 PR #1로 이미 머지된 상태라 유실 없음.
   새로 들어온 것: 모니터링 스택, AOP 감사 로그, 추가인증 로그인, 외부 API Mock, 세목분류 리포지토리 분리
2. **문서 충돌 진단** — 루트 문서 4종이 서로 다른 시점에 작성돼 내용이 어긋난 것을 코드와 대조해 확인
3. **`todoerror.md` 제거** — 낡은 서술 확인 후 삭제
4. **기능 검증 구역 11개 정리** — 실제 컨트롤러·라우터·화면 목록을 뽑아 의존 순서대로 분할
5. **`CLAUDE.md` 신규 + `.gitignore` 수정** — 작업 기록 규칙을 저장소 차원 약속으로 명문화
6. **커밋·푸시·PR** — `f1ec09b` → PR #3

### 현재 상태

| 항목 | 값 |
|---|---|
| 브랜치 | `docs/work-log-verification-zones` (원격과 동기) |
| 커밋 | `f1ec09b` — `origin/main` 대비 1커밋 앞섬, 뒤처짐 0 |
| PR | [#3](https://github.com/qkrrlxor627/7ITAX/pull/3) open — 머지 대기 |
| 작업 트리 | 깨끗함 |
| 코드 변경 | **없음** (문서만) → 빌드/테스트 영향 없음 |

### 다음 세션 시작점

**검증 구역 `[0]`부터 순서대로 진행한다.** 구역 정의는 아래 "기능 검증 구역 분할" 항목 참조.

```bash
# [0] 인프라 — compose에 backend 서비스는 없다(BE는 bootRun 전용)
docker compose up -d postgres redis

# [1] BE 기동 — JDK 17 필수, SPRING_PROFILES_ACTIVE=local 사용 불가
cd BE
AES_ENCRYPTION_KEY=<키> JWT_SECRET=<32바이트+> REDIS_PASSWORD=ssafy ./gradlew bootRun
./gradlew test          # 308개 통과가 기준선

# [7]은 bge-m3 약 2GB 다운로드로 오래 걸리므로 [2] 작업 중 백그라운드로 미리 띄울 것
```

재개 시 유의:
- **`command grep` / `command find`를 쓸 것.** 맨 `grep`/`find`는 셸 함수에 가로채져 빈 결과 + exit 1을 반환한다(거짓 음성)
- PR #3이 머지되지 않았다면 `docs/work-log-verification-zones`에서 계속 작업하고, 머지됐다면 `main`을 pull한 뒤 새 브랜치를 판다
- 검증 결과는 구역 단위로 이 문서에 새 항목으로 기록한다

### 아직 손대지 않은 것 (우선순위 순)

1. **`application-local.yaml` 부재** — `BE/.gitignore:44`로 제외돼 클론본에 없다. `[1]` 진행 시 바로 부딪힌다. 실질 영향 가장 큼
2. **INT-1 개인화** — BE에 `/api/v1/users/**` 엔드포인트 없음. `[8]`에서 확인 필요
3. **`ssafy.oauth` dead config** — `application.yaml:44-51`, Java 소비처 0건
4. **FE RELEASE URL** — `Constants.kt:9,14`가 죽은 `j14c203.p.ssafy.io`
5. **FE 테스트 부재** — 템플릿 예제 2개뿐이라 `[9]`는 전부 수동 확인

### 변경된 파일 목록
| 파일 | 변경 유형 |
|------|----------|
| `WORK_LOG.md` | 수정(본 항목 추가) |
| `CLAUDE.md` | 수정(현재 진행 상황 섹션 추가) |

---

## 2026-08-04: 기능 검증 구역 분할 (11개 구역)

### 배경
"기능이 정상 작동하는지 하나씩 쪼개서 확인"하기 위해, 실제 컨트롤러·라우터·화면 목록을 뽑아
의존 순서가 있는 검증 구역으로 나눴다. 앞 구역이 깨지면 뒤 구역 결과를 신뢰할 수 없으므로 **순서가 핵심**이다.

### 의존 관계

```text
[0] 인프라 ──> [1] BE 기동 ──> [2] 인증/토큰 ──┬─> [3] 뱅킹 원장
                                              ├─> [4] 결제/카드
                                              ├─> [5] 장부/세목분류
                                              └─> [6] 세금계산/신고서
     [7] AI 단독 ─────────────> [8] BE↔AI 연동 (1+7 필요)
                               [9] FE Android (1 필요)
                               [10] 모니터링 (1 필요)
```

권장 순서: `[0]→[1]→[2]` 통과 후 `[3]~[6]` 병렬, `[8]~[10]` 마지막.
`[7]`은 모델 다운로드가 오래 걸리므로 `[2]` 작업 중 백그라운드로 미리 기동해둔다.

### 구역별 상세

#### [0] 인프라 — postgres + redis
- `docker compose up -d postgres redis`
- compose에 **backend 서비스는 없다** — BE는 `bootRun` 전용
- 확인: 컨테이너 2개 healthy, `db/init/01_schema.sql`·`02_seed_data.sql` 적재
- ⚠️ redis는 `--requirepass ssafy`로 기동

#### [1] BE 기동
```bash
cd BE
AES_ENCRYPTION_KEY=<키> JWT_SECRET=<32바이트+> REDIS_PASSWORD=ssafy ./gradlew bootRun
```
- 확인: 컨텍스트 기동 + `./gradlew test` 308개 통과 (테스트 클래스 36개)
- ⚠️ **JDK 17 필수** — 시스템 기본 JDK로는 Gradle 구동 불가
- ⚠️ **`SPRING_PROFILES_ACTIVE=local` 사용 불가** — `application-local.yaml`이 `BE/.gitignore:44`로 제외돼 클론본에 없다

#### [2] 인증/토큰 — 전체의 관문
- `/api/auth`: `verify-identity` → `setup-pin` → `login` → `login/additional-auth` → `reissue` → `logout`
- `/api/pay/pin`: 결제 전용 PIN (로그인 PIN과 분리된 `payPinHash`)
- 확인: 로그인으로 JWT 확보(이후 전 구역이 의존). `reissue`·`logout`까지 돌려야 Redis 연동이 검증됨
- 테스트: `AuthControllerTest`, `AuthServiceTest`, `JwtTokenProviderTest`, `JwtAuthenticationFilterTest`, `PinServiceTest`, `PayPinServiceTest`, `NiceIdentityMockServiceTest`

#### [3] 뱅킹 원장
- `/api/banking/accounts`, `/api/transfers` (p2p · withdraw)
- 외부 금융망 없이 **DB 원장 100%**, 신규 계좌 500만원 시드
- 테스트: `AccountServiceTest`, `AccountControllerTest`, `TransferServiceTest`, `TransferControllerTest`, `LocalBankingLedgerTest`

#### [4] 결제/카드
- `/api/payments` (QR merchant-token 포함), `/api/cards`
- ⚠️ `POST /api/payments/qr/merchant-token/*/pay`만 인증 필수로 분리됨(SEC-1). **미인증 401이 정상 동작**
- 테스트: `PaymentServiceTest`, `PaymentControllerTest`, `CardServiceTest`, `CardControllerTest`

#### [5] 장부/세목분류
- `/api/book-entries`, `/api/classification`
- ⚠️ 장부 금액 필드는 **부가세 포함 원 결제금액**이 정본(공급가액 아님). 55,000 / 1,100,000 / 3,300,000이 올바른 값
- 테스트: `BookEntryServiceTest`, `BookEntryControllerTest`, `BookEntryEventListenerTest`, `MccClassificationTest`, `AiCategoryMapperTest`, `TaxClassificationAiFallbackTest`, `ClassificationServiceTest`

#### [6] 세금 계산/신고서
- `/api/tax`, `/api/tax/vat-returns`, `/api/tax-estimation`, `/api/tax-calendar`, `/api/export`
- 확인: `docs/samples/` CSV/PDF 6종과 실제 산출물 대조
- 테스트: `TaxFullPipelineTest`, `TaxEstimationServiceTest`, `TaxEstimationControllerTest`, `TaxCalendarServiceTest`, `TaxSavingServiceTest`, `ExportControllerTest`, `AggregateResultTest`

#### [7] AI 단독 (BE와 독립)
- `ai/` FastAPI — `/api/v1/health`, `/api/v1/chat`, `/api/v1/transaction`. pytest 파일 20개
- ⚠️ **최초 기동이 매우 느리다** — bge-m3 약 2GB 다운로드 + Chroma 자동 인덱싱 (compose healthcheck `start_period` 600초인 이유)
- ⚠️ **`ANTHROPIC_API_KEY`가 없어도 기동되고 health는 green인데 chat만 실패한다**(AI-6). 가장 오판하기 쉬운 함정
- ⚠️ `/transaction/classify`는 파인튜닝 모델 부재 시 규칙기반으로 degrade — 응답 `method`가 `rule_based`인지 `local_model`인지 확인할 것

#### [8] BE↔AI 연동
- BE `/api/chatbot` → AI. 정방향 경로·스키마 일치 확인됨
- ⚠️ **개인화는 아직 동작하지 않는다(INT-1 잔여).** `ai/app/services/backend_client.py:67,96`이
  `/api/v1/users/{id}/transactions`·`/business-info`를 호출하나 **BE에 해당 매핑 0건**(2026-08-04 재확인).
  500은 나지 않고 **개인화만 생략된 일반 답변**이 반환되므로 "정상 동작"으로 오판하기 쉽다
- 테스트: `ChatbotServiceTest`, `AiClassificationServiceTest`

#### [9] FE Android
- `FE/app/src/main/java/com/ssafy/seveniTax/ui/` 하위 15개 영역(auth · pay · payment · card · book · classification · calendar · ai · home · settings 등)
- ⚠️ **자동 테스트가 사실상 없다** — `src/test`·`androidTest`에 템플릿 예제 2개뿐(`com.example.a71tax`). 전부 에뮬레이터 수동 확인
- ⚠️ DEBUG는 `10.0.2.2:8080`로 정렬됐으나 **RELEASE는 죽은 `j14c203.p.ssafy.io`**(`Constants.kt:9,14`)
- 별도로 `FE/webview`(Vite/TS) 존재 — DEBUG WebView는 `10.0.2.2:3000`을 바라봄

#### [10] 모니터링
- prometheus / alertmanager / grafana / webhook-logger
- 확인: Prometheus target UP, `/actuator/prometheus` 노출, 알림 룰 발화
- ⚠️ compose `worker`는 전용 프로파일 없이 백엔드 전체를 복제하는 구조라 별도 기동 검증 필요

### 결과
- 검증 구역 11개 확정. 코드 변경 없음(분석·문서화만)
- 각 구역의 "알려진 지뢰"를 명시해, 통과처럼 보이지만 실제로는 degrade된 상태([7] health green / [8] 개인화 생략)를 구분할 수 있게 함

### 변경된 파일 목록
| 파일 | 변경 유형 |
|------|----------|
| `WORK_LOG.md` | 수정(본 항목 추가) |
| `CLAUDE.md` | 신규 — 작업 기록 규칙 + 환경 함정 + 미해결 항목 정리 |
| `.gitignore` | 수정 — `# personal` 블록에서 `CLAUDE.md` 제외 규칙 삭제(저장소 차원 규칙 문서이므로 추적) |

---

## 2026-08-04: 낡은 진단 문서 `todoerror.md` 제거

### 배경
`origin/main`을 pull해 최신화(`ab24bca`)한 뒤 루트 문서 4종(`todoerror.md`, `wehave0702.md`, `0722ready.md`, `WORK_LOG.md`)이
서로 다른 시점에 작성돼 내용이 충돌하는 것을 확인했다. 이 중 `todoerror.md`는
"로컬 구동 불가 원인" 목록인데, 이 문서가 지적한 문제 대부분을 아래 2026-06-05 항목이 이미 해결한 상태였다.

### 작업 내역

#### 1. 실제 코드와 대조해 낡은 서술 확인
| `todoerror.md` 항목 | 실제 상태 |
|---|---|
| #2 API URL이 DEBUG/RELEASE 모두 `ssafy.io` 하드코딩 | **틀림** — `Constants.kt:6-7`에서 DEBUG는 이미 `http://10.0.2.2:8080/api/`. RELEASE만 잔존 |
| #3 SSAFY OAuth 필요 | 해소 — 인증은 `NiceIdentityMockService` + PIN 기반 |
| #4 opus 모델 ID `claude-opus-4-7` | **낡음** — 코드는 `claude-opus-4-8`(`ai/app/core/config.py:19`). `4-7`을 **설정해야 할 값으로 제시**하던 유일한 문서였음 |
| #5 Solapi SMS 키 없으면 가입 불가 | 해소 — `MockSmsSender` 도입 |
| #6 AES/JWT 미설정 | 해소 — `0722ready.md` BE-2에서 처리 |

#### 2. 제거
- `todoerror.md` 삭제. 아래 2026-08-02 항목의 변경 파일 목록(90행)에 남은 `todoerror.md` 언급은
  당시 커밋의 사실 기록이므로 **수정하지 않았다**.

### 결과
- `claude-opus-4-7`을 설정값으로 안내하던 곳이 사라짐. 남은 `4-7` 언급은 `0722ready.md:77`의
  CFG-3 수정 이력(`4-7` → `4-8`) 한 곳뿐이고, 이는 사실 기록이므로 유지
- 코드 변경 없음 → 빌드/테스트 영향 없음
- 유지 결정: `wehave0702.md`(미완 TODO 다수 — `audit_log` 테이블·`reauth-pin` API·`RiskScoreService` 모두 미구현 확인),
  `0722ready.md`(INT-1 잔여 유효), `Jenkinsfile`, `.gitlab/`

### 확인했으나 이번 범위에서 제외한 항목
- **`ssafy.oauth` dead config** — `application.yaml:44-51`. Java 소비처 0건으로 미사용 확인
  (2026-06-05에 주석 처리했으나 2026-08-02 rebase에서 기본값 방식으로 되살아남). 제거는 보류
- **`application-local.yaml` 부재** — `BE/.gitignore:44`로 제외돼 클론본에 없다.
  아래 2026-06-05 항목의 `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` 절차를 새로 클론한 환경에서 그대로 따라할 수 없음
- **FE RELEASE URL** — `Constants.kt:9,14`가 여전히 `https://j14c203.p.ssafy.io`

### 변경된 파일 목록
| 파일 | 변경 유형 |
|------|----------|
| `todoerror.md` | 삭제 |
| `WORK_LOG.md` | 수정(본 항목 추가) |

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
