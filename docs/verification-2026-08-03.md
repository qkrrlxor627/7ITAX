# 2026-08-03 전체 스택 구동 검증

`chore/repo-cleanup` (67bb628) 기준으로 BE / FE / ai / db / infra를 실제로 빌드·기동해
기능별로 호출한 결과를 기록한다. 정적 분석이 아니라 전부 실행 결과다.

## 검증 환경

- Windows 11, Docker 29.1.2, OpenJDK 21(호스트) / Liberica JDK 17(Gradle toolchain), Node 24.11.1
- 로컬 Python은 3.9뿐 — `ai/`는 3.11 필요(`Dockerfile`), 따라서 ai는 Docker 경로로만 검증
- Android SDK: platform 36 / build-tools 36.0.0 설치됨

## 요약

| 영역 | 결과 |
|------|------|
| BE 빌드 + 단위테스트 | ✅ 308 통과 / 실패 0 / 오류 0 / 스킵 1 |
| BE 기동 | ✅ `:8080`, 38.5s (`Started Tax7iApplication`) |
| BE 기능 (컨트롤러 16개) | ✅ 호출한 엔드포인트 전부 200 |
| FE Android 빌드 | ✅ `app-debug.apk` 생성 (1m17s) |
| FE webview | ❌ 미구현 (추적 파일 28개 중 24개가 빈 파일) |
| FE 루트 Vite 스캐폴드 | ❌ 프로젝트와 무관한 잔재 17개 |
| ai 이미지 빌드 | ✅ 9.73GB (14m43s) |
| ai 테스트 | ⚠️ 420 통과 / 5 실패 / 22 오류 |
| ai 서비스 기동 | ❌ 첫 기동 인덱싱 약 3.8시간 소요 → healthcheck 유예(600s) 초과, worker 미기동 |
| docker compose config | ✅ 유효 |
| db 스키마 + 시드 | ✅ 25테이블, 참조데이터 적재 확인 |

## BE — 기능별 호출 결과

`app.test-login`으로 JWT를 발급받아 호출했다. 전부 `200`.

| 기능 | 엔드포인트 | 확인 내용 |
|------|-----------|----------|
| 인증 | `POST /api/auth/test-login` | accessToken/refreshToken 발급 |
| 계좌 | `GET /api/banking/accounts` | 빈 목록 정상 응답 |
| 카드 | `GET /api/cards`, `/merchants`, `/products`, `/accounts` | 가맹점 84건·카드상품 시드 반환 |
| 결제 PIN | `GET /api/pay/pin/status` | `registered: false` |
| 장부 | `GET /api/book-entries`, `/summary`, `/unconfirmed-count` | 페이징·집계 구조 정상 |
| 종합소득세 | `POST /api/tax/calculate` | 공제 1,500,000 / 세율 6% 계산 |
| 절세 | `GET /api/tax/savings`, `/savings/summary` | 노란우산공제 등 추천, 접대비·차량 한도 추적 |
| 신고 | `GET /api/tax/returns`, `/api/tax/vat-returns` | 목록 조회 정상 |
| 캘린더 | `GET /api/tax-calendar/deadlines` | 마감일 12건 |
| 추정 | `GET /api/tax-estimation`, `/monthly` | 부가세 2기·소득세 추정 반환 |
| 내보내기 | `/export/vat`, `/income-tax`, `/book-entries`, `/vat/excel`, `/income-tax/excel`, `/income-tax/pdf`, `/simple-ledger/pdf` | 7개 전부 실제 XLSX(PK..)/PDF(%PDF-1.4) 바이트 생성 |
| AI 챗봇 | `POST /api/chatbot`, `GET /history/{id}` | mock 모드 응답 정상 |
| 세목분류 | `POST /api/classification` | 신뢰도 `RECOMMENDED`(90), 법적근거 `소득세법§33①5` |

## 발견된 결함

### 1. [BE] 내보내기 응답에 `Content-Disposition` 헤더가 누락된다

`ExportController.java:88-90`

```java
String encoded = URLEncoder.encode(filename, UTF_8).replace("+", "%20");
return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
```

`filename*=UTF-8''` (RFC 5987) 부분은 올바르나, 앞의 `filename="..."`에 한글 원문이 그대로 들어간다.
HTTP 헤더는 ISO-8859-1이라 Tomcat이 인코딩에 실패한다.

```
java.lang.IllegalArgumentException: The Unicode character [부] at code point [48,512]
cannot be encoded as it is outside the permitted range of 0 to 255
  at org.apache.coyote.http11.Http11OutputBuffer.sendHeader
```

본문(PDF/XLSX)은 정상 전송되어 응답은 200이지만, **헤더 전체가 응답에서 빠진다.**
실제 응답 헤더에 `Content-Disposition`이 없음을 확인했다 → 클라이언트가 파일명을 못 받는다.
export 엔드포인트 7개 전부 해당. 수정 방향: `filename=` 파라미터는 ASCII 대체명으로 두고
한글은 `filename*`에만 담는다.

### 2. [BE] 필수 쿼리 파라미터 누락 시 400이 아니라 500이 반환된다

`GlobalExceptionHandler`가 처리하는 예외는 `BusinessException`,
`MethodArgumentNotValidException`, `DataIntegrityViolationException`,
`OptimisticLockingFailureException` 4종뿐이다.
`MissingServletRequestParameterException`이 빠져 있어 generic 핸들러로 떨어진다.

```
GET /api/tax/savings          → 500 INTERNAL_SERVER_ERROR
GET /api/tax-estimation/monthly?year=2025  → 500 (month 누락)
```

클라이언트 오류가 서버 오류로 보고되어 원인 파악이 어렵다.

### 3. [ai] 리팩터링 이후 갱신되지 않은 테스트 27개

임베딩 로딩이 `app/services/embeddings.py`의 `get_embeddings()` 팩토리로 분리되면서
`vectorstore.py`는 더 이상 `HuggingFaceEmbeddings`를 직접 import하지 않는다.
그런데 테스트는 여전히 예전 위치를 patch한다.

```
AttributeError: <module 'app.services.vectorstore'> does not have the attribute 'HuggingFaceEmbeddings'
AttributeError: <module 'app.routers.transaction'> does not have the attribute '_llm_fallback'
```

- `tests/test_vectorstore.py` → `app.services.vectorstore.HuggingFaceEmbeddings`
- `tests/test_embedding_service.py` → `app.services.embedding_service.HuggingFaceEmbeddings`
- `tests/test_transaction_router.py` → `app.routers.transaction._llm_fallback`

프로덕션 코드 자체는 정상 동작한다(서비스 기동 시 임베딩 로드 성공). 테스트만 stale.

### 4. [ai] 첫 기동 인덱싱이 healthcheck 유예를 초과해 worker가 기동되지 않는다

빈 Chroma 볼륨으로 처음 기동하면 lifespan에서 세법 PDF 9종을 임베딩한다.
`중복 chunk_id 제거: 6500개 → 6246개` 이후 bge-m3를 CPU로 돌린다. 실측치:

| 경과 | Chroma `embeddings` 행 수 | 상태 |
|------|--------------------------|------|
| 37분 | **1,000 / 6,246** | CPU 568%, 정상 진행 중(hang 아님) |

이 속도면 전체 인덱싱에 **약 3.8시간**이 걸린다.
`docker-compose.yml`의 `start_period: 600s`(10분)와 자릿수가 다르다.
유예를 넘기면 컨테이너가 `unhealthy`가 되고,

```yaml
worker:
  depends_on:
    ai:
      condition: service_healthy
```

이 조건이 만족되지 않아 **worker가 영구히 기동되지 않는다.**
`start_period`를 늘리는 것만으로는 부족하다(3.8시간). 인덱싱을 lifespan에서 분리해
별도 1회성 작업으로 돌리고, 인덱싱된 Chroma 볼륨을 산출물로 배포하거나
빌드 시점에 인덱스를 만들어 이미지에 포함하는 쪽이 현실적이다.
GPU(`EMBEDDING_DEVICE=cuda`)를 쓸 수 있으면 그것도 대안이다.

### 5. [ai] reranker 모델이 이미지에 사전 포함되지 않는다

`Dockerfile`은 임베딩 모델만 빌드 시 캐시한다.

```dockerfile
# 런타임 HuggingFace 네트워크 의존 제거 + 컨테이너 재생성 시 재다운로드 방지
RUN python -c "... SentenceTransformer('${EMBEDDING_MODEL}')"
```

그런데 `reranker_service.py`의 `BAAI/bge-reranker-v2-m3`는 첫 사용 시 런타임에 내려받는다.
주석이 선언한 "런타임 네트워크 의존 제거"가 reranker에는 적용되지 않는다.

### 6. [ai] 테스트 의존성이 어디에도 선언되어 있지 않다

`requirements.txt`는 `=== 프로덕션 의존성 ===`만 담고 있고 `requirements-dev.txt`도 없다.
`pytest.ini`가 `asyncio_mode = auto`(pytest-asyncio 필요)를 쓰는데 `pytest`, `pytest-asyncio`가
선언되지 않아, 새로 받은 사람은 테스트를 돌릴 수 없다. (본 검증에서는 수동 설치했다.)

### 7. [ai] `tests/evaluation/test_ragas_eval.py`의 깨진 설정 참조

`settings.gms_api_key`, `settings.gms_base_url`을 참조하지만 `app/core/config.py`에 두 필드가 없다.
`pytest.ini`의 `addopts = --ignore=tests/evaluation`으로 수집에서 빠져 있어 CI에는 영향이 없으나,
이 스크립트는 현재 상태로 실행 불가다.

### 8. [FE] webview가 사실상 미구현이며, 안드로이드 연동부도 전부 dead code다

`FE/webview` 추적 파일 28개 중 **24개가 빈 파일(2바이트)** 이다.
`App.tsx`, `index.tsx`, `api/client.ts`, `bridge/nativeBridge.ts`, `store/paySlice.ts`,
`pages/*`, `components/*` 전부 비어 있고 `vite.config.ts`도 빈 파일이다.
`package.json`에는 `dependencies`가 아예 없어 `pnpm install` / `build`가 성립하지 않는다.

안드로이드 쪽 연동부도 참조가 0곳이다.

- `util/Constants.kt`의 `WEBVIEW_BASE_URL` — 정의만 있고 사용처 없음
- `bridge/WebBridge.kt` — 어디서도 참조되지 않음
- `app/build.gradle.kts`의 `androidx.webkit` — 미사용 의존성

즉 하이브리드 WebView 계층은 착수 전 상태다. 네이티브 Compose 화면(50개)만으로 동작한다.

### 9. [FE] 루트에 프로젝트와 무관한 Vite 스캐폴드 17개가 남아 있다

`FE/`는 안드로이드 Gradle 프로젝트인데, 루트에 `npm create vite` 기본 산출물이 그대로 있다.

```
FE/index.html          <title>tax-frontend</title>
FE/src/App.tsx         Vite/React 기본 템플릿 (로고 + 카운터 + "Edit src/App.tsx")
FE/src/assets/{react.svg, vite.svg, hero.png}
FE/{eslint.config.js, vite.config.ts, package.json, pnpm-lock.yaml}
FE/tsconfig{,.app,.node}.json, FE/public/{favicon,icons}.svg
```

67bb628의 저장소 정리에서 누락된 잔재다. `node_modules`도 없어 한 번도 빌드된 적이 없다.

## 저장소 결함이 아닌 환경 이슈 (참고)

이번 검증에서 기동을 막았지만 저장소 문제는 아닌 것들:

1. **포트 충돌** — 5432/6379를 다른 프로젝트(`dailybrief-postgres`, `dailybrief-redis`)가 점유 중.
   compose override로 55432/56379로 우회했다.
2. **오래된 postgres 볼륨** — 2026-06-09에 만들어진 볼륨이 남아 있어
   `Skipping initialization`으로 `db/init/*.sql`이 실행되지 않는다(스키마·시드는 이미 적재됨).
   저장된 비밀번호가 compose의 `POSTGRES_PASSWORD`와 달라도 무시된다.
3. **`application-local.yaml`이 환경변수를 무력화** — 이 파일은 `BE/.gitignore:44`로
   **추적되지 않는 로컬 전용 파일**이다. 신규 클론에는 없으므로 저장소 결함은 아니다.
   다만 이 PC에서는 `jdbc:postgresql://localhost:5432/tax7i`를 하드코딩해
   README가 안내하는 `DB_HOST`/`DB_PORT`를 덮어써서, 기동이 엉뚱한 DB로 붙어 실패했다.
   Spring은 환경변수를 설정 파일보다 우선하므로 `SPRING_DATASOURCE_URL`로 우회했다.
4. **로컬 Python 3.9** — `ai/`는 3.11 필요. 로컬 venv 경로는 불가하고 Docker만 가능하다.

## 재현 명령

```bash
# 인프라 (포트 충돌 시 override 사용)
docker compose up -d postgres redis

# BE 테스트 + 기동
cd BE && ./gradlew cleanTest test
AES_ENCRYPTION_KEY=<base64 32B> JWT_SECRET=<32B+> REDIS_PASSWORD=ssafy ./gradlew bootRun

# FE 안드로이드
cd FE && ./gradlew :app:assembleDebug

# ai 이미지 빌드 + 테스트
docker compose build ai
docker run --rm -v "$PWD/ai:/src" -w /src 7itax-ai:latest \
  sh -c "pip install -q pytest pytest-asyncio && python -m pytest -q"
```
