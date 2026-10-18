# 7iTAX 성능 테스트 시나리오

> 로컬과 배포(성능측정) 환경에서 **같은 시나리오·같은 기준**으로 측정해 결과를 비교하기 위한 문서.
> 스크립트: `scripts/perf/tax7i.js`(k6), 실행: `scripts/perf/run.sh`, 서버 지표: `scripts/perf/sample_metrics.py`
> 결과는 이 문서 7절에 누적한다(측정할 때마다 행 추가, 기존 행은 수정하지 않는다).

---

## 1. 무엇을 왜 측정하나

개인사업자가 하루 동안 하는 일 — **장부·세금 화면 조회, 사업자 카드 결제(→ 장부 자동 생성), 가끔 취소·신고서 내보내기** — 을
여러 사람이 동시에 할 때 BE가 버티는지 본다.

| 질문 | 답하는 테스트 |
|---|---|
| 스크립트·환경이 정상인가 | smoke |
| 평상시 부하에서 응답 시간·오류율이 기준 안인가 | load |
| 어디까지 버티고, 무엇이 먼저 막히나 | stress |
| 갑자기 몰렸다 빠지면 오류가 나고 회복되나 | spike |
| 오래 돌리면 메모리·커넥션이 새거나 점점 느려지나 | soak |
| 한 계좌에 결제가 동시에 몰리면(행 잠금) 어떻게 되나 | contention |

측정 대상은 **BE 단독**이다. AI 챗봇은 외부 LLM(Anthropic API) 지연이 지배적이라 BE 성능과 섞지 않고,
`AI_MOCK_ENABLED=true`로 모의 AI를 쓴다(장부 자동 분류가 신뢰도 낮을 때 AI를 부르기 때문, `TaxClassificationService.java:71,86,130`).

---

## 2. 합격 기준

| 구분 | 대상 요청 | 기준 |
|---|---|---|
| 조회(`flow:read`) | 장부 목록·연간 요약·미확정 건수·세액 추정·계좌·카드 | **p95 < 500ms**, 오류율 < 1% |
| 결제(`flow:pay`) | 결제 승인·확정·취소 | **p95 < 1000ms**, 오류율 < 1% |
| 내보내기(`flow:export`) | 간편장부 PDF | **p95 < 3000ms** |
| 전체 | 모든 check | 성공률 > 99% |
| 장부 생성 | 결제 확정 후 2초 안에 장부 존재(표본) | `ledger_missing` 0 |

- 기준은 k6 `thresholds`로 들어 있어 위반하면 k6가 exit 99로 끝난다
- **load·soak는 기준 통과가 목표**, stress·spike는 기준이 깨지는 **지점과 양상을 기록**하는 것이 목표다
- 로그인(`flow:auth`)·장부 확인(`flow:verify`)은 준비·검증용이라 기준에서 뺀다

---

## 3. 사용자 행동 모델

VU(가상 사용자) 1명이 반복하는 한 번의 행동. 행동 사이에 0.5~1.5초 쉰다(사람의 생각 시간).

| 비율 | 행동 | 요청 |
|---|---|---|
| 65% | 화면 조회 | 6개 조회 API 중 하나 |
| 25% | 결제 | `POST /api/payments/authorize` → `POST /{id}/capture` (→ 비동기 장부 생성) |
| 5% | 결제 후 취소 | 결제 + `POST /{id}/cancel` (→ 환불, 장부 삭제) |
| 4% | 신고 자료 내보내기 | `GET /api/export/simple-ledger/pdf` |
| 1% | 장부 생성 확인 | 최근 결제의 장부가 생겼는지 |

- 테스트 사용자는 `setup()`에서 간편 로그인(`POST /api/auth/simple-login`)으로 **미리** 만든다
  (전화번호 `0107700` + 4자리, 기본 50명 — 사람마다 계좌 500만원·사업자 카드). 처음 로그인은 계좌·카드 발급까지 해서 느리므로 측정에서 뺀다
- VU가 사용자 수보다 많으면 여러 VU가 같은 사용자를 쓴다(같은 계좌 동시 결제가 일부 생긴다 — 현실과 비슷)

---

## 4. 테스트 종류(프로필)

| 프로필 | 부하 모양 | 시간 | 실행 |
|---|---|---|---|
| smoke | VU 2 고정 | 30초 | `./scripts/perf/run.sh smoke` |
| load | 0 → `VUS`(기본 50) 1분 → 유지 5분 → 0 | 6.5분 | `./scripts/perf/run.sh load` |
| stress | 50 → 100 → 200 → 300, 각 단계 2분 유지 | 14분 | `./scripts/perf/run.sh stress` |
| spike | 10 → 10초 만에 200 → 1분 → 10 → 1분 | 3분 | `./scripts/perf/run.sh spike` |
| soak | `VUS`(기본 30) 유지 `DURATION`(기본 30m) | 30분+ | `DURATION=2h ./scripts/perf/run.sh soak` |
| contention | load 모양, 전원 사용자 1명(계좌 1개)으로 결제만 | 6.5분 | `CONTENTION=1 ./scripts/perf/run.sh load` |

권장 순서: **smoke → load → stress → spike → contention → soak**. 앞 단계가 실패하면 뒤 단계 결과는 의미가 없다.

---

## 5. 환경별 실행

### 5-1. 로컬 (docker compose, 이 PC)

```bash
docker compose stop ai                                   # 측정 대상 아님 + 메모리 1.4GB 확보
AI_MOCK_ENABLED=true docker compose up -d backend        # 모의 AI로 재기동 (필요하면 DB_POOL_MAX=20 등)
python scripts/perf/sample_metrics.py http://localhost:8080 5 900 &   # 서버 지표 기록(선택)
./scripts/perf/run.sh smoke && ./scripts/perf/run.sh load
```

- k6는 Docker 이미지(`grafana/k6:0.54.0`)로 compose 네트워크 안에서 `backend:8080`을 직접 친다(설치 불필요)
- **한계**: 부하 발생기와 BE·DB가 같은 PC(논리 12코어·16GB)를 나눠 쓴다. 절대 처리량이 아니라
  **같은 PC에서의 전후 비교**(코드 개선 전후, `DB_POOL_MAX` 10 vs 20 등)에 쓴다
- 측정 후 원복: `docker compose up -d backend ai`(셸 변수 없이 → `.env` 값)

### 5-2. 배포 환경 (성능측정 환경)

**운영 실서비스에 돌리지 않는다.** 운영과 같은 이미지·인프라 사양으로 띄운 **성능측정 환경**에 돌린다
(PRE_CICD 부록: 성능측정 환경은 `TEST_LOGIN_ENABLED=true`, `APP_SEED_ENABLED=true`, 운영은 둘 다 false).

| 항목 | 설정 |
|---|---|
| 대상 BE | `TEST_LOGIN_ENABLED=true`, `AI_MOCK_ENABLED=true`, `SWAGGER_ENABLED`는 무관 |
| 부하 발생기 | **BE와 다른 머신**(같은 리전의 별도 EC2 등). 같은 머신이면 로컬과 같은 한계 |
| 경로 | 사용자와 같은 경로로: `https://<도메인>` (nginx·TLS 포함). 병목을 나누려면 BE 직접 주소로 한 번 더 |
| 실행 | `./scripts/perf/run.sh load https://<도메인>` (BASE_URL을 주면 compose 네트워크를 쓰지 않는다) |
| 서버 지표 | `python scripts/perf/sample_metrics.py https://<도메인> 5 900` 또는 Grafana(`monitoring.md`) |
| 데이터 | 테스트 사용자 전화번호가 `0107700xxxx`로 고정이라 반복 실행해도 사용자는 늘지 않고 결제·장부만 쌓인다. 회차마다 같은 조건을 원하면 DB 스냅샷 복원 |

추가로 배포 환경에서만 보는 것:
- nginx 경유 시 지연 증가분(BE 직접 대비), TLS 핸드셰이크(`http_req_tls_handshaking`)
- 인스턴스 사양 대비 CPU·메모리 사용률(CloudWatch), RDS 커넥션 수·CPU
- 인스턴스 2대 이상이면 `APP_SCHEDULING_ENABLED`는 1대만 true

---

## 6. 결과 읽는 법

| 신호 | 의미 | 볼 곳 |
|---|---|---|
| p95가 VU 증가에 비례해 오르는데 처리량(`http_reqs/s`)은 정체 | 포화 — 어딘가 대기열이 생김 | k6 요약 |
| `hikari_pending` > 0 이 지속 | DB 커넥션 부족(풀 크기 또는 느린 쿼리) | `sample_metrics.py` |
| `cpu_process`가 100%/코어수에 가깝게 | CPU 포화(PDF 생성, BCrypt 등) | `sample_metrics.py` |
| `heap_used_mb`가 soak 동안 계속 우상향 | 메모리 누수 의심 | `sample_metrics.py` |
| `async_queued` 증가 | 장부 생성 비동기 큐 적체 → `ledger_missing` | `sample_metrics.py`, k6 |
| 결제만 오류·지연(contention) | 계좌 행 잠금 대기 | k6 `flow:pay` |

---

## 7. 결과 기록

| 날짜 | 환경 | 프로필 | VU 최대 | 처리량(req/s) | read p95 | pay p95 | export p95 | 오류율 | 서버 최대값 | 판정 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-10-18 | 로컬(compose, 12코어·16GB, 모의 AI, 풀 10) | smoke | 2 | 5.7 | 47ms | 46ms | 1.13s(1건) | 0% | — | 통과 | |
| 2026-10-18 | 로컬(동일) | load | 50 | 32.7 | 280ms | 127ms | 1.84s | 0.62% | Hikari active 10/10·pending 52(CPU 0%로 30초씩), 힙 2.1GB | **불합격**(k6 기준은 통과) | p99 ≈ 30s·최대 60s, 카드 거래 기록 28건 유실, `ledger_missing` 3 → 결제 중 REQUIRES_NEW 풀 교착. 수정: 카드 거래 기록을 커밋 후 비동기로, 캐시 역직렬화, 실행기 확대 |

> **교훈**: p95만 보면 통과였다. **p99·최대값과 서버 지표(Hikari pending)를 같이 봐야** 교착을 잡는다. 판정은 k6 exit 코드만으로 하지 않는다.
| 2026-10-18 | 로컬(동일, 수정 후) | load | 50 | **57.2** | **14.5ms** | 82ms | 93ms | **0%** (0/22,494) | Hikari pending 최대 40(순간), 힙 0.95GB, CPU 14% | **통과** | p99 0.64s·최대 3.3s. 장부 생성 지연(DB: 장부 created_at − 결제 captured_at) 안정 구간 p95 9~106ms, 부하 상승 1분간 p95 2.2s. k6 `ledger_missing` 104는 확인 요청 정렬 문제로 인한 오탐(스크립트 수정) |

### 1차 → 2차 비교 (같은 시나리오·같은 PC, VU 50)

| 지표 | 1차(수정 전) | 2차(수정 후) |
|---|---|---|
| 처리량 | 32.7 req/s | 57.2 req/s (+75%) |
| 실패 | 81 / 12,875 (0.62%) | 0 / 22,494 |
| p99 / 최대 | 29.5s / 60s | 0.64s / 3.3s |
| read p95 | 280ms | 14.5ms |
| 카드 거래 기록 유실 | 28건 | 0건 |
| 장부 생성 지연 최대 | 60초 | 3.9초(부하 상승 구간) |
| 서버 힙 최대 | 2.1GB | 0.95GB |

> 처리량이 늘어난 것은 교착으로 멈춰 있던 30초 구간이 사라졌기 때문이다(VU의 생각 시간은 같다).
> 2차의 Hikari pending 40은 한 번의 표본에서만 보였다. 지속되지 않으면 문제가 아니지만, stress에서 다시 확인한다.
