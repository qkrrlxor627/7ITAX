# CLAUDE.md

7iTAX — 개인사업자 세무 자동화. BE(Spring Boot) · FE(Android Compose) · ai(FastAPI) · PostgreSQL/Redis/ChromaDB.

## 현재 진행 상황 (2026-08-04)

문서 정리 완료, **기능 검증 착수 직전**. 다음 작업은 검증 구역 `[0]` (postgres + redis 기동)부터.

- 브랜치 `docs/work-log-verification-zones`, PR [#3](https://github.com/qkrrlxor627/7ITAX/pull/3) open
- 검증 구역 11개의 정의·의존 순서·구역별 지뢰는 `WORK_LOG.md`의 "기능 검증 구역 분할" 항목에 있다
- 세션 재개 절차와 미착수 항목은 `WORK_LOG.md` 최상단 "세션 마무리 — 재개 지점 기록" 항목 참조

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
# 인프라 (compose에 backend 서비스는 없다 — BE는 bootRun 전용)
docker compose up -d postgres redis

# BE — JDK 17 필수(시스템 기본 JDK로는 Gradle 구동 불가)
cd BE
AES_ENCRYPTION_KEY=<키> JWT_SECRET=<32바이트+> REDIS_PASSWORD=ssafy ./gradlew bootRun
./gradlew test          # 308개 통과가 기준선

# AI — 최초 기동 시 bge-m3 약 2GB 다운로드 + Chroma 자동 인덱싱으로 매우 느림
cd ai && uvicorn app.main:app
```

- `SPRING_PROFILES_ACTIVE=local`은 **쓸 수 없다** — `application-local.yaml`이 `BE/.gitignore:44`로 제외돼 클론본에 없다
- redis는 compose가 `--requirepass ssafy`로 띄우므로 `REDIS_PASSWORD` 전달 필요
- AES/JWT 시크릿은 의도적으로 기본값 없음(약한 기본값 배포 방지)

## 알려진 미해결 항목

- **INT-1 (개인화)** — `ai/app/services/backend_client.py:67,96`이 `/api/v1/users/{id}/transactions`·`/business-info`를
  호출하나 BE에 해당 매핑이 없다. 500은 나지 않고 **개인화만 생략된 일반 답변**이 오므로 정상 동작으로 오판하기 쉽다
- **AI-6** — `ANTHROPIC_API_KEY` 없이도 기동되고 health는 green인데 chat만 실패한다
- **FE RELEASE URL** — `Constants.kt:9,14`가 죽은 `j14c203.p.ssafy.io`를 가리킨다 (DEBUG는 `10.0.2.2`로 정렬됨)
- **`ssafy.oauth` dead config** — `application.yaml:44-51`, Java 소비처 0건
- **FE 테스트 부재** — `src/test`·`androidTest`에 템플릿 예제 2개뿐. 실질 커버리지 0

## 도메인 규칙

- 장부(BookEntry) 금액 필드는 **부가세 포함 원 결제금액**이 정본이다(공급가액 아님).
  `V3__fix_book_entry_amounts.sql`에서 의미가 확정됐다
- 뱅킹은 외부 금융망 없이 **DB 원장 100%** — `SsafyFinanceClient`/`SsafyCreditCardClient`는 in-repo 구현이다(HTTP 호출 없음)
- 인증 principal은 `Long userId` (`JwtAuthenticationFilter`)

## 문서 지도

| 문서 | 역할 |
|---|---|
| `WORK_LOG.md` | **작업 기록 정본.** 시간순 이력 + 기능 검증 구역 11개 |
| `0722ready.md` | 2026-07-22 배포 준비 크로스레이어 진단 트래커. 대부분 해결, INT-1 잔여 |
| `wehave0702.md` | 2026-07-03 전반 진단 + 개선 계획 + 포트폴리오 전략. 미완 TODO 다수 |
| `monitoring.md` | Prometheus/Grafana/Alertmanager 구성 |
| `exec/포팅_매뉴얼.md` | 배포 매뉴얼 |

## Git

- 커밋/PR에 Claude co-author·생성 푸터를 넣지 않는다. contributor는 사용자 본인만
- `main` 직접 푸시 대신 브랜치 + PR
