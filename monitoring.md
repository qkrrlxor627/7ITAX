# 모니터링 가이드 (tax7i)

Spring Boot 백엔드의 상태를 **수집 → 시각화 → 알림**하는 모니터링 스택 문서입니다.
금융권에서 쓰는 관측가능성(Observability) 방법론을 학습/포트폴리오 수준으로 구현했습니다.

---

## 1. 왜 모니터링인가

> 개발자는 대시보드를 24시간 쳐다보지 않는다. **문제가 생기면 시스템이 나를 부르게** 만든다.

모니터링은 "보는 것(pull)"이 아니라 **"알림받는 것(push)"** 으로 동작합니다.

```
[평상시]   Prometheus가 15초마다 메트릭 수집 + 규칙 검사     ← 사람은 안 봄
   ↓ (이상 조건 위반 시에만)
[알림]     Alertmanager → 웹훅(Slack/Discord/문자)로 호출    ← 여기서 인지
   ↓
[대시보드] Grafana로 "어디가" 문제인지 시각적으로 좁힘        ← 디버깅 도구
   ↓
[로그]     "왜" 문제인지 정확한 스택트레이스 확인             ← 근본 원인
```

대시보드는 **감시 도구가 아니라 디버깅 도구**다. 알림을 받고 나서 연다.

---

## 2. 관측가능성 3대 축

| 축 | 내용 | 본 프로젝트 |
|---|---|---|
| **Metrics** | 수치 지표 (TPS, 응답시간, 에러율, 자원) | ✅ Actuator + Prometheus + Grafana |
| **Logs** | 이벤트 기록 (에러, 거래내역) | 콘솔(stdout) — 알림 받은 뒤 확인 |
| **Traces** | 요청 흐름 추적 (MSA 호출 경로) | 미구현 (확장 시 OpenTelemetry) |

### 골든 시그널 (알림 기준의 핵심)
모니터링에서 "이것만 보면 치명적인 건 다 잡힌다"고 보는 4(+1)가지:

| 시그널 | 의미 | 알림 규칙 예 |
|---|---|---|
| **Latency** | 응답 지연 | p95 > 2초 |
| **Traffic** | 처리량 | 평소 대비 트래픽 끊김 |
| **Errors** | 에러율 | 5xx 비율 > 5% |
| **Saturation** | 자원 포화 | CPU > 80%, DB 커넥션 풀 고갈 |
| (+가용성) | 서비스 생존 | `up == 0` (DOWN) |

---

## 3. 아키텍처

```
┌─────────────────┐   /actuator/prometheus    ┌──────────────┐
│ Spring Boot 앱   │ ────(15초마다 스크랩)────▶ │  Prometheus  │
│ (localhost:8080) │   메트릭 숫자만 노출        │  (9090)      │
└─────────────────┘   ※ 기준 판단 안 함         │  규칙 검사    │
                                                └──────┬───────┘
                                          alert 발생   │   쿼리
                                          신호 전달     │   ▼
                                                 ▼   ┌──────────────┐
                                         ┌──────────┐│   Grafana     │
                                         │Alertmanager││  (3001)       │
                                         │ (9093)    ││  대시보드      │
                                         │그룹/억제/  │└──────────────┘
                                         │라우팅      │
                                         └────┬──────┘
                                       웹훅    ▼
                                         ┌──────────────┐
                                         │ webhook-logger│  ← 로컬 테스트 수신기
                                         │ (알림 JSON 로그)│     (실제론 Slack/Discord)
                                         └──────────────┘
```

**핵심 원칙 — 관심사 분리:**
- **자바 앱**: 자기 상태를 숫자로 "보고"만 함. 알림 기준을 코드에 넣지 않는다.
- **Prometheus**: 그 숫자가 위험한지 "판단"(규칙). 기준은 `.yml` 파일에 있어 앱 재배포 없이 변경 가능.
- **Alertmanager**: 알림을 "어떻게/누구에게" 보낼지 처리.

---

## 4. 구성 요소

| 컴포넌트 | 포트 | 역할 |
|---|---|---|
| Spring Boot Actuator | 8080 | `/actuator/prometheus`로 메트릭 노출 (Micrometer) |
| Prometheus | 9090 | 메트릭 수집·저장, 알림 규칙 검사 |
| Grafana | 3001 | 대시보드 시각화 (3000은 프론트와 충돌하여 3001 사용) |
| Alertmanager | 9093 | 알림 그룹화·억제·라우팅·발송 |
| webhook-logger | 8025 | 로컬 테스트용 알림 수신기 (요청을 stdout에 로깅) |

### 적용한 코드/설정 변경
| 파일 | 내용 |
|---|---|
| `BE/build.gradle` | `spring-boot-starter-actuator`, `micrometer-registry-prometheus` 추가 |
| `BE/src/main/resources/application.yaml` | `management.*` (health·prometheus·metrics 노출, 앱 태그) |
| `BE/.../config/SecurityConfig.java` | `/actuator/health`, `/actuator/prometheus`, `/actuator/info` permitAll |
| `monitoring/prometheus/prometheus.yml` | 스크랩 타깃 + 규칙 파일 + Alertmanager 연결 |
| `monitoring/prometheus/alert.rules.yml` | 골든 시그널 알림 규칙 |
| `monitoring/alertmanager/alertmanager.yml` | 라우팅·그룹화·억제·웹훅 수신자 |
| `monitoring/grafana/provisioning/**` | 데이터소스 + 대시보드 자동 등록 |
| `docker-compose.yml` | prometheus·grafana·alertmanager·webhook-logger 서비스 |

---

## 5. 실행 방법

```powershell
# 1) 모니터링 스택 기동 (DB·Redis 포함)
docker compose up -d postgres redis prometheus grafana alertmanager webhook-logger

# 2) 백엔드 실행 (로컬 8080)
cd BE; ./gradlew.bat bootRun
```

### 접속
| 화면 | URL |
|---|---|
| 백엔드 메트릭 | http://localhost:8080/actuator/prometheus |
| 백엔드 헬스 | http://localhost:8080/actuator/health |
| Prometheus 타깃 | http://localhost:9090/targets |
| Prometheus 알림 | http://localhost:9090/alerts |
| Grafana | http://localhost:3001 (admin / admin) |
| Alertmanager | http://localhost:9093 |

정리:
```powershell
docker compose stop postgres redis prometheus grafana alertmanager webhook-logger
```

---

## 6. 수동 확인 — 정상 vs 이상 판단 기준

### ① 백엔드 메트릭 `:8080/actuator/prometheus`
- ✅ 정상: `# HELP`, `jvm_memory_used_bytes{...} 2.3E7` 등 수백 줄 출력
- ❌ 빈 화면/연결 거부 → 백엔드 안 뜸
- ❌ 404 → `exposure.include`에 prometheus 누락 또는 actuator 의존성 빠짐
- ❌ 401/403 → SecurityConfig permitAll 누락

### ② 헬스 `:8080/actuator/health`
- ✅ 정상: `{"status":"UP"}`
- ❌ `DOWN` → 하위 컴포넌트 문제(보통 `db`/`redis`)

### ③ Prometheus 타깃 `:9090/targets`  ← 가장 중요한 진단 화면
| 항목 | ✅ 정상 | ❌ 이상 & 의미 |
|---|---|---|
| State | 초록 `UP` | 빨강 `DOWN` |
| Error | 비어 있음 | `connection refused`=앱 죽음 / `deadline exceeded`=타임아웃 / `404`=경로 오타 |
| Last Scrape | 방금 전 | 안 갱신 → 스크랩 중단 |

### ④ Prometheus 쿼리 `:9090/graph`
- `up{job="tax7i-backend"}` → ✅ `1` / ❌ `0`(죽음) / Empty(job명 오타)
- **"Empty result"는 에러가 아니라 "아직 데이터 없음"** — 빨간 `parse error`와 구분할 것

### ⑤ Grafana `:3001` 대시보드
- ✅ 패널에 그래프가 그려짐
- ⚠️ `No data` → 시간 범위(우상단 Last 30m)·HTTP 요청 유무 확인. JVM/CPU 패널은 즉시 떠야 정상
- ❌ `Datasource not found` → 데이터소스 연결(Save & test) 확인

### 판단 흐름
```
①8080 숫자 나옴?  ─아니오→ 백엔드/actuator 문제
   │예
③9090 타깃 UP?    ─아니오→ Error 메시지로 원인 파악
   │예
⑤Grafana 그래프?  ─아니오→ 데이터소스 연결 확인
   │예
 → 전 구간 정상
```

**핵심 구분:** `빈화면/No data/Empty = 아직 데이터 없음(정상일 수 있음)` vs `빨간 에러/DOWN/연결거부 = 진짜 고장`

---

## 7. 알림 (Alerting)

### 알림 규칙 (`monitoring/prometheus/alert.rules.yml`)
| 알림 이름 | 조건 | 심각도 |
|---|---|---|
| `BackendDown` | `up == 0` 1분 지속 | critical |
| `HighErrorRate` | 5xx 비율 > 5% 1분 지속 | critical |
| `HighLatencyP95` | p95 응답시간 > 2초 2분 지속 | warning |
| `HighCpuUsage` | process CPU > 80% 2분 지속 | warning |
| `DbConnectionPoolPending` | HikariCP 대기 커넥션 > 0, 1분 지속 | warning |

규칙 상태 확인: http://localhost:9090/alerts (Inactive→Pending→Firing 단계)

### Alertmanager가 단순 알림 이상으로 해주는 것
| 기능 | 설명 | 효과 |
|---|---|---|
| **그룹화** | 비슷한 알림을 하나로 묶음 | 알림 폭탄 방지 |
| **억제(Inhibition)** | `BackendDown`(critical) 발생 시 그로 인한 하위 경고 숨김 | 진짜 원인만 노출 |
| **침묵(Silence)** | 점검 중 특정 알림 일시 정지 | 계획 작업 중 스팸 방지 |
| **라우팅** | 심각도별 다른 채널로 분기 (critical→전화, warning→Slack) | 에스컬레이션 |
| **반복** | 미응답 시 주기적 재알림 | 무시 방지 |

본 프로젝트는 `alertmanager.yml`에 그룹화 + `BackendDown → 하위 경고 억제` 규칙을 구성했고,
수신자는 로컬 `webhook-logger`로 두었습니다. 운영 시 `webhook_configs.url`만 실제 Slack/Discord URL로 교체하면 됩니다.

### 알림 동작 검증 (로컬)
```powershell
# 1) 백엔드를 의도적으로 중지 → 1분 뒤 BackendDown 발화
#    (bootRun 터미널 Ctrl+C 또는 8080 프로세스 종료)

# 2) Prometheus 알림 상태가 Firing 되는지 확인
#    http://localhost:9090/alerts

# 3) webhook-logger 로그에 알림 JSON이 찍히는지 확인
docker compose logs --tail=50 webhook-logger

# 4) 백엔드 재기동 → 잠시 후 'resolved' 알림 수신
```

---

## 8. 운영(금융권) 관점 보강 포인트

지금은 **로컬/포트폴리오용**입니다. 실제 운영이라면:
- Actuator를 `management.server.port`로 **별도 포트 분리** + 내부망/인증 제한 (현재 permitAll)
- 알림 수신자를 실제 채널 + **심각도별 에스컬레이션** 라우팅
- **SLO/에러버짓** 도입 → 사용자 체감에 영향 주는 것만 알림 (알림 피로 방지)
- 로그 **중앙화(ELK/Loki)** + 개인정보(주민번호·계좌) **마스킹** (세금/금융 데이터)
- 로그 **보존·무결성**(전자금융감독규정) + 감사 추적(Audit Trail)
- **분산 추적**(OpenTelemetry)으로 AI 서비스·외부 연동 구간 trace
