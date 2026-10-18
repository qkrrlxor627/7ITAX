# CLAUDE.md

7iTAX — 개인사업자 세무 자동화. BE(Spring Boot) · FE(Android Compose) · ai(FastAPI) · PostgreSQL/Redis/ChromaDB.

## 현재 진행 상황 (2026-10-18)

`docs/PRE_CICD.md`(CI/CD 직전 완성 지시서) 0~9단계 구현 완료, 브랜치 `chore/pre-cicd`. 단계별 근거는 `WORK_LOG.md` 최상단 항목들.

- 기능 검증 구역 11개의 정의·의존 순서·구역별 지뢰는 `WORK_LOG.md`의 "기능 검증 구역 분할" 항목에 있다

## 작업 지시서

- 진행 중인 작업은 `docs/PRE_CICD.md`(먼저) → `docs/IMPROVEMENT_PLAN.md` 순서다. **`IMPROVEMENT_PLAN.md`는 아직 저장소에 없다**(PRE_CICD가 참조만 함)
- 단계 하나를 끝내면 `plan-verifier` 에이전트로 검증받고 PASS일 때만 다음 단계로 간다
- 완료 정의: `cd BE && ./gradlew test` + `./scripts/verify_stack.sh` + `docker compose config -q` 통과
- `.claude/hooks/`(Python)가 규칙 위반을 차단한다. 차단 메시지가 나오면 우회하지 말고 메시지대로 고친다
  - 훅이 gradle을 돌리므로 시스템 기본 JDK가 17이 아니면 `.claude/settings.local.json`의 `env.JAVA17_HOME`에 JDK 17 경로를 둔다

## 작업 기록 규칙 (필수)

**저장소에 영향을 주는 작업을 완료할 때마다 `WORK_LOG.md` 맨 위에 항목을 추가한다.** 예외 없음.

- 형식: `## YYYY-MM-DD: 제목` → `### 배경` / `### 작업 내역` / `### 결과` / `### 변경된 파일 목록`
- 최신 항목이 항상 맨 위. 기존 항목은 **사실 기록이므로 수정하지 않는다** — 내용이 뒤집혔으면 새 항목에서 정정한다
- 코드 변경이 없는 분석·조사도 결론이 남을 가치가 있으면 기록한다
- 검증한 내용은 근거를 함께 남긴다 (`파일:줄번호`, 명령어, 테스트 결과 수치)
- 완료하지 못했거나 범위에서 제외한 항목은 "확인했으나 이번 범위에서 제외한 항목"으로 명시한다

## 환경 함정 (먼저 읽을 것)

- **`grep`/`find`가 셸 함수로 가로채져 있고 백엔드 바이너리가 미설치** → 에러를 내면서 **빈 결과 + exit 1**을 반환한다.
  빈 출력이 "매칭 없음"과 구분되지 않아 **거짓 음성**을 만든다. 반드시 `command grep` / `command find`를 쓸 것.
  복구: `node node_modules/@anthropic-ai/claude-code/install.cjs`
- zsh에서는 `--include=*.py` 같은 따옴표 없는 glob이 `no matches found`로 죽는다 → `--include="*.py"`

## 빌드 · 실행

```bash
# 최초 1회: cp .env.example .env 후 AES_ENCRYPTION_KEY(openssl rand -base64 32)·JWT_SECRET(openssl rand -base64 48) 채우기

# 컨테이너로 한 번에 검증 (빌드 → 기동 → healthy → 결제·장부 스모크)
./scripts/verify_stack.sh

# 인프라만 + BE는 bootRun
docker compose up -d postgres redis
cd BE   # JDK 17 필수(시스템 기본 JDK로는 Gradle 구동 불가)
AES_ENCRYPTION_KEY=<키> JWT_SECRET=<32바이트+> REDIS_PASSWORD=ssafy ./gradlew bootRun
./gradlew test          # 324개 통과가 기준선 (2026-10-18 간편 로그인·QR 상태 수정 후, skipped 1)

# AI — 최초 기동 시 bge-m3 약 2GB 다운로드 + Chroma 자동 인덱싱으로 매우 느림
cd ai && uvicorn app.main:app

# FE — Android Studio 없이 커맨드라인 빌드(JDK 17+로 Gradle 구동, JDK 21은 toolchain이 자동 다운로드. 첫 빌드 약 10분)
cd FE && echo "sdk.dir=<Android SDK 경로>" > local.properties && ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html (`SWAGGER_ENABLED`, 운영 false).
  토큰은 `POST /api/auth/simple-login {"phoneNumber":"01022222222"}` → Authorize에 accessToken
- **간편 로그인**(`TEST_LOGIN_ENABLED=true`일 때만): 전화번호 = 사용자. 처음 보는 번호면 사용자·계좌(500만원)·카드(기본 BUSINESS)를
  자동 생성(`TestUserProvisioner`). FE DEBUG 빌드(`BuildConfig.SIMPLE_LOGIN`)는 본인인증·PIN 대신 이 화면으로 시작한다
- **에뮬레이터 1대로 QR 결제**: 앱 [QR 결제] 탭이 1,000원 결제 토큰을 띄우면 `python scripts/confirm_latest_qr.py`가 가맹점 역할로 확정한다

- compose `backend`(8080)와 bootRun은 포트가 겹친다. 둘 중 하나만 쓴다
- `application-local.yaml`은 `BE/.gitignore:44`로 제외돼 클론본에 없다. 직접 만들면 `application.yaml`의
  `optional:classpath:` import로 **프로필 지정 없이** 로드된다(Docker 이미지에는 `BE/.dockerignore`로 제외)
- redis는 compose가 `--requirepass ssafy`로 띄우므로 `REDIS_PASSWORD` 전달 필요
- AES/JWT 시크릿은 의도적으로 기본값 없음(약한 기본값 배포 방지). **AES 키를 바꾸면 기존 DB의 암호화 컬럼을 읽지 못해 BE가 기동 중에 죽는다**
- 시드(`CardDataInitializer`)는 빈 DB에서만 동작한다: 김싸피 `01011111111`(개인 카드), 이싸피 `01022222222`(사업자 카드).
  장부 자동 생성은 결제 purpose가 아니라 **카드 유형이 BUSINESS**일 때만 된다(`PaymentService.java:529`)
- 호스트에 PostgreSQL이 이미 5432를 쓰면 `.env`에 `POSTGRES_HOST_PORT=5433`

## 알려진 미해결 항목

- **INT-1 (개인화)** — `ai/app/services/backend_client.py:67,96`이 `/api/v1/users/{id}/transactions`·`/business-info`를
  호출하나 BE에 해당 매핑이 없다. 500은 나지 않고 **개인화만 생략된 일반 답변**이 오므로 정상 동작으로 오판하기 쉽다
- **AI-6** — `ANTHROPIC_API_KEY` 없이도 기동되고 `/api/v1/health/`는 green인데 chat만 실패한다.
  준비 상태는 `/api/v1/health/ready`(키 없으면 503)로 확인한다
- **FE RELEASE URL** — `Constants.kt:9,14`가 죽은 `j14c203.p.ssafy.io`를 가리킨다 (DEBUG는 `10.0.2.2`로 정렬됨)
- **FE 테스트 부재** — `src/test`·`androidTest`에 템플릿 예제 2개뿐. 실질 커버리지 0
- **FE QR 결제 금액 고정** — `QrPaymentScreen.kt`가 첫 가맹점·1,000원으로 토큰을 만든다(금액 입력 UI 없음)

## 도메인 규칙

- 장부(BookEntry) 금액 필드는 **부가세 포함 원 결제금액**이 정본이다(공급가액 아님).
  `V3__fix_book_entry_amounts.sql`에서 의미가 확정됐다
- 뱅킹은 외부 금융망 없이 **DB 원장 100%** — `SsafyFinanceClient`/`SsafyCreditCardClient`는 in-repo 구현이다(HTTP 호출 없음)
- 인증 principal은 `Long userId` (`JwtAuthenticationFilter`)

## 문서 지도

| 문서 | 역할 |
|---|---|
| `WORK_LOG.md` | **작업 기록 정본.** 시간순 이력 + 기능 검증 구역 11개 |
| `docs/PRE_CICD.md` | 2026-10-18 CI/CD 직전 완성 지시서(0~9단계). 구현과 다른 점은 WORK_LOG에 기록 |
| `0722ready.md` | 2026-07-22 배포 준비 크로스레이어 진단 트래커. 대부분 해결, INT-1 잔여 |
| `wehave0702.md` | 2026-07-03 전반 진단 + 개선 계획 + 포트폴리오 전략. 미완 TODO 다수 |
| `monitoring.md` | Prometheus/Grafana/Alertmanager 구성 |
| `exec/포팅_매뉴얼.md` | 배포 매뉴얼 |

## Git

- 커밋/PR에 Claude co-author·생성 푸터를 넣지 않는다. contributor는 사용자 본인만
- `main` 직접 푸시 대신 브랜치 + PR
