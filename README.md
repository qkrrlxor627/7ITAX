# 7iTAX - 1인 개발자 절세 SaaS

1인 개발자(사업자)를 위한 거래 기반 절세 특화 서비스입니다.
결제부터 장부 작성, 세목 분류, 세금 계산, 절세 전략까지 하나의 앱에서 처리합니다.

## 핵심 파이프라인

```
거래 발생 → 데이터 확보 → 자동 분류 → 기장 → 세금 계산 → 절세 전략
```

## 프로젝트 구조

```
S14P21C203/
├── BE/         Spring Boot 3.x (Java 17) — REST API 서버
├── FE/         Android (Kotlin, Jetpack Compose) — 모바일 앱
├── ai/         FastAPI (Python 3.11) — AI 챗봇 / 세목 분류
├── db/         PostgreSQL 초기화 및 마이그레이션 SQL
├── exec/       포팅 매뉴얼 및 산출물
└── docs/       API 문서
```

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.x, Spring Security, JPA, Redis |
| Frontend | Kotlin, Jetpack Compose, Hilt, Retrofit, CameraX |
| AI | Python 3.11, FastAPI, LangChain, ChromaDB, Anthropic Claude API, 로컬 HF 임베딩(bge-m3) |
| Database | PostgreSQL 16, Redis 7 |
| Infra | Docker, Jenkins, Nginx |
| 금융 | 자체 구현 로컬 뱅킹(원장/잔액/이체) — 외부 금융망 미사용 |
| 외부 API | Anthropic Claude, Solapi SMS, Firebase(선택) |

## 주요 기능

### 금융 서비스
- PIN 기반 인증 (본인인증 + JWT)
- 계좌 개설/조회, 카드 등록/관리
- QR 결제 (MPM 가맹점 QR 스캔)
- P2P 송금, 출금

### 장부/세무 (핵심)
- 결제 시 간편장부 자동 생성
- MCC 코드 + AI 기반 세목 자동 분류 (3단계 신뢰도)
- 세목 결정은 사용자 본인이 수행 (법적 요건)
- 종합소득세/부가세 자동 계산 및 절세 시뮬레이션
- 세금 캘린더 (신고 마감일 알림)

### 내보내기
- 간편장부 PDF
- 부가세/종합소득세 Excel
- CSV 내보내기

### AI 챗봇
- RAG 기반 세법 지식 답변
- 거래 자동 분류 및 절세 추천

## 성능 개선 (Before / After)

k6 부하 테스트로 **동시 결제 시 DB 커넥션 풀이 30초씩 멈추는 교착**을 찾아 해결했습니다.
시나리오·합격 기준·측정 기록 전체는 [성능 테스트 문서](docs/PERF_TEST.md)에 있습니다.

### 무엇을 측정했나

- **도구**: k6(Docker 이미지) + 서버 지표 수집기(`/actuator/prometheus`의 Hikari·힙·CPU·비동기 큐)
- **부하 모양(load)**: 가상 사용자 0 → 50명(1분) → 50명 유지(5분) → 0, 행동 사이 0.5~1.5초 휴지
- **사용자 행동**: 장부·세금 화면 조회 65% / 사업자 카드 결제(→ 장부 자동 생성) 25% / 결제 후 취소 5% / 간편장부 PDF 4% / 장부 생성 확인 1%
- **테스트 사용자**: 간편 로그인으로 50명을 미리 만들어 각자 계좌·카드로 결제
- **환경**: 로컬 docker compose(12코어·16GB), BE 단독 측정을 위해 AI는 모의 클라이언트, DB 풀 10

### 발견한 문제

1차 측정은 p95 기준(조회 500ms·결제 1s)을 **통과**했지만, p99와 서버 지표를 함께 보니 실제로는 장애였습니다.

- 응답 중앙값은 7ms인데 **p99 ≈ 30초, 최대 60초 타임아웃**
- Hikari 커넥션 **10/10 사용 + 대기 52건이 CPU 0%인 채 30초씩 지속** → 일을 하느라 바쁜 게 아니라 서로 기다리는 상태
- 로그: `Connection is not available` 283건, **카드 거래 기록 28건 유실**

### 원인과 해결

| 원인 | 해결 |
|---|---|
| 결제 트랜잭션(계좌 행 잠금 보유) 안에서 카드 거래 기록을 `REQUIRES_NEW`로 저장 → **커넥션 1개를 쥔 채 2번째를 요구**. 동시 결제가 풀 크기만큼 몰리면 전원이 대기하다 Hikari 대기 한도(30초)에 풀림. 결제가 롤백돼도 기록은 커밋되는 정합성 문제도 있었음 | 카드 거래 기록을 이벤트로 바꿔 **결제 커밋 후 비동기 저장**(`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) |
| `@Async` 실행기가 스레드 2개·큐 50에 큐가 차면 요청 스레드가 직접 실행 → 같은 교착 재발 가능 | 스레드 4~8개·큐 500, 환경변수로 조정 |
| 세목 규칙 Redis 캐시가 엔티티의 계산용 getter까지 저장해 역직렬화가 매번 실패 → 매 결제마다 DB 재조회 + 경고 로그 수천 건 | 캐시 역직렬화에서 모르는 필드 무시 |

### 결과 (같은 시나리오·같은 PC)

| 지표 | Before | After |
|---|---|---|
| 처리량 | 32.7 req/s | **57.2 req/s** (+75%) |
| 실패 요청 | 81 / 12,875 (0.62%) | **0 / 22,494** |
| p99 / 최대 응답 시간 | 29.5s / 60s | **0.64s / 3.3s** |
| 조회 p95 | 280ms | **14.5ms** |
| 결제 p95 | 127ms | 82ms |
| 카드 거래 기록 유실 | 28건 | **0건** |
| 결제 → 장부 생성 지연(최대) | 60s | 3.9s (부하 상승 구간), 안정 구간 p95 9~106ms |
| 서버 힙 최대 | 2.1GB | 0.95GB |

> **배운 점**: p95만 보면 통과였습니다. 교착처럼 "가끔 전부 멈추는" 문제는 **p99·최대값과 서버 지표(커넥션 대기)를 함께** 봐야 드러납니다.
> 로컬 결과는 부하 발생기와 서버가 한 PC를 나눠 쓰므로 절대 성능이 아니라 **같은 조건에서의 전후 비교**로 읽어야 합니다.
> 배포(성능측정) 환경에서는 같은 스크립트를 `./scripts/perf/run.sh load https://<도메인>`으로 실행합니다.

## 빌드 및 실행

### 로컬 개발 (Docker Compose)

```bash
# 인프라 기동 (PostgreSQL, Redis, AI, Worker)
# AES_ENCRYPTION_KEY: Base64 인코딩된 32바이트, JWT_SECRET: 32바이트 이상 문자열
ANTHROPIC_API_KEY=<키> AES_ENCRYPTION_KEY=<키> JWT_SECRET=<키> docker compose up -d

# Backend
# 위 인라인 env는 docker compose 프로세스에만 적용되므로, bootRun에는 별도로 전달해야 한다.
# Redis는 compose가 비밀번호(ssafy)를 요구하므로 REDIS_PASSWORD도 함께 넘긴다.
# DB 접속 정보(tax7i/ssafy/ssafy)는 application.yaml 기본값이 compose와 일치해 생략 가능하다.
cd BE
AES_ENCRYPTION_KEY=<키> JWT_SECRET=<키> REDIS_PASSWORD=ssafy ./gradlew bootRun

# Android
Android Studio에서 FE/ 프로젝트 열기 → Run
```


> 금융 기능은 자체 구현 로컬 뱅킹으로 동작하므로 SSAFY 금융망 키가 필요 없습니다.
> 계좌/잔액/이체/카드결제는 DB 원장 위에서 처리되며, 신규 계좌는 데모용 시드 잔액을 갖습니다.
> AI 임베딩은 로컬 모델(`BAAI/bge-m3`)이라 외부 키가 불필요하며, LLM만 `ANTHROPIC_API_KEY`를 사용합니다.
> 새 엔티티/컬럼(잔액·원장 등)은 JPA `ddl-auto=update`로 자동 생성됩니다.
> SMS OTP 없이 로컬 시연하려면 백엔드에서 `TEST_LOGIN_ENABLED=true`를 사용하세요.
> 이때 `POST /api/auth/simple-login {"phoneNumber":"010..."}`로 전화번호만으로 로그인하며, 처음 보는 번호는 계좌·카드가 자동 생성됩니다.
> API 문서(Swagger UI): http://localhost:8080/swagger-ui/index.html

### 운영 배포

Jenkins Pipeline으로 자동 빌드/배포됩니다. 상세 내용은 [포팅 매뉴얼](exec/포팅_매뉴얼.md)을 참고하세요.

## API 엔드포인트

| 도메인 | 주요 엔드포인트 |
|--------|---------------|
| 인증 | `POST /api/auth/login`, `/verify-identity`, `/setup-pin` |
| 계좌 | `GET/POST /api/banking/accounts` |
| 카드 | `GET/POST /api/cards`, `POST /api/cards/{id}/deposit` |
| 결제 | `POST /api/payments/authorize`, `/qr` |
| 장부 | `GET/POST /api/book-entries`, `PATCH /{id}/confirm` |
| 세목분류 | `POST /api/classification` |
| 세금 | `GET /api/tax-estimation`, `/api/tax-calendar/deadlines` |
| 내보내기 | `GET /api/export/book-entries`, `/vat`, `/income-tax` |
| 송금 | `POST /api/transfers/p2p`, `/withdraw` |

## 팀 (C203)

| 이름 | 역할 |
|------|------|
| 은태현 | 팀장, 인프라 |
| 송주헌 | 인증, 결제 백엔드 |
| 박기택 | 계좌, 카드 세금 백엔드 및 총괄 기획 |
| 이지언 | 프론트 및 기획 인터뷰|
| 김영웅 | AI |
| 국민혁 | 프론트 및 플로우 차트 설계|

## 문서

- [성능 테스트 시나리오·측정 기록](docs/PERF_TEST.md)
- [포팅 매뉴얼](exec/포팅_매뉴얼.md)
- [Export API 문서](docs/export-api-docs.md)
- [기획서](planning.md)
