# 로컬 구동 블로커 점검 (2026-08-03)

> `chore/repo-cleanup` 브랜치 기준으로 "지금 이 프로젝트를 구동하려면 무엇을 고쳐야 하는가"를 전수 조사한 결과.
> 기존 `0722ready.md`(배포 준비 진단 트래커)가 다루는 기능 결함과 달리, 이 문서는 **환경·포트·프레시 클론 재현성**에 초점을 맞춘다.

**점검 범위:** BE + DB/Redis, AI 서비스(FastAPI), Android 앱. 모니터링 스택은 범위 밖이나 발견 항목은 D절에 기록.

---

## 요약

핵심 결론 두 가지.

1. **코드는 깨져 있지 않다.** 직전 정리 커밋(`67bb628`)이 삭제한 5개 클래스는 참조가 0곳이고, 191개 `com.ssafy.tax7i.*` import가 전부 해소된다. BE 테스트 308개 통과 기록(`C:/tmp/tax7i-build/test-results`, 실패 0·에러 0)도 정리 커밋 **이후** 시각이다.
2. **막고 있는 것은 환경과 설정이다.** 이 머신에서는 포트 충돌이, 프레시 클론에서는 gitignore된 설정 파일 부재가 실질 블로커다.

---

## 1. 환경 점검 결과

| 항목 | 상태 | 판정 |
|---|---|---|
| JDK 21 (Liberica, `JAVA_HOME`) | 설치됨 | OK |
| JDK 17 (Liberica) — `build.gradle` toolchain 요구 | `C:\Program Files\BellSoft\LibericaJDK-17` 존재 | OK |
| Gradle wrapper (BE 8.14.4 / FE 9.3.1) | jar·properties 존재, dist 캐시됨 | OK |
| Node 24.11.1 / pnpm 10.28.0 | 설치됨 | OK |
| **Python 3.9.13 (및 3.8)** | `ai/Dockerfile`은 **3.11** 기준 | **부적합** |
| Docker 29.1.2 / Compose v2.40.3 | 설치됨 | OK |
| Android SDK 36 + build-tools 36.0.0 + AVD(API 29) | 설치됨 | OK |
| 저장소 루트 `.env` | **없음** (`ai/.env.example`만 존재) | 블로커 |

### 포트 점유 현황 — 충돌의 근원

| 호스트 포트 | 현재 점유자 | compose가 바인딩하려는 것 |
|---|---|---|
| 5432 | `dailybrief-postgres` (가동 중) + 네이티브 postgres 서비스 | `tax7i-postgres` |
| 6379 | `dailybrief-redis` (가동 중) | `tax7i-redis` |
| 55432 / 56379 | `tax7i-postgres` / `tax7i-redis` (수동 기동됨) | — |
| 8080 | 비어 있음 (BE 미기동 상태로 확인) | BE `bootRun` |

`tax7i-postgres`/`tax7i-redis`가 55432/56379에 떠 있는 것은 `docker-compose.yml`이 아니라 수동 기동의 결과다. compose 파일과 실제 상태가 어긋나 있다.

---

## 2. 블로커 목록

### A. 하드 블로커 — 이걸 안 고치면 기동 자체가 안 됨

| # | 항목 | 위치 | 증상 |
|---|---|---|---|
| A-1 | compose가 이미 점유된 5432/6379를 바인딩 | `docker-compose.yml:6,24` | `docker compose up -d` → `port is already allocated` 즉시 실패 |
| A-2 | BE 기본값이 잘못된 DB를 가리킴 | `application.yaml:7,14` (`localhost:5432` / `6379`) | 실제 `tax7i-postgres`는 55432. 무설정 `bootRun`은 **다른 프로젝트의 DB**에 붙는다 |
| A-3 | `application-local.yaml`이 gitignore됨 | `BE/.gitignore` 마지막 줄 · `git ls-files`에 없음 | 프레시 클론에서 `AES_ENCRYPTION_KEY`/`JWT_SECRET`(설정 전체에서 **기본값 없는 유일한 2개**, `application.yaml:32,35`)이 해소 불가 → `EncryptionProperties.java:12-15`가 `IllegalStateException` |
| A-4 | 설정 우선순위 함정 | `application.yaml:4-5` `spring.config.import` — **프로필과 무관하게 무조건** import | import된 문서가 아래에 삽입되어 `application-local.yaml`의 하드코딩 `localhost:5432`가 `${DB_PORT}` 플레이스홀더를 덮어쓴다. 즉 `DB_PORT=55432`가 무시된다 — `README.md:73`의 안내와 정면 배치 |
| A-5 | `ai` 서비스가 로컬 Python으로 실행 불가 | `ai/Dockerfile:1` (`python:3.11-slim`) vs 로컬 3.9.13 | chromadb/torch/pydantic-settings 등이 3.11 전제. Docker 경로만 현실적 |
| A-6 | `ai/.env` 부재 | `ai/.env.example`만 존재, `.gitignore:646`이 `.env` 제외 | 기동은 되지만(`config.py:16` 기본값 `""`) 첫 챗 호출에서 401/500 |
| A-7 | compose 시크릿에 기본값 없음 | `docker-compose.yml:42,80,81`, `env_file:` 지시자 0개 | 맨몸 `up -d` → 빈 문자열 → jjwt HS256이 32B 미만 거부 → `worker` 크래시 루프 |
| A-8 | **FE `gradle-wrapper.jar`가 미추적** | 루트 `.gitignore:261` `*.jar` | 프레시 클론에서 `./gradlew` → `Could not find or load main class GradleWrapperMain`. **BE 쪽 jar은 추적되고 있어 일관성 문제**이기도 하다 |
| A-9 | `FE/webview`가 빈 껍데기 | `webview/src/**` 전부 2바이트(CRLF), `package.json` 의존성 0개, `index.html`이 루트 아닌 `public/` | install·build·run 전부 불가. `vite.config.ts`에 default export 없음 |

### B. 설정값 필요 — 기동은 되지만 기능이 죽음

| # | 항목 | 근거 | 영향 |
|---|---|---|---|
| B-1 | Redis 포트 불일치 | `application-local.yaml:6-10`은 `localhost:6379`, 실제는 56379 | Redis는 lazy라 기동은 통과. `CacheConfig.java:60-83`이 캐시 예외를 삼키지만 **OTP·리프레시 토큰 경로는 런타임 예외** |
| B-2 | `ANTHROPIC_API_KEY` 미설정 | `docker-compose.yml:42`, `ai/.env` 부재 | 컨테이너는 healthy, 첫 챗에서 500 (`0722ready.md:72`의 AI-6과 동일 증상) |
| B-3 | AI 최초 기동 = 대용량 다운로드 | `embeddings.py`(bge-m3 ~2GB), `reranker_service.py:24`(~560MB), `dependencies.py:68-84`(PDF 9종 자동 인덱싱) | `start_period: 600s`가 이 때문. 네트워크 필수이고, 멈춘 것처럼 보인다 |
| B-4 | `FE/local.properties`가 머신 종속 | `FE/.gitignore:15` | 다른 머신은 `sdk.dir` 또는 `ANDROID_HOME`을 직접 설정해야 함 |
| B-5 | Android 릴리스 URL이 폐기된 서버 | `Constants.kt:6-14` → `https://j14c203.p.ssafy.io` | 디버그 경로(`10.0.2.2:8080`)는 정상. **릴리스 빌드는 사망**. 실기기도 `10.0.2.2` 불가(에뮬레이터 전용 별칭) |
| B-6 | `db/seed/test_book_entries.sql`이 미마운트 | compose는 `./db/init`만 마운트 | 시연 시나리오 4/5/7이 쓰는 원장 데이터가 안 들어감. V2/V3는 런타임 initializer가 커버하므로 문제 없음 |

### C. 구조적 결함 — 당장 막지는 않지만 재현성을 깨뜨림

| # | 항목 | 위치 |
|---|---|---|
| C-1 | `application-local.yaml`이 worker 이미지에 섞여 들어감 | `BE/.dockerignore` **부재** + `Dockerfile:9` `COPY src src` + A-4의 무조건 import → 컨테이너가 `localhost`를 해석하고 mock/test-login 플래그가 조용히 활성화. **로컬에 이 파일이 있느냐에 따라 빌드 산출물이 달라진다** |
| C-2 | `worker` 프로필 설정 없음 | `docker-compose.yml:67`이 `SPRING_PROFILES_ACTIVE: worker`를 주지만 `application-worker.yaml`이 없고 `@Profile("worker")` 빈도 0개 → 데이터 initializer 포함 앱 전체가 중복 기동 |
| C-3 | 루트 `Dockerfile` 빌드 불가 | `BE/Dockerfile`의 복제본인데 루트 컨텍스트엔 `gradlew`/`build.gradle`/`src`가 없어 `COPY gradlew .`에서 실패. 참조하는 곳도 없는 사문 |
| C-4 | foojay toolchain resolver 없음 | `BE/settings.gradle` — JDK 17이 없는 머신은 `No matching toolchains found`로 자동 다운로드 없이 실패 |
| C-5 | 테스트가 워킹트리를 더럽힘 | `SampleExportGenerator.java:19`가 `../docs/samples`에 기록 → `./gradlew test`마다 9개 바이너리가 modified 상태로 남음 |
| C-6 | 빌드 출력 경로 하드코딩 | `build.gradle:12-14`가 `C:/tmp/tax7i-build`로 고정 → 체크아웃 2개가 충돌하고 IDE 툴링이 혼선 |
| C-7 | `TEST_LOGIN_ENABLED` 문서-코드 불일치 | `README.md:89`가 이 env var를 안내하지만 `application.yaml:40-41`은 `false` 하드코딩에 `${}`가 없다 → 해당 env var는 무효 |
| C-8 | CI 없음 | `.github/`에 워크플로 0개. `Jenkinsfile`은 폐기된 SSAFY 호스트(`/home/ubuntu/.env.backend`, `ubuntu_default` 네트워크) 대상 |
| C-9 | `ai;C` 빈 디렉터리 | 셸 인용 사고 잔재(`cd ai;C:\...`). 내용 0, 미추적 |
| C-10 | `FE/src`가 Vite 기본 템플릿 | 카운터 데모. `pnpm build`는 성공하지만 의미 없음. `FE/nginx.conf:11-12`는 존재하지 않는 `tax-backend` 서비스로 프록시 |

### D. 범위 밖(모니터링) — 참고용

- Prometheus가 `host.docker.internal:8080`을 스크랩 → `bootRun` 없이는 `BackendDown` 알람 상시 발생 (`monitoring/prometheus/prometheus.yml:18-23`, `alert.rules.yml:5-13`)
- Grafana 프로비저닝 `dashboard.yml:12`가 자기 자신이 있는 디렉터리를 가리켜 파싱 실패 로그 발생
- `redis-data` 볼륨이 `/var/lib/redis/data`에 마운트 — Redis 실제 경로는 `/data`라 무의미
- `ai` 서비스에 `extra_hosts` 없음 → Linux/CI에서 `host.docker.internal` 해석 실패 (`docker-compose.yml:49` vs `:103-105`)

### E. 확인했고 문제 없는 항목

정리 커밋 이후 의심될 만하지만 실제로는 정상인 것들. 재조사 낭비를 막기 위해 기록한다.

- **정리 커밋이 삭제한 5개 클래스** — 참조 0곳, 컴파일 영향 없음
- **Gradle wrapper jar(BE) 및 의존성 캐시** — `com.solapi:sdk`, `firebase-admin`, `openhtmltopdf`, `hypersistence-tsid`, `poi` 전부 캐시됨 → 오프라인 빌드 가능
- **Claude 모델 ID** `claude-opus-4-8`, `claude-haiku-4-5-20251001` — 둘 다 현재 유효한 live 모델
- **`ai/models/`가 `.gitkeep`뿐** — 의도된 것. `tax_classifier_service.py:70-92`가 규칙 기반 분류기로 폴백
- **`application-test.yaml`은 H2 인메모리** → `./gradlew test`에 외부 서비스 불필요
- **SSAFY 금융망** — 키 불필요. `SsafyFinanceClient.java:25-42`는 이미 DB 기반 로컬 원장이고 `ssafy.oauth.*`는 사문

---

## 3. 미해결 결정 사항 — 실행 방식

A-1/A-2의 수정 방향이 여기서 갈린다.

| 안 | 내용 | 전제 조건 |
|---|---|---|
| **하이브리드 (권장)** | 인프라는 compose, BE는 호스트 `bootRun` | `README.md`·`exec/포팅_매뉴얼.md`가 이미 전제하는 방식. A-1을 포트 재매핑으로 해결하면 됨 |
| 전부 compose | worker 컨테이너까지 compose 하나로 | C-1·C-2를 먼저 고쳐야 함 |
| 전부 로컬 (Docker 없이) | Postgres/Redis도 로컬 설치본 사용 | 5432/6379 점유 문제를 별도 해결해야 함 |

---

## 4. 이 머신에서 지금 당장 BE를 띄우는 명령

코드 수정 없이 A-2·A-4·B-1을 우회하는 방법. 환경변수가 설정 파일보다 우선하므로 `SPRING_DATASOURCE_URL`을 통째로 덮어써야 한다 (`DB_PORT`만 주면 A-4 때문에 무시된다).

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/tax7i \
SPRING_DATA_REDIS_PORT=56379 \
./gradlew bootRun
```

`AES_ENCRYPTION_KEY`/`JWT_SECRET`은 로컬에 존재하는 `application-local.yaml:33-37`이 공급한다. 단 그 파일은 미추적이므로, **다른 사람이 클론하면 A-3에 바로 막힌다.**

---

## 5. 우선순위 제안

1. **A-3 / A-8** — 프레시 클론이 아예 불가능한 상태를 먼저 푼다. `application-local.yaml.example` 추가와 FE wrapper jar 추적(`git add -f`, 또는 `.gitignore`에 negation 규칙)
2. **A-1 / A-2 / A-4** — 포트와 설정 우선순위. 실행 방식(3절)을 정한 뒤 착수
3. **A-7 / A-6** — `.env.example` 기반의 루트 `.env` 도입과 compose `env_file:` 연결
4. **C-1** — `BE/.dockerignore` 추가 및 `spring.config.import`의 프로필 게이팅. 이미지 재현성 문제라 배포 전에는 반드시
5. 나머지 C절 항목은 정리 작업으로 묶어서 진행
