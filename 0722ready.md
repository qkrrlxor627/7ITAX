# 7iTAX 배포 준비 진단 (2026-07-22)

> 범위: BE(Spring Boot) · AI(FastAPI) · FE(Android) 크로스레이어 연결 + 런타임 기동성.
> 방법: 전 12 FE API · 16 BE 컨트롤러 · 3 AI 라우터 · 40+ DTO 대조 + 고위험 항목 직접 파일 검증.
> 요약: **코드는 돌아가나, 문서(README) 대로 로컬 데모를 띄우면 로그인·AI가 실패한다.** 아래 🔴 5건이 최우선.
>
> **진행(2026-07-22):** BE-1·2·3 ✅ (BE 테스트 308개 전부 통과), AI-1·2·3·4·5 ✅, INF-1 ✅, SEC-1 ✅, CFG-2·3 ✅, INT-1 🟡 부분(챗봇 크래시 제거). **남은 실작업: INT-1 개인화 실동작(BE 엔드포인트)뿐.**

---

## 🔴 BLOCKER — 기동/핵심 흐름이 안 됨

### BE-1. Redis 비밀번호 불일치 → 로그인·토큰 전면 실패 — ✅ 해결(2026-07-22)
- compose는 `redis-server --requirepass ssafy`(`docker-compose.yml:25`)로 인증을 요구하지만, 앱 설정 `spring.data.redis`에는 **password 키가 아예 없었다**(`application.yaml`, grep 확인).
- 부팅은 되지만(Lettuce lazy), 첫 Redis 연산인 리프레시 토큰 저장(`AuthService.issueTokens` → `redisTemplate.opsForValue().set`)에서 `NOAUTH Authentication required` → **login/reissue/logout 전부 500**.
- **✅ 적용**: `application.yaml`에 `spring.data.redis.password: ${REDIS_PASSWORD:}` 추가(미설정 시 빈 값→무인증 유지 → 무비번 redis·테스트 안전), `docker-compose.yml` worker에 `REDIS_PASSWORD: ${REDIS_PASSWORD:-ssafy}` 추가, README bootRun에 `REDIS_PASSWORD=ssafy` 명시.

### BE-2. bootRun에 필수 env 미전달(README 지침 오류) → 컨텍스트 기동 실패 — ✅ 해결(2026-07-22)
- `DB_PASSWORD`, `AES_ENCRYPTION_KEY`, `JWT_SECRET`는 **기본값 없는 `${VAR}`** → 없으면 부팅 크래시(AES는 생성자에서 Base64 디코드, JWT는 32바이트 미만 시 WeakKeyException).
- README(`:66-69`)는 이 값들을 `docker compose up` 앞에만 붙여 **별도 `./gradlew bootRun` 셸엔 전달되지 않았다**.
- 추가로 DB 기본값 `tax_db`/`postgres`가 compose의 `tax7i`/`ssafy`와 불일치 → bootRun이 없는 DB/계정에 접속.
- **✅ 적용**: `application.yaml` DB 기본값을 compose와 정렬(`DB_NAME:tax7i`, `DB_USERNAME:ssafy`, `DB_PASSWORD:ssafy` — 이제 bootRun이 DB env 없이 compose postgres에 접속). AES/JWT는 **의도적으로 필수 유지**(약한 기본값 시크릿 배포 방지)하고 README bootRun 명령을 `AES_ENCRYPTION_KEY=<키> JWT_SECRET=<키> REDIS_PASSWORD=ssafy ./gradlew bootRun`으로 수정 + 데모용 예시 키 추가.

### AI-1. Docker 이미지 빌드 실패 — 존재하지 않는 `models/` COPY — ✅ 해결(2026-07-22)
- `ai/Dockerfile:19` `COPY models/ models/` 인데 `ai/models/` 디렉토리가 **없었다**. → `docker compose build ai` 가 해당 레이어에서 실패, `depends_on: ai` 인 `worker`도 못 뜬다.
- **✅ 적용**: `ai/models/.gitkeep` 생성. 루트 `.gitignore`의 `ai/models/`가 디렉토리 전체를 막아 `.gitkeep`이 추적 불가였으므로 `ai/models/*` + `!ai/models/.gitkeep` 패턴으로 변경 → 대용량 모델 파일은 계속 무시하되 **빈 디렉토리는 클론에 포함**돼 Docker COPY 성공. `.dockerignore`는 `models/`를 제외하지 않음(확인).

### AI-2. 로컬 실행 시 `ANTHROPIC_API_KEY` 없으면 import 시점 크래시 — ✅ 해결(2026-07-22)
- `ai/app/core/config.py` `anthropic_api_key: str`(필수, 기본값 없음), `settings = Settings()`가 **모듈 import 시 실행**. `.env` 없음(`.env.example`만 존재).
- `uvicorn app.main:app` → import 체인에서 pydantic `ValidationError` → 서버 미기동. `pytest`도 동일하게 collection 실패.
- **✅ 적용**: `anthropic_api_key: str = ""`로 기본값 부여 → import·pytest collection 크래시 제거(키 없이도 기동). 대신 `dependencies.init_services`에서 키가 비면 **startup 경고 로그**를 남겨 AI-6("healthy인데 chat 실패")를 표면화. 실제 챗봇은 여전히 `.env`/환경변수로 키를 설정해야 동작.

### AI-3(≈BLOCKER). 최초 실행 시 bge-m3(~2GB) 필요 + 이중 로드 — ✅ 해결(이중 로드), ⚠️ 부분(오프라인)
- `vectorstore.py`와 `embedding_service.py`가 **각각** `HuggingFaceEmbeddings(BAAI/bge-m3)`를 로드(메모리 2배). 둘 다 `init_services`(lifespan)에서 하드 필수.
- **✅ 적용(이중 로드 제거)**: 공유 provider `app/services/embeddings.py`의 `@lru_cache get_embeddings(model, device)` 추가. 두 서비스가 동일 `(model, device)`로 호출 → **단일 인스턴스 재사용**(bge-m3 한 번만 로드, 메모리 ~2배→1배, startup 시간 단축).
- **⚠️ 남는 특성(설계상 불가피)**: 앱은 임베딩 모델이 반드시 있어야 동작하므로 최초 로컬 실행은 네트워크가 필요. 단, Docker는 빌드 시 모델을 이미지에 캐시(`Dockerfile:15-17`)하므로 **컨테이너 배포는 런타임 다운로드 불필요**(AI-1 해결로 빌드가 실제로 완료됨).

---

## 🟠 MAJOR — 뜨긴 하나 기능이 동작 안 함

### AI-4. 벡터스토어(ChromaDB) 미구축 → RAG가 항상 빈 컨텍스트(법령 근거 없는 답) — ✅ 해결(2026-07-22)
- `ai/data/chroma` 미존재, `init_services`가 비면 경고만 로그. 인덱싱은 수동 스텝(`python -m app.scripts.index_documents`)뿐이라 startup에서 자동 호출 안 함 → `similarity_search`/BM25 모두 `[]` → RAG 답변이 근거·인용 없이 생성.
- **✅ 적용**: (1) `run_indexing`을 순수 함수로 리팩터 — 내부 `argparse.parse_args()`·`VectorStoreService` 재생성 버그 제거, **전달받은 인스턴스 재사용**(임베딩 이중 로드 방지, AI-3과 정합). CLI `--force`는 `main()`으로 분리. (2) `init_services`에서 Chroma가 비면 `run_indexing(_vectorstore_service)`를 **자동 호출**(실패해도 경고 후 빈 RAG로 계속 — graceful). blocking이라 최초 기동만 느리고, 볼륨 지속으로 이후 기동은 스킵. (3) compose `ai` healthcheck `start_period` 30s→**600s**(lifespan 인덱싱 중 포트 미개방으로 인한 unhealthy 오탐 방지).

### AI-5. 세목 분류 엔드포인트 비작동 — 파인튜닝 모델 부재 — ✅ 해결(규칙기반 폴백, 2026-07-22)
- 모델(`models/tax_classifier`) 없으면 `TaxClassifierService.__init__`이 `FileNotFoundError` → init에서 서비스 None → `POST /api/v1/transaction/classify`가 `AIServiceError`로 500.
- **✅ 적용**: (1) `__init__`을 모델 부재/로드실패 시 예외 없이 `_model=None`으로 **degrade**하도록 변경. (2) `classify()`가 `_model is None`이면 신규 `app/services/rule_based_classifier.py`의 `classify_by_rules`로 위임 — 20개 세목 키워드 부분일치 스코어링, `method="rule_based"`, 매칭 없으면 "기타경비"+`needs_fallback`. (`scripts/data/tax_categories.py`의 예시는 dockerignore 대상이라 런타임 앱 아래로 키워드 맵을 자체 구성.) (3) `dependencies.py` 초기화가 이제 항상 성공 → 500 제거. 파인튜닝 모델이 생기면 자동으로 ML 경로(`method="local_model"`) 사용.
- 검증: 규칙기반 모듈을 실제 실행해 11개 샘플 분류 확인(주유→차량유지비, AWS 클라우드→지급수수료, KTX 출장→여비교통비, 재산세→제세공과금, 매칭없음→기타경비 등).

### INT-1. AI→BE 개인화 데이터 호출이 존재하지 않는 경로/인증 → 해당 인텐트 챗봇 실패 — 🟡 부분 해결(2026-07-22)
- `backend_client.py:67,96`가 `GET /api/v1/users/{id}/transactions`·`/business-info`를 `Authorization: Bearer {backend_api_key}`(기본 `""`)로 호출. **BE엔 `/api/v1/users/**` 없음**, BE는 사용자 JWT만 인증(정적 키 경로 없음).
- 호출부 `chat_service.py`에 try/except가 없어 `be_data_required: true` 인텐트(`tax_intents.json`의 2개)에서 `BackendClientError` 전파 → 챗봇 500.
- **✅ 적용(크래시 제거)**: `chat_service.py`의 백엔드 보강 블록을 try/except로 감쌈 → 실패 시 경고 로그 후 **개인화만 생략하고 일반 답변을 정상 반환**(500 → graceful degrade). 검색(retrieval) 재raise는 유지(필수 경로).
- **⬜ 남음(개인화 실동작)**: 개인화 데이터를 실제로 쓰려면 BE에 내부 인증 포함 사용자 데이터 엔드포인트(거래내역/사업자정보) 추가 + `backend_client`의 경로·인증 정합 필요. 현재는 개인화 없이 답변.

### INF-1. compose `worker` — 약한 JWT 기본값으로 크래시 루프 + Redis 인증 갭 — ✅ 해결(2026-07-22)
- `JWT_SECRET:-default-secret`(14바이트) → jjwt `Keys.hmacShaKeyFor`(raw UTF-8, HS256는 ≥32바이트 요구)에서 WeakKeyException → 기동 실패 + `restart: on-failure` 루프.
- **✅ 적용**: worker의 약한 기본값 제거 → `JWT_SECRET: ${JWT_SECRET}`(AES와 동일하게 시크릿=기본값 없이 필수). 백엔드(bootRun)와 worker가 `docker compose up`에 전달된 **동일 JWT_SECRET** 공유(독립 기본값 불일치 방지). README 데모 예시 키(`demo-jwt-secret-key-must-be-at-least-32-bytes!!`, 47바이트)가 유효해 문서 경로대로면 worker 정상 기동. Redis 비번 갭은 BE-1에서 worker에 `REDIS_PASSWORD` 추가로 이미 해결.
- ⬜ 참고(범위 밖): worker는 전용 프로파일/`application-worker.yaml` 없이 백엔드 전체를 복제하는 구조(기능엔 지장 없음, 설계 정리 항목).

### BE-3. 간편장부 부가세 계산 단위테스트 3건 실패 (신규 발견 → ✅ 해결, 2026-07-22)
- `./gradlew test` 결과 **308개 중 3개 실패**. 전부 `BookEntryServiceTest`(순수 Mockito 단위테스트 — Spring 컨텍스트/DB/Redis 무관 → **BE-1·BE-2 config 수정과 무관한 기존 실패**):
  - `create_과세거래_부가세자동분리`, `create_수입거래_incomeAmount설정`, `create_고정자산_fixedAssetAmount설정` — 실제값이 기대값의 정확히 ×1.1(예: 기대 50,000 ↔ 실제 55,000 / 100만↔110만 / 300만↔330만).
- 원인: `db/migration/V3__fix_book_entry_amounts.sql`가 `income/expense/fixedAsset_amount = supply_price + vat_amount`(원 결제금액=부가세 **포함**)로 의미를 **의도적으로 변경**했고 서비스 코드(`BookEntryService.create`)는 이를 따르나, **단위테스트 기대값이 옛 의미(부가세 제외 공급가액)로 남아 있었음** → stale test.
- **✅ 적용**: 코드의 VAT-포함 의미를 정본으로 확정하고, 3개 테스트의 `expense/income/fixedAssetAmount` 기대값을 원 결제금액(55,000 / 1,100,000 / 3,300,000)으로 갱신. `vatAmount`·`supplyPrice` 단언은 원래 옳았으므로 유지. → `BookEntryServiceTest` 17/17 통과, **전체 308개 통과**.

---

## 🟡 MINOR / 잠재

- **AI-6.** compose가 빈 `ANTHROPIC_API_KEY` 주입(`docker-compose.yml:42`, `.env` 없으면 "") → `Settings`가 ""도 허용해 기동·health는 green이나, 첫 chat에서 `ChatAnthropic(api_key="")` → `AuthenticationError`. "healthy인데 chat 실패"의 함정.
- **INT-2.** `account_type` 쿼리 파라미터 불일치(잠재): `PayApi.kt:16` `@Query("account_type")` vs `AccountController.java:35` `accountType`. 단 현재 FE 호출부는 전부 type 미전달(`getAccounts()`)이라 **미발현**. 향후 타입 필터 도입 시 필터가 조용히 무시됨.
- **SEC-1.** ✅ 해결(2026-07-22): `/api/payments/qr/merchant-token/**` 전체 `permitAll`이라 `PaymentController.payMerchantQr`(`@AuthenticationPrincipal Long userId`)가 미인증 도달 → `userId=null` NPE + 무인증 결제 실행 가능. **조치**: `POST .../{token}/pay`만 `.requestMatchers(HttpMethod.POST, ".../*/pay").authenticated()`로 분리(blanket permitAll보다 먼저 평가). 결제 실행은 인증 필수(미인증 401)로 바뀌고, 나머지(생성·조회·SSE 구독)는 공개 유지. BE 308 테스트 통과(회귀 없음).
- **CFG-1.** `TEST_LOGIN_ENABLED` 지침 무효: README:79 안내하나 `app.test-login.enabled`는 하드코딩 `false`(`application.yaml:38-40`), env 플레이스홀더 없음. (로그인이 OTP-게이트가 아니라 실제 영향은 적음)
- **CFG-2.** ✅ 해결(2026-07-22): Flyway 부재로 `db/migration/V2·V3__*.sql`이 자동 적용 안 되던 문제. **V2**(2025 세금 데이터)는 `02_seed_data.sql`에 동일 데이터가 이미 있고 `Tax*DataInitializer`(멱등)가 매 기동 시 보장 → **이미 자동 적용**(추가 조치 불필요). **V3**(간편장부 금액 정규화)만 실제 갭이라 `BookEntryAmountBackfillInitializer`(멱등 `CommandLineRunner`, `WHERE ... <> supply_price+vat_amount` 가드) 추가 → 매 기동 시 레거시 행만 1회 보정(신규/정규화 완료 DB는 no-op, 지속 볼륨도 커버). `db/migration/README.md`에 전략 문서화. BE 308 테스트 통과.
- **CFG-3.** ✅ 해결(2026-07-22): opus 모델 ID `claude-opus-4-7` → **`claude-opus-4-8`**(현행 권장, claude-api 스킬로 확인). `config.py` 기본값·`docker-compose.yml` 기본값·`ai/.env.example`·`ai/README.md`·`exec/포팅_매뉴얼.md` 전부 갱신. haiku `claude-haiku-4-5-20251001`는 유효해 유지(변경 없음).

---

## ✅ 정상 확인 (안심 근거)
- **뱅킹은 100% 로컬 DB 원장**: `SsafyFinanceClient`/`SsafyCreditCardClient`는 in-repo 구현(HTTP/RestTemplate 전무, `AccountRepository`/`BankTransactionRepository`로 원장 처리, 신규 계좌 데모 시드 5,000,000). 외부 금융망·키 불필요 — README 주장 사실.
- **FE↔BE 나머지 매핑 정상**: auth, card, bookentry, payment/QR, classification, export, taxcalendar, pay/pin 등 경로·verb·DTO 일치.
- **BE→AI(정방향) 정상**: `/api/v1/chat/`·`/history/{id}`·`/transaction/classify` 경로·스키마 일치(trailing slash 포함).
- **JWT principal = `Long userId` 정상**(`JwtAuthenticationFilter:36-39`) → 전 컨트롤러 `@AuthenticationPrincipal` 정상.
- **스키마↔엔티티**: CHECK/enum 충돌 없음, 시드 테이블명 일치, 데이터 초기화 idempotent, `ssafy.oauth.*`는 미소비 dead config(부팅 차단 안 함).
- **SMS(Solapi)/FCM 부재**는 부팅·핵심 로그인 차단 안 함(로그인은 identity-mock+PIN).

---

## 🚩 우선순위 조치 체크리스트 (✅=적용 완료 / ⬜=잔여)
1. ✅ **(BE-1)** Redis password 설정 추가/전달 — 로그인 복구.
2. ✅ **(BE-2)** README bootRun env 수정 + DB 기본값 정렬 — BE 기동 복구.
3. ✅ **(AI-1/AI-2/AI-3)** `ai/models/.gitkeep`+gitignore, `anthropic_api_key` 기본값+startup 경고, 임베딩 공유 provider — AI 기동/빌드 복구.
4. ✅ **(AI-4)** 앱 기동 시 Chroma 비면 자동 인덱싱 + healthcheck start_period 상향 — RAG 근거 복구.
5. ✅ **(AI-5)** 모델 부재 시 규칙기반 폴백(keyword)으로 degrade — /classify 500 제거.
6. 🟡 **(INT-1)** 챗봇 try/except로 크래시 제거 ✅ / BE 사용자 데이터 엔드포인트는 잔여 ⬜ — 개인화 실동작 복구.
7. ✅ **(INF-1·SEC-1·CFG-2·CFG-3)** worker JWT ✅ + merchant-token pay 인증 ✅ + V2/V3 마이그레이션 런타임 자동적용 ✅ + opus 모델 ID→4.8 ✅.
8. ✅ **(BE-3)** 간편장부 부가세 테스트 기대값 갱신 — BE 테스트 그린.

---

## 재현/검증 방법
- **Redis 갭**: compose 기동 후 `/api/auth/login` 호출 → 500(NOAUTH) 확인.
- **AI import**: `.env` 없이 `cd ai && python -c "import app.main"` → ValidationError.
- **Docker AI 빌드**: `docker compose build ai` → `COPY models/` 실패.
- **BE 전체 테스트**(2026-07-22 실행): `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" && cd BE && ./gradlew test` → **308개 전부 통과 / 1 skip**(BE-3 테스트 기대값 갱신 후). BE-1·BE-2 config 변경으로 인한 회귀 없음.
