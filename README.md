# 7iTAX - 1인 개발자 절세 SaaS

1인 개발자(사업자)를 위한 거래 기반 절세 특화 서비스입니다.
결제부터 장부 작성, 세목 분류, 세금 계산, 절세 전략까지 하나의 앱에서 처리합니다.

## 핵심 파이프라인

```
거래 발생 → 데이터 확보 → 자동 분류 → 기장 → 세금 계산 → 절세 전략
```

## 프로젝트 구조

```
7ITAX/
├── BE/          Spring Boot 3.x (Java 17) — REST API 서버
├── FE/          Android (Kotlin, Jetpack Compose) — 모바일 앱
├── ai/          FastAPI (Python 3.11) — AI 챗봇 / 세목 분류
├── db/          PostgreSQL 초기화 및 마이그레이션 SQL
├── monitoring/  Prometheus / Grafana / Alertmanager 설정
├── exec/        포팅 매뉴얼 및 산출물
└── docs/        API 문서, 진단 기록, 샘플 데이터
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

> 데모용 예시 키(실서비스에서는 반드시 교체):
> `AES_ENCRYPTION_KEY=dGVzdC1hZXMtZW5jcnlwdGlvbi1rZXktMzItYnl0ZXM=`,
> `JWT_SECRET=demo-jwt-secret-key-must-be-at-least-32-bytes!!`

> 금융 기능은 자체 구현 로컬 뱅킹으로 동작하므로 SSAFY 금융망 키가 필요 없습니다.
> 계좌/잔액/이체/카드결제는 DB 원장 위에서 처리되며, 신규 계좌는 데모용 시드 잔액을 갖습니다.
> AI 임베딩은 로컬 모델(`BAAI/bge-m3`)이라 외부 키가 불필요하며, LLM만 `ANTHROPIC_API_KEY`를 사용합니다.
> 새 엔티티/컬럼(잔액·원장 등)은 JPA `ddl-auto=update`로 자동 생성됩니다.
> SMS OTP 없이 로컬 시연하려면 백엔드에서 `TEST_LOGIN_ENABLED=true`를 사용하세요.

### 운영 배포

Jenkins Pipeline으로 자동 빌드/배포됩니다. 상세 내용은 [포팅 매뉴얼](exec/포팅_매뉴얼.md)을 참고하세요.

## API 엔드포인트

| 도메인 | 주요 엔드포인트 |
|--------|---------------|
| 인증 | `POST /api/auth/login`, `/verify-identity`, `/setup-pin` |
| 계좌 | `GET/POST /api/banking/accounts` |
| 카드 | `GET/POST /api/cards`, `POST /api/cards/{id}/activate`, `/payment` |
| 결제 | `POST /api/payments/authorize`, `/qr` |
| 장부 | `GET/POST /api/book-entries`, `PATCH /{id}/confirm` |
| 세목분류 | `POST /api/classification` |
| 세금 | `GET /api/tax-estimation`, `/api/tax-calendar/deadlines` |
| 내보내기 | `GET /api/export/book-entries`, `/vat`, `/income-tax` |
| 송금 | `POST /api/transfers/p2p`, `/withdraw` |

## 팀 (C203)

| 이름 | 역할 |
|------|------|
| 은태현 | 팀장 |

## 문서

- [포팅 매뉴얼](exec/포팅_매뉴얼.md)
- [Export API 문서](docs/export-api-docs.md)
- [모니터링 가이드](docs/monitoring.md)
- [작업 기록](WORK_LOG.md)
- [세법 신고 로직 정리](tax.md)
