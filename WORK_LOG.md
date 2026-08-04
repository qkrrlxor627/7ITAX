# 7iTAX 작업 기록

> 작업을 완료하거나 저장소에 영향을 주는 행동을 했을 때 이 문서에 최신 항목을 맨 위에 추가한다.
> 형식: `## YYYY-MM-DD: 제목` → 배경 / 작업 내역 / 결과 / 변경된 파일 목록.

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
