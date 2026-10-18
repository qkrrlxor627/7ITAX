# 7iTAX CI/CD 직전 완성 지시서

> 작성일 2026-10-18 · 대상 브랜치 `main` · 브랜치명 `chore/pre-cicd`
> 목표: 이 문서의 작업이 끝나면 **GitHub Actions 워크플로 파일만 추가해서 바로 CI/CD를 돌릴 수 있는 상태**가 된다.
> 관련 문서: `docs/IMPROVEMENT_PLAN.md` (이 문서는 그중 1번, 2번 일부, 11번 사전 작업, 13번 nginx 설정을 먼저 끝내는 묶음이다)

---

## 0. CLI 작업 규칙

1. `CLAUDE.md`와 `docs/IMPROVEMENT_PLAN.md`의 "0. CLI 작업 규칙"을 그대로 따른다.
2. 이 문서의 코드는 **작성 환경에서 빌드 검증을 하지 못했다**(의존성 저장소 접근 불가). 그래서 모든 단계 끝에 적힌 검증 명령을 실제로 실행하고, 실패하면 원인을 고친 뒤 무엇을 바꿨는지 `WORK_LOG.md`에 남긴다.
3. 각 단계의 "현재 상태 근거"를 먼저 열어 확인한다. 문서와 다르면 멈추고 차이를 보고한다.
4. 단계는 순서대로 진행한다. 커밋은 단계마다 하나씩 한다.
5. AI 로직(`ai/app/services/**`, 프롬프트, RAG, 분류 모델)은 수정하지 않는다. 이 문서에서 AI 쪽 변경은 헬스체크 엔드포인트 추가 하나뿐이다.
6. 안드로이드 앱(`FE/app/`)은 이번 범위에서 수정하지 않는다(기기 없음, CI/CD 대상 아님).

### 완료 정의 (Definition of Done)

아래 세 가지가 모두 통과하면 이 문서의 작업이 끝난 것이다.

```bash
cd BE && ./gradlew test                 # 기준선 308개 통과
cd .. && ./scripts/verify_stack.sh      # 컨테이너 빌드 → 기동 → healthy → 결제·장부 스모크 통과
docker compose config -q                # compose 문법 검증
```

---

## 단계 요약

| 단계 | 내용 | 바뀌는 파일 |
|---|---|---|
| 0 | 쓰지 않는 코드·파일 정리 | 아래 0단계 삭제 목록, `scripts/find_unused_java.py` |
| 1 | 설정을 환경변수로 외부화 + 죽은 설정 제거 | `BE/src/main/resources/application.yaml` |
| 2 | 스케줄러·시드 데이터를 끌 수 있게 | `SchedulingConfig.java`, `CardDataInitializer.java` |
| 3 | 운영용 BE 이미지 | `BE/Dockerfile`, `BE/.dockerignore` |
| 4 | compose에 backend 서비스 추가 | `docker-compose.yml`, `.env.example`, `monitoring/prometheus/prometheus.yml` |
| 5 | SSE가 프록시에서 끊기지 않게 | `FE/nginx.conf` |
| 6 | AI 준비 상태 확인 엔드포인트 | `ai/app/routers/health.py` |
| 7 | 한 번에 검증하는 스크립트 | `scripts/verify_stack.sh`, `scripts/smoke_payment_ledger.py`, `FE/docs/merchant-test.html` |
| 8 | CI 워크플로 (CI/CD의 첫 파일) | `.github/workflows/ci.yml` |
| 9 | 하네스 (CLI가 이 규칙을 지키게 만드는 장치) | `.claude/settings.json`, `.claude/hooks/*`, `.claude/agents/plan-verifier.md`, `CLAUDE.md` |

---

## 0단계. 쓰지 않는 코드·파일 정리

**원칙**
- **참조 0건이 확인된 것만 지운다.** 아래 목록은 작성 시점에 저장소 전체 검색으로 참조가 없음을 확인한 것이다. CLI는 지우기 직전에 같은 검색을 다시 돌려 0건인지 재확인한다.
- **판단이 필요한 것은 지우지 않는다.** "확인 필요" 목록은 사람에게 물어본 뒤 처리한다.
- `git rm`으로 지운다. 이력에 남으므로 필요하면 되살릴 수 있다.
- 묶음(A·B·C)마다 커밋 하나. 각 커밋 뒤에 검증 명령을 돌린다.
- 루트의 기록 문서(`WORK_LOG.md`, `0722ready.md`, `wehave0702.md`, `tax.md`, `monitoring.md`)는 `CLAUDE.md` 문서 지도에 연결돼 있으므로 지우지 않는다.

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 저장소에 서비스와 무관한 연습 프로젝트, 빌드되지 않는 Dockerfile, 호출되지 않는 클래스·메서드가 섞여 있다 |
| 문제 정의 | 정상: 저장소의 모든 파일이 빌드·실행·문서 중 하나에 쓰임 / 현재: 참조 0건 파일·코드가 존재(아래 목록) |
| 원인 분석 | SSAFY 서버 종료로 HTTP 클라이언트를 DB 원장으로 바꾸면서 남은 메서드, 팀 개발 중 올라간 연습 프로젝트, 기능 변경 후 남은 DTO |
| 가설 수립 | 참조 0건인 것만 지우면 빌드·테스트·동작에 영향 없이 읽을 코드가 줄어든다 |
| 실험 및 실행 | 묶음별로 `git rm` → 검증 명령 실행 |
| 결과 검증 | 각 묶음 뒤 `./gradlew compileJava compileTestJava test` 통과(308개 유지). 삭제한 파일 수·줄 수를 `git diff --stat`으로 WORK_LOG에 기록 |
| 재발 방지 | `scripts/find_unused_java.py`를 저장소에 두고 PR 전에 실행. 9단계 Stop 훅에 연결할지는 오탐 빈도를 보고 결정 |

### 묶음 A — 서비스와 무관한 디렉토리·파일 (빌드 영향 없음)

| 대상 | 근거 |
|---|---|
| `2026_oneday_journal/` | 독립 안드로이드 프로젝트(패키지 `com.example.a2026_oneday_journal`, 파일 47개). 저장소 내 참조 0건 |
| `jieonPractice/` | 독립 안드로이드 연습 프로젝트(`com.example.jieonpractice`, 39개). 참조 0건 |
| `MyApplication/` | 안드로이드 스튜디오 기본 템플릿(`com.example.myapplication`, 38개). 참조 0건 |
| `excel/` (루트) | 산출물 사본 3개(`5월_종합소득세_신고 (6).xlsx` 등, 파일명의 `(6)`은 다운로드 중복 표시). 코드가 쓰는 템플릿은 `BE/src/main/resources/templates/excel/`에 따로 있음(`SampleExportGenerator.java:115,131`) |
| `Dockerfile` (루트) | `BE/Dockerfile`과 같은 내용인데 루트에는 `gradlew`·`src`가 없어 빌드 불가. compose·매뉴얼은 모두 `BE/Dockerfile`을 참조(`docker-compose.yml:63-64`, `exec/포팅_매뉴얼.md:293`) |

```bash
# 지우기 직전 재확인 — 출력이 비어 있어야 한다 (이 지시서 자체와 WORK_LOG는 제외)
EXCL="--exclude-dir=.git --exclude=PRE_CICD.md --exclude=WORK_LOG.md"
for k in 2026_oneday_journal jieonPractice MyApplication; do
  command grep -rIl $EXCL --exclude-dir="$k" "$k" . ; done
command grep -rIn $EXCL --exclude-dir=excel "excel/" . | command grep -v "templates/excel"

git rm -r -q 2026_oneday_journal jieonPractice MyApplication excel Dockerfile
git commit -m "chore: 서비스와 무관한 연습 프로젝트·산출물 사본·빌드 불가 Dockerfile 제거"
```

`jieonPractice/`는 팀원 이름이 붙은 연습 프로젝트로 보인다. 지우기 전에 사람에게 한 번 확인받는다.

### 묶음 B — 참조 0건인 BE 클래스

| 대상 | 근거 |
|---|---|
| `card/dto/CardBalanceResponse.java` | main·test 참조 0건 |
| `card/dto/CardDepositRequest.java` | main·test 참조 0건 |
| `card/dto/CardDepositResponse.java` | main·test 참조 0건 |
| `card/dto/CreateCardFromAccountRequest.java` | main·test 참조 0건 |
| `payment/event/BookEntryCreationFailedEvent.java` | 발행·구독 0건. 주석상 "장부 생성 실패 보상용"인데 구현되지 않았다. 같은 목적은 IMPROVEMENT_PLAN 4번(Event Publication Registry)이 대체한다 |

```bash
(cd BE/src/main/java/com/ssafy/tax7i && git rm -q \
    card/dto/CardBalanceResponse.java card/dto/CardDepositRequest.java \
    card/dto/CardDepositResponse.java card/dto/CreateCardFromAccountRequest.java \
    payment/event/BookEntryCreationFailedEvent.java)
(cd BE && ./gradlew compileJava compileTestJava test -q)
git commit -m "chore(be): 참조되지 않는 DTO 4개와 미구현 보상 이벤트 제거"
```

### 묶음 C — 호출되지 않는 메서드 (HTTP 클라이언트 시절 잔재)

| 대상 | 근거 |
|---|---|
| `SsafyFinanceClient.searchMember(String)` (`:190`) | 호출 0건. 본문이 `getOrRegisterMember`와 같음(`localUserKey(userId)` 반환) |
| `SsafyFinanceClient.registerMember(String)` (`:194`) | 호출 0건. 위와 같음 |
| `SsafyCreditCardClient.getMyCards(String)` (`:120`) | 호출 0건. 카드 목록은 `CardRepository`로 직접 조회 |

메서드만 지우고, 지운 메서드에서만 쓰던 DTO(`SsafyCreditCardListResponse` 등)가 생기면 다시 `find_unused_java.py`를 돌려 연쇄 삭제한다.

```bash
(cd BE && ./gradlew compileJava compileTestJava test -q)
git commit -m "chore(be): 로컬 원장 전환 후 남은 미사용 클라이언트 메서드 제거"
```

### 확인 필요 — 사람에게 묻고 처리 (CLI가 임의로 지우지 않음)

| 대상 | 상태 | 물어볼 것 |
|---|---|---|
| `ConsentService.revokeConsent` | 호출 0건(컨트롤러 없음). `getConsents`는 테스트에서만 호출 | 동의 철회 API를 만들 계획이 있는가. 개인정보 동의 철회는 금융 서비스에서 필요한 기능이라 지우기보다 API를 붙이는 쪽일 수 있다 |
| `TaxParameterService.getVehicleNoInsuranceRatio` (`:167`) | 호출 0건 | 업무용 승용차 관련 세법 파라미터를 계산에 반영할 계획이 있는가 |
| `AiCategoryMapper.mapToName`·`mapToCode` | 테스트에서만 호출. 실제 코드는 `map()`만 사용(`AiClassificationService.java:24`) | 테스트 편의용으로 남길지, 테스트를 `map()` 기준으로 바꾸고 지울지 |
| `FE/src/` React 기본 템플릿 | 실제 기능 없음 | IMPROVEMENT_PLAN 13번(가맹점 POS)에서 교체 예정 → 지금 지우지 않고 13번에서 대체 |
| `FE/webview/` | 안드로이드 `Constants.WEBVIEW_BASE_URL`이 참조 | 2차 범위 WebView를 계속 쓸 것인가 |
| `Jenkinsfile` | GitHub Actions로 대체 예정 | CD 문서에서 대체 완료 후 삭제 |
| `banking/client/dto/Ssafy*` 클래스명 | 사용 중 | 삭제가 아닌 이름 변경 대상(`LocalLedger*` 등). 범위가 커서 별도 PR |

**프레임워크가 호출하는 메서드는 대상이 아니다.** 검색상 호출이 0건이어도 아래는 정상이다: `@EventListener`·`@TransactionalEventListener`·`@Scheduled`·`@Recover` 메서드, `@Aspect`의 어드바이스, JPA `AttributeConverter`(`AesEncryptor`), 템플릿 엔진이 쓰는 getter(`SimpleLedgerPdfExporter`의 `getDate` 등).

### 탐지 스크립트 — `scripts/find_unused_java.py` 신규

0단계 목록을 만든 검사를 그대로 스크립트로 남긴다. 결과는 "후보"다. 위 프레임워크 호출 규칙으로 걸러서 판단한다.

```python
"""BE에서 다른 코드가 참조하지 않는 클래스와 public 메서드 후보를 출력한다.

사용: python3 scripts/find_unused_java.py
결과는 후보 목록이다. 프레임워크가 호출하는 메서드(리스너, 스케줄러, AOP, JPA 컨버터,
템플릿 getter)는 정상이므로 사람이 걸러서 판단한다.
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "BE", "src")
MAIN = os.path.join(ROOT, "main", "java")
STEREOTYPE = re.compile(
    r"@(Component|Service|Repository|RestController|Controller|Configuration|Entity|"
    r"Embeddable|ControllerAdvice|RestControllerAdvice|SpringBootApplication|Aspect|MappedSuperclass)\b"
)
PUBLIC_METHOD = re.compile(r"^\s+public\s+(?!class|record|interface|enum)[\w<>\[\], ?]+\s+(\w+)\s*\(", re.M)
SKIP_METHODS = {"equals", "hashCode", "toString", "main", "run", "of", "from"}


def load_sources():
    sources = {}
    for directory, _, names in os.walk(ROOT):
        for name in names:
            if name.endswith(".java"):
                path = os.path.join(directory, name)
                with open(path, encoding="utf-8") as f:
                    sources[path] = f.read()
    return sources


def unused_classes(sources):
    for path, text in sources.items():
        if not path.startswith(MAIN) or STEREOTYPE.search(text):
            continue
        name = os.path.basename(path)[:-5]
        pattern = re.compile(rf"\b{re.escape(name)}\b")
        if not any(pattern.search(t) for p, t in sources.items() if p != path):
            yield os.path.relpath(path, MAIN)


def unused_methods(sources):
    main_sources = {p: t for p, t in sources.items() if p.startswith(MAIN)}
    for path, text in main_sources.items():
        if not re.search(r"@(Service|Component)\b", text):
            continue
        for method in sorted(set(PUBLIC_METHOD.findall(text)) - SKIP_METHODS):
            call = re.compile(rf"\.{method}\s*\(|::{method}\b")
            called_elsewhere = any(call.search(t) for p, t in main_sources.items() if p != path)
            called_inside = len(re.findall(rf"\b{method}\s*\(", text)) > 1
            if not called_elsewhere and not called_inside:
                yield os.path.relpath(path, MAIN), method


def main():
    sources = load_sources()
    classes = list(unused_classes(sources))
    methods = list(unused_methods(sources))
    for c in classes:
        print(f"[class]  {c}")
    for path, m in methods:
        print(f"[method] {path}#{m}")
    if not classes and not methods:
        print("후보 없음")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

**작성 시점 실행 결과** (위 스크립트를 이 문서에서 그대로 추출해 실행): 클래스 후보 5개(묶음 B 전부), 메서드 후보 23개. 그중 삭제 대상 3개(묶음 C), 확인 필요 5개(`ConsentService` 2개, `getVehicleNoInsuranceRatio`, `AiCategoryMapper.mapToName`·`mapToCode`), 나머지 15개는 프레임워크 호출(리스너·스케줄러·AOP·JPA 컨버터·템플릿 getter·`@Recover`).

---

## 1단계. 설정을 환경변수로 외부화 + 죽은 설정 제거

**현재 상태 근거**
- `application.yaml:39-41` — `app.test-login.enabled: false` 하드코딩. 매뉴얼(`exec/포팅_매뉴얼.md:95`)의 `TEST_LOGIN_ENABLED`가 바인딩되지 않음
- `application.yaml:44-51` — `ssafy.oauth` 블록, Java 소비처 0건
- `application.yaml:16-18` — `ddl-auto: update` 고정
- `server.shutdown` 설정 없음 → 배포 시 처리 중인 결제 요청이 강제 종료될 수 있음
- `ai.mock.enabled`는 코드에 있으나(`AiServiceClient.java:21`, `MockAiServiceClient.java:14`) yaml에 노출되지 않음

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 같은 이미지를 로컬·성능측정·운영에 쓰려면 환경마다 바꿀 값이 환경변수로 열려 있어야 하는데, 일부가 하드코딩돼 있다 |
| 문제 정의 | 정상: 이미지 1개 + 환경변수만 바꿔 환경 전환 / 현재: 테스트 로그인·DDL 전략·AI 목 여부를 바꾸려면 코드 수정 필요 |
| 원인 분석 | 초기 개발 때 로컬 전용 값을 yaml에 그대로 넣었고, SSAFY 서버 종료 후 설정을 정리하지 않았다 |
| 가설 수립 | 환경별로 달라지는 값을 모두 `${ENV:기본값}`으로 바꾸고 기본값은 지금 동작과 같게 두면, 기존 동작은 유지되고 환경 전환만 가능해진다 |
| 실험 및 실행 | 아래 파일로 교체 |
| 결과 검증 | 환경변수 없이 `./gradlew test` 308개 통과(기존 동작 유지). `TEST_LOGIN_ENABLED=true`로 기동 시 `/api/auth/test-login` 200, 없으면 403 |
| 재발 방지 | 9단계 하네스에서 yaml의 placeholder와 `.env.example` 키 목록을 대조하는 검사 실행 |

**`BE/src/main/resources/application.yaml` 전체 교체**

```yaml
spring:
  application:
    name: tax7i
  config:
    import: optional:classpath:application-local.yaml
  lifecycle:
    timeout-per-shutdown-phase: 30s
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:tax7i}
    username: ${DB_USERNAME:ssafy}
    password: ${DB_PASSWORD:ssafy}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_MAX:10}   # Hikari 기본값과 동일. 성능 측정 시 이 값만 바꿔 비교한다
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:update}       # Flyway 도입(IMPROVEMENT_PLAN 5번) 후 validate로 전환
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
        default_batch_fetch_size: 100
    open-in-view: false

server:
  shutdown: graceful                         # 종료 신호 후 처리 중 요청을 마치고 내려간다
  forward-headers-strategy: native           # ALB/nginx 뒤에서 X-Forwarded-* 반영

encryption:
  aes-key: ${AES_ENCRYPTION_KEY}

jwt:
  secret: ${JWT_SECRET}
  access-expiration: 1800000
  refresh-expiration: 604800000

app:
  test-login:
    enabled: ${TEST_LOGIN_ENABLED:false}     # 운영 false, 로컬·성능측정 true
  seed:
    enabled: ${APP_SEED_ENABLED:true}        # 기동 시 더미 유저·카드 생성(CardDataInitializer)
  scheduling:
    enabled: ${APP_SCHEDULING_ENABLED:true}  # 인스턴스 2대 이상이면 1대만 true

tax:
  office-code: "0305"

ai:
  base-url: ${AI_SERVICE_BASE_URL:http://localhost:8000}
  connect-timeout: 3
  read-timeout: 10
  mock:
    enabled: ${AI_MOCK_ENABLED:false}        # true면 AI 서버 없이 MockAiServiceClient 사용(BE 단독 성능 측정용)

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}

fcm:
  enabled: ${FCM_ENABLED:false}
  service-account-path: ${FCM_SERVICE_ACCOUNT_PATH:}

solapi:
  api-key: ${SOLAPI_API_KEY:}
  api-secret: ${SOLAPI_API_SECRET:}
  sender-phone: ${SOLAPI_SENDER_PHONE:}

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true
```

**확인할 것**
- `ssafy.oauth`를 지운 뒤 `command grep -rn "ssafy.oauth\|SSAFY_CLIENT" BE/src` 0건인지 확인.
- 테스트 프로필(`src/test/resources/application-test.yaml`)은 `app.test-login.enabled: true`를 직접 지정하므로 영향 없음.
- `exec/포팅_매뉴얼.md`의 환경변수 표에 새 변수(`APP_SEED_ENABLED`, `APP_SCHEDULING_ENABLED`, `AI_MOCK_ENABLED`, `JPA_DDL_AUTO`, `DB_POOL_MAX`)를 추가하고 `SSAFY_*` 3개를 삭제한다.

---

## 2단계. 스케줄러·시드 데이터를 끌 수 있게

**현재 상태 근거**
- `config/SchedulingConfig.java` — 조건 없는 `@EnableScheduling`. BE가 2개 떠 있으면 `TaxCalendarScheduler`(매일 09:00 FCM)가 2번 돈다
- `config/CardDataInitializer.java:24-26` — 조건 없는 `@Component` + `CommandLineRunner`. 유저가 없으면 더미 유저 2명(`01011111111`, `01022222222`)과 카드를 만든다. 운영 DB에도 생성됨

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 운영에서도 더미 유저가 생기고, 인스턴스를 늘리면 알림이 중복된다 |
| 문제 정의 | 정상: 환경변수로 시드·스케줄러를 켜고 끔 / 현재: 항상 켜짐 |
| 원인 분석 | 두 빈 모두 조건 애너테이션이 없다 |
| 가설 수립 | `@ConditionalOnProperty`를 붙이고 기본값을 true로 두면 로컬 동작은 그대로, 운영·2대 구성에서만 끌 수 있다 |
| 실험 및 실행 | 아래 변경 후 `APP_SEED_ENABLED=false`로 빈 DB 기동 |
| 결과 검증 | false일 때 `users` 테이블 0건, 로그에 `[CardDataInitializer]` 없음. 기본값일 때 기존처럼 2명 생성 |
| 재발 방지 | 중복 실행 근본 해결은 IMPROVEMENT_PLAN 7번(Spring Batch). 이 단계는 끌 수 있는 스위치까지만 |

```java
// BE/src/main/java/com/ssafy/tax7i/config/SchedulingConfig.java
package com.ssafy.tax7i.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
```

```java
// BE/src/main/java/com/ssafy/tax7i/config/CardDataInitializer.java — 클래스 선언부에 한 줄 추가
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
// (기존 애너테이션 유지)
public class CardDataInitializer implements CommandLineRunner {
```

**확인할 것**: 같은 방식의 다른 Initializer(`TaxBracketDataInitializer`, `TaxParameterDataInitializer`, `TaxDeadlineDataInitializer`, `BookEntryAmountBackfillInitializer`)는 세법 기준 데이터라 운영에서도 필요하다. 건드리지 않는다.

---

## 3단계. 운영용 BE 이미지

**현재 상태 근거** (`BE/Dockerfile`)
- root 사용자로 실행, 헬스체크 없음, JVM 메모리 옵션 없음, 타임존 미지정
- `.dockerignore` 없음 → 로컬 `build/`, `.gradle/`이 빌드 컨텍스트로 전송됨

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 컨테이너가 떠도 앱이 준비됐는지 알 수 없어, 배포 도구가 준비 전에 트래픽을 넘길 수 있다 |
| 문제 정의 | 정상: 이미지에 헬스체크가 있고, 컨테이너 메모리에 맞춰 힙이 잡히고, 비root로 실행 / 현재: 셋 다 없음 |
| 원인 분석 | 최소 동작용 Dockerfile에서 멈춰 있다 |
| 가설 수립 | readiness 헬스체크 + `MaxRAMPercentage` + 비root 사용자를 넣으면 무중단 배포(IMPROVEMENT_PLAN 11번)와 성능 측정의 전제가 갖춰진다 |
| 실험 및 실행 | 아래로 교체 후 `docker build -t tax7i-backend BE` |
| 결과 검증 | `docker inspect --format '{{.State.Health.Status}}'`가 `healthy`, `docker exec ... id -u`가 0이 아님 |
| 재발 방지 | 8단계 CI에서 이미지 빌드를 매 PR 실행 |

**`BE/Dockerfile` 전체 교체**

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=90s --retries=5 \
    CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```

**`BE/.dockerignore` 신규**

```
build/
.gradle/
out/
*.log
.idea/
```

**확인할 것**: `build/libs/`에 jar가 2개(plain jar 포함) 생기면 `COPY *.jar app.jar`가 실패한다. 실패하면 `build.gradle`에 `tasks.named('jar') { enabled = false }`를 추가한다.

---

## 4단계. compose에 backend 서비스 추가

**현재 상태 근거**
- `docker-compose.yml` — backend 서비스가 없고 `worker`가 같은 BE 이미지를 `SPRING_PROFILES_ACTIVE: worker`로 띄운다. `application-worker.yaml`은 없다(`command grep -rn worker BE/src/main/resources` 0건). 즉 worker는 이름만 다른 backend다
- worker가 `ai: condition: service_healthy`에 걸려 AI 최초 기동(bge-m3 다운로드, `start_period: 600s`)을 기다린다. BE는 AI 없이도 동작한다(분류는 규칙 기반 폴백)
- `monitoring/prometheus/prometheus.yml:21` — `host.docker.internal:8080`(bootRun 전제)
- `.env.example` 없음. 필요한 환경변수가 compose 주석과 매뉴얼에 흩어져 있다

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 로컬에서 "컨테이너로 띄운 BE"를 검증할 표준 경로가 없고, CD가 쓸 실행 정의도 없다 |
| 문제 정의 | 정상: `docker compose up backend` 한 줄로 BE가 DB·Redis와 함께 뜨고 healthy / 현재: bootRun 수동 실행 또는 AI를 기다리는 worker |
| 원인 분석 | BE를 컨테이너로 띄우는 정의가 worker라는 이름으로 남아 있고, AI 의존이 과하게 걸려 있다 |
| 가설 수립 | worker를 backend로 바꾸고 AI 의존을 빼면, CI와 성능 측정에서 BE만 빠르게 띄울 수 있다 |
| 실험 및 실행 | 아래로 교체 후 `cp .env.example .env` → 값 채우기 → `docker compose up -d postgres redis backend` |
| 결과 검증 | `docker compose ps`에서 backend `healthy`. Prometheus targets에서 `tax7i-backend` UP |
| 재발 방지 | 7단계 `verify_stack.sh`가 이 경로를 매번 사용 |

**`docker-compose.yml` 변경**: `worker` 서비스를 아래 `backend`로 교체한다. 나머지 서비스는 그대로 둔다.

```yaml
  backend:
    build:
      context: ./BE
      dockerfile: Dockerfile
    container_name: tax7i-backend
    ports:
      - "8080:8080"
    env_file:
      - path: .env
        required: false
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: tax7i
      DB_USERNAME: ssafy
      DB_PASSWORD: ssafy
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-ssafy}
      AI_SERVICE_BASE_URL: http://ai:8000
      # 기본값 없음: 비어 있으면 BE가 기동 시 실패한다(약한 기본값 배포 방지, 기존 worker와 동일)
      # `:?` 필수 표기를 쓰지 않는 이유: compose 파일 전체에 적용돼 postgres·redis만 띄울 때도 .env를 요구하게 된다
      AES_ENCRYPTION_KEY: ${AES_ENCRYPTION_KEY}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: on-failure
```

- `env_file`의 `required: false`는 Docker Compose 2.24 이상에서 동작한다. 버전이 낮으면 `env_file` 블록을 빼고 `.env`(compose가 자동으로 읽는 변수 치환용)만 쓴다.
- 로컬에서 bootRun과 backend 컨테이너를 동시에 띄우면 8080이 충돌한다. 둘 중 하나만 쓴다.

**`.env.example` 신규** (`.env`는 이미 `.gitignore:646`에 있음)

```dotenv
# 필수 시크릿 — 로컬용 생성 예: openssl rand -base64 32 / openssl rand -base64 48
AES_ENCRYPTION_KEY=
JWT_SECRET=

# 인프라
REDIS_PASSWORD=ssafy

# 환경별 스위치 — 값은 로컬·성능측정 기준. 주석은 값과 같은 줄에 두지 않는다(파서에 따라 값에 섞임)
# 운영: false
TEST_LOGIN_ENABLED=true
# 운영: false
APP_SEED_ENABLED=true
# 인스턴스 2대 이상이면 1대만 true
APP_SCHEDULING_ENABLED=true
# BE 단독 성능 측정(AI 서버 제외) 시 true
AI_MOCK_ENABLED=false
# Flyway 도입 후 validate
JPA_DDL_AUTO=update
DB_POOL_MAX=10

# AI 서버
ANTHROPIC_API_KEY=

# 선택
CORS_ALLOWED_ORIGINS=http://localhost:3000
FCM_ENABLED=false
```

**`monitoring/prometheus/prometheus.yml` 변경**: `tax7i-backend` 타깃을 compose 서비스명으로 바꾸고, bootRun용은 주석으로 남긴다.

```yaml
  - job_name: 'tax7i-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']          # compose backend 컨테이너
      # - targets: ['host.docker.internal:8080']  # 호스트에서 bootRun으로 띄울 때
        labels:
          application: 'tax7i'
```

기존 `tax7i-worker` 주석 블록은 삭제한다.

---

## 5단계. SSE가 프록시에서 끊기지 않게

**현재 상태 근거**
- `FE/nginx.conf` — `/api/` 프록시에 버퍼링·타임아웃 설정 없음. `proxy_pass http://tax-backend:8080`(Jenkins 컨테이너명 기준)
- SSE 엔드포인트: `/api/payments/qr/token/{token}/events`, `/api/payments/qr/merchant-token/{token}/events`
- 토큰 TTL 300초(`PaymentService.java:49`), SseEmitter 타임아웃도 같은 값

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | nginx 뒤에서 SSE를 열면 결제 완료 이벤트가 바로 오지 않거나 60초쯤에 연결이 끊길 수 있다 |
| 문제 정의 | 정상: 이벤트가 즉시 전달되고 연결이 토큰 TTL(300초)까지 유지 / 현재: nginx 기본값(응답 버퍼링 on, `proxy_read_timeout` 60초) 적용 |
| 원인 분석 | SSE 경로에 일반 API와 같은 프록시 설정이 적용된다 |
| 가설 수립 | SSE 경로만 버퍼링을 끄고 읽기 타임아웃을 TTL보다 길게 두면 해결된다 |
| 실험 및 실행 | 아래로 교체 후 nginx 경유로 SSE 연결, 다른 터미널에서 결제 → 이벤트 도착 시간 측정 |
| 결과 검증 | 결제 직후 1초 이내 이벤트 수신, 연결 유지 300초 |
| 재발 방지 | IMPROVEMENT_PLAN 13번 k6 SSE 시나리오에 nginx 경유 케이스 포함 |

**`FE/nginx.conf` 전체 교체**

```nginx
upstream tax7i_backend {
    server backend:8080;    # compose 서비스명. Jenkins 시절 컨테이너명 tax-backend에서 변경
}

server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # SSE (QR 결제 결과 알림) — 버퍼링 끄고 토큰 TTL(300초)보다 길게 유지
    location ~ ^/api/payments/qr/.+/events$ {
        proxy_pass http://tax7i_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 360s;
    }

    location /api/ {
        proxy_pass http://tax7i_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**확인할 것**: FE 컨테이너는 아직 compose에 없다(가맹점 POS는 IMPROVEMENT_PLAN 13번). 이 단계는 설정 파일만 맞춰 두고, `docker run --rm -v $PWD/FE/nginx.conf:/etc/nginx/conf.d/default.conf:ro nginx:alpine nginx -t`로 문법만 검증한다. upstream 호스트를 못 찾아 실패하면 `--add-host backend:127.0.0.1`을 붙여 문법 검증한다.

---

## 6단계. AI 준비 상태 확인 엔드포인트

**현재 상태 근거**
- `ai/app/routers/health.py` — 항상 `{"status": "ok"}`
- `CLAUDE.md` AI-6 — `ANTHROPIC_API_KEY`가 없어도 기동되고 health는 green인데 chat만 실패
- `ai/app/core/config.py:16` `anthropic_api_key: str = ""`, `:45` `settings = Settings()`

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 배포 후 헬스체크가 통과해도 챗봇은 실패할 수 있다 |
| 문제 정의 | 정상: 요청을 처리할 수 없는 상태면 준비 실패로 보고 / 현재: 프로세스만 살아 있으면 통과 |
| 원인 분석 | 살아 있음(liveness)과 준비됨(readiness)을 구분하지 않는다 |
| 가설 수립 | 기존 `/health/`는 liveness로 두고, 필수 설정을 확인하는 `/health/ready`를 추가하면 배포 도구가 구분할 수 있다 |
| 실험 및 실행 | 키 없이 기동 → `/api/v1/health/ready` 호출, 키 넣고 재호출 |
| 결과 검증 | 키 없음 503, 키 있음 200. 기존 `/api/v1/health/` 응답은 변화 없음 |
| 재발 방지 | CD에서 AI 배포 성공 조건을 `/ready` 200으로 지정 |

**`ai/app/routers/health.py` 전체 교체**

```python
from fastapi import APIRouter, Response, status

from app.core.config import settings

router = APIRouter(prefix="/api/v1/health", tags=["health"])


@router.get("/")
async def health_check() -> dict[str, str]:
    """프로세스 생존 확인(liveness). 컨테이너 재시작 판단에만 쓴다."""
    return {"status": "ok"}


@router.get("/ready")
async def readiness_check(response: Response) -> dict[str, str]:
    """요청 처리 가능 여부(readiness). 필수 설정이 없으면 503을 반환한다."""
    if not settings.anthropic_api_key:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return {"status": "not_ready", "reason": "ANTHROPIC_API_KEY is not set"}
    return {"status": "ready"}
```

**`ai/tests/test_health.py` 신규** — `app.main`을 임포트하면 lifespan과 서비스 의존성이 따라오므로, health 라우터만 붙인 앱으로 테스트한다.

```python
import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient

from app.core.config import settings
from app.routers import health


@pytest.fixture
def client():
    app = FastAPI()
    app.include_router(health.router)
    return AsyncClient(transport=ASGITransport(app=app), base_url="http://test")


async def test_키가_없으면_ready는_503(client, monkeypatch):
    monkeypatch.setattr(settings, "anthropic_api_key", "")
    res = await client.get("/api/v1/health/ready")
    assert res.status_code == 503


async def test_키가_있으면_ready는_200(client, monkeypatch):
    monkeypatch.setattr(settings, "anthropic_api_key", "sk-test")
    res = await client.get("/api/v1/health/ready")
    assert res.status_code == 200


async def test_liveness는_키와_무관하게_200(client, monkeypatch):
    monkeypatch.setattr(settings, "anthropic_api_key", "")
    res = await client.get("/api/v1/health/")
    assert res.status_code == 200
```

**주의**: `tests/conftest.py`가 `app.main`과 서비스들을 임포트하므로(1~12행), 이 테스트도 실행 시 `requirements.txt` 전체가 설치돼 있어야 한다. `requirements.txt`에는 pytest 계열이 없다(`pytest.ini`는 `asyncio_mode = auto` 사용) → 테스트 실행 환경에 `pytest pytest-asyncio httpx`를 추가 설치한다.

---

## 7단계. 한 번에 검증하는 스크립트

**현재 상태 근거**
- `scripts/smoke_payment_ledger.py` docstring이 `APP_TESTLOGIN_ENABLED`를 안내함(1단계 이후 `TEST_LOGIN_ENABLED`가 정답)
- `FE/docs/merchant-test.html:50` 기본 서버가 죽은 `https://j14c203.p.ssafy.io/api`
- "로컬에서 컨테이너로 띄워 핵심 흐름이 도는지" 확인하는 단일 명령이 없다

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 검증 절차가 사람 기억과 문서에 흩어져 있어, CI와 사람이 서로 다른 방식으로 확인한다 |
| 문제 정의 | 정상: 명령 하나가 빌드 → 기동 → healthy 대기 → 핵심 흐름 검증을 하고 성공/실패를 exit code로 반환 / 현재: 수동 |
| 원인 분석 | 검증 단계가 스크립트로 묶여 있지 않다 |
| 가설 수립 | `verify_stack.sh` 하나로 묶으면 로컬·CI·CLI 하네스가 같은 기준을 쓴다 |
| 실험 및 실행 | 아래 스크립트 작성 후 실행. 일부러 `JWT_SECRET`을 비워 실패 경로도 확인 |
| 결과 검증 | 정상 시 exit 0, 실패 시 exit 1 + 실패 원인과 backend 로그 마지막 200줄 출력 |
| 재발 방지 | 8단계 CI와 9단계 Stop 훅이 이 스크립트를 호출 |

**`scripts/verify_stack.sh` 신규** (`chmod +x` 필수)

```bash
#!/usr/bin/env bash
# 컨테이너 빌드 → 기동 → healthy 대기 → 결제·장부 스모크.
# 성공하면 조용히 한 줄, 실패하면 원인과 로그를 자세히 출력한다.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "FAIL: .env 없음 — cp .env.example .env 후 AES_ENCRYPTION_KEY, JWT_SECRET을 채울 것" >&2
  exit 1
fi

docker compose up -d --build postgres redis backend >/dev/null

status="starting"
for _ in $(seq 1 60); do
  status=$(docker inspect -f '{{.State.Health.Status}}' tax7i-backend 2>/dev/null || echo "missing")
  [[ "$status" == "healthy" ]] && break
  [[ "$status" == "missing" || "$status" == "exited" ]] && break
  sleep 5
done

if [[ "$status" != "healthy" ]]; then
  echo "FAIL: backend 상태=$status (healthy가 아님)" >&2
  docker compose logs --tail=200 backend >&2
  exit 1
fi

if ! out=$(python3 scripts/smoke_payment_ledger.py http://localhost:8080 2>&1); then
  echo "FAIL: 스모크 테스트 실패" >&2
  echo "$out" >&2
  docker compose logs --tail=100 backend >&2
  exit 1
fi

echo "OK: backend healthy + 결제→장부 스모크 통과"
```

**`scripts/smoke_payment_ledger.py` 수정**: docstring의 전제 부분을 아래로 교체한다(코드는 그대로).

```text
  - BE 기동 시 테스트 로그인 활성화: TEST_LOGIN_ENABLED=true
    (docker compose 사용 시 .env에 설정, bootRun 사용 시 환경변수로 전달)
```

**`FE/docs/merchant-test.html:50` 수정**: `value="https://j14c203.p.ssafy.io/api"` → `value="http://localhost:8080/api"`

---

## 8단계. CI 워크플로 (CI/CD의 첫 파일)

이 파일이 통과하면 "CI/CD 직전" 상태가 증명된다. CD(이미지 푸시·AWS 배포)는 다음 문서에서 이 워크플로 뒤에 붙인다.

**현재 상태 근거**
- `.github/workflows/` 없음. `Jenkinsfile`에 테스트 스테이지 없음, Mattermost(SSAFY) 알림
- BE 테스트 설정은 자체 시크릿을 가진다: `application-test.yaml`(AES·JWT 테스트 키, H2), `Tax7iApplicationTests`가 `encryption.aes-key`를 System property로 설정 → **CI에 시크릿 등록 불필요**
- `application-test.yaml`의 Redis가 `localhost:6379` → 혹시 모를 연결에 대비해 CI에 Redis 서비스를 붙인다

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | 테스트가 깨져도 머지·배포가 막히지 않는다 |
| 문제 정의 | 정상: PR마다 BE 테스트 + 이미지 빌드 + 스택 스모크 통과해야 머지 / 현재: 검사 없음 |
| 원인 분석 | CI가 빌드 전용 Jenkins였고 SSAFY 종료와 함께 멈췄다 |
| 가설 수립 | GitHub Actions에 3개 잡(test, image, stack)을 두면 로컬 `verify_stack.sh`와 같은 기준으로 막을 수 있다 |
| 실험 및 실행 | PR 생성 → 3개 잡 통과 확인. 테스트 하나를 일부러 깨뜨린 커밋으로 실패 확인 후 원복 |
| 결과 검증 | 정상 PR 통과, 깨뜨린 PR 실패. 잡별 소요 시간 `[측정: ___]` |
| 재발 방지 | 저장소 Settings → Branches에서 `main` 보호 규칙에 `be-test`, `stack-smoke`를 필수 체크로 지정 |

**`.github/workflows/ci.yml` 신규**

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  be-test:
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7
        ports: ["6379:6379"]
    defaults:
      run:
        working-directory: BE
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      - uses: gradle/actions/setup-gradle@v4
      - run: chmod +x gradlew && ./gradlew test --no-daemon
      - if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: be-test-report
          path: BE/build/reports/tests/test

  ai-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ai
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: "3.11"
          cache: pip
          cache-dependency-path: ai/requirements.txt
      - run: pip install -r requirements.txt pytest pytest-asyncio httpx
      # 6단계 헬스 테스트만 게이트로 건다. 기존 테스트 20개는 로컬에서 통과 여부를 먼저 확인한 뒤
      # `pytest -q -m "not integration"`으로 범위를 넓힌다(통과 확인 전에는 CI를 막지 않도록)
      - run: pytest -q tests/test_health.py

  stack-smoke:
    runs-on: ubuntu-latest
    needs: be-test
    steps:
      - uses: actions/checkout@v4
      - name: .env 생성 (CI 전용 임시 키)
        run: |
          cp .env.example .env
          sed -i "s|^AES_ENCRYPTION_KEY=.*|AES_ENCRYPTION_KEY=$(openssl rand -base64 32)|" .env
          sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|" .env
      - run: ./scripts/verify_stack.sh
      - if: always()
        run: docker compose down -v
```

**확인할 것**
- `ai-test`는 torch(CPU)·sentence-transformers 설치로 수 분이 걸린다. 소요 시간을 WORK_LOG에 기록하고, 너무 길면 pip 캐시 키를 점검한다. 모델 파일은 테스트에서 로드하지 않으므로 다운로드되지 않아야 한다 — 다운로드 로그가 보이면 원인을 보고한다.
- `Jenkinsfile`은 CD 문서에서 GitHub Actions로 대체가 확정되면 삭제한다. 이번 단계에서는 건드리지 않는다.

---

## 9단계. 하네스 — CLI가 이 규칙을 스스로 지키게 만드는 장치

이 문서와 `IMPROVEMENT_PLAN.md`는 CLI에게 주는 "말"이다. 하네스는 그 말을 어겼을 때 **실행 환경이 막거나 되돌려 보내는 장치**다. 규칙은 `CLAUDE.md`에 이미 있다(WORK_LOG 필수, `command grep`, AI 로직 불변). 지금은 지켜지는지 사람이 확인해야 한다. 이 단계는 그 확인을 훅과 검증 에이전트로 옮긴다.

| 단계 | 실제 행동 |
|---|---|
| 문제 인식 | CLI가 규칙을 잊으면(WORK_LOG 누락, AI 서비스 코드 수정, 테스트 안 돌리고 완료 보고) 사람이 리뷰에서 발견할 때까지 모른다 |
| 문제 정의 | 정상: 규칙 위반이 그 자리에서 차단되거나 완료 전에 되돌려짐 / 현재: 규칙이 문서에만 있음 |
| 원인 분석 | 규칙이 컨텍스트(`CLAUDE.md`)에만 있고, 실행을 막는 장치(훅·검사)가 없다 |
| 가설 수립 | ① 금지 경로·위험 명령은 PreToolUse 훅으로 차단 ② 완료 시점에 Stop 훅이 테스트·WORK_LOG를 검사 ③ 별도 검증 에이전트가 완료 조건을 대조하면, 사람 리뷰 전에 대부분 걸러진다 |
| 실험 및 실행 | 아래 파일 추가 후, CLI에게 일부러 `ai/app/services/chat_service.py` 수정을 요청 → 차단되는지 확인. 코드만 바꾸고 WORK_LOG 없이 끝내게 해 Stop 훅이 되돌려 보내는지 확인 |
| 결과 검증 | 두 경우 모두 차단·재작업. 정상 작업은 추가 메시지 없이 통과(성공은 조용히) |
| 재발 방지 | CLI가 새로운 방식으로 실수하면 그 실수를 막는 검사를 훅에 하나씩 추가한다(래칫). 더 이상 걸리지 않는 검사는 정리한다 |

**전제**: 훅 스크립트는 bash와 `jq`를 쓴다. Windows에서는 Git Bash 환경에서 동작하는지 먼저 확인한다. 특히 `protect-paths.sh`는 경로 접두어를 잘라 비교하므로, `file_path`가 `C:\...` 백슬래시 형식으로 들어오면 매칭되지 않고 통과해 버린다. 설치 직후 9단계 "실험 및 실행"의 차단 테스트를 반드시 한 번 돌려 실제로 막히는지 확인한다.

**작성 환경 검증 결과**: 위 bash 블록 6개는 `bash -n` 문법 검사 통과, `guard-bash.sh`·`protect-paths.sh`는 샘플 입력 11건(main 푸시, maintenance 브랜치 푸시, force push, 맨 grep, `command grep`, 볼륨 삭제, AI 서비스 파일, health.py, `.env`, `.env.example` 등)으로 차단/통과가 의도대로 나오는 것을 확인했다. YAML·JSON 블록은 파서로 문법만 확인했다. Gradle 빌드·Docker 빌드는 확인하지 못했다.

**`.claude/settings.json` 신규**

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/protect-paths.sh" }
        ]
      },
      {
        "matcher": "Bash",
        "hooks": [
          { "type": "command", "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/guard-bash.sh" }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/compile-check.sh", "timeout": 300 }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          { "type": "command", "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/check-done.sh", "timeout": 900 }
        ]
      }
    ]
  }
}
```

**`.claude/hooks/protect-paths.sh`** — AI 로직·시크릿·초기화 SQL 수정 차단

```bash
#!/usr/bin/env bash
input=$(cat)
file=$(jq -r '.tool_input.file_path // empty' <<<"$input")
[[ -z "$file" ]] && exit 0
rel="${file#"$CLAUDE_PROJECT_DIR"/}"

case "$rel" in
  ai/app/services/*|ai/app/models/*|ai/models/*)
    echo "차단: $rel 은 AI 로직이다. 이 작업 범위(PRE_CICD.md 0-5, IMPROVEMENT_PLAN.md 0-7)에서는 수정하지 않는다. 서빙 레이어(routers/health.py, main.py 계측)만 허용." >&2
    exit 2 ;;
  .env|.env.*)
    [[ "$rel" == ".env.example" ]] && exit 0
    echo "차단: $rel 은 시크릿 파일이다. 값은 사람이 채운다. 키 목록 변경은 .env.example에서 한다." >&2
    exit 2 ;;
  db/init/*)
    echo "차단: $rel 은 신규 볼륨 초기화 SQL이다. 스키마 변경은 Flyway 마이그레이션(IMPROVEMENT_PLAN 5번)으로 한다." >&2
    exit 2 ;;
esac
exit 0
```

**`.claude/hooks/guard-bash.sh`** — 되돌리기 어려운 명령 차단

```bash
#!/usr/bin/env bash
input=$(cat)
cmd=$(jq -r '.tool_input.command // empty' <<<"$input")

if [[ "$cmd" =~ git[[:space:]]+push.*(--force|-f([[:space:]]|$)) ]]; then
  echo "차단: force push 금지. 브랜치 + PR로 진행한다(CLAUDE.md Git 규칙)." >&2; exit 2
fi
if [[ "$cmd" =~ git[[:space:]]+push[[:space:]]+[^[:space:]]+[[:space:]]+(HEAD:)?main([[:space:]]|:|$) ]]; then
  echo "차단: main 직접 푸시 금지. 작업 브랜치로 푸시하고 PR을 연다." >&2; exit 2
fi
if [[ "$cmd" =~ docker[[:space:]]+compose.*down.*-v && "${CI:-}" != "true" ]]; then
  echo "차단: 로컬 볼륨(postgres-data 등) 삭제 명령. 정말 필요하면 사람에게 요청한다." >&2; exit 2
fi
if [[ "$cmd" =~ (^|[[:space:]])(grep|find)[[:space:]] && ! "$cmd" =~ command[[:space:]]+(grep|find) ]]; then
  echo "차단: 이 환경에서 맨 grep/find는 셸 함수에 가로채져 빈 결과를 낸다(CLAUDE.md 환경 함정). command grep / command find 를 쓴다." >&2; exit 2
fi
exit 0
```

**`.claude/hooks/compile-check.sh`** — BE 자바 파일을 고친 직후 컴파일만 빠르게 확인

```bash
#!/usr/bin/env bash
input=$(cat)
file=$(jq -r '.tool_input.file_path // empty' <<<"$input")
[[ "$file" == *"/BE/src/"*.java ]] || exit 0

cd "$CLAUDE_PROJECT_DIR/BE" || exit 0
if ! out=$(./gradlew compileJava compileTestJava -q 2>&1); then
  echo "컴파일 실패 — 다음 수정 전에 먼저 고칠 것:" >&2
  echo "$out" | tail -40 >&2
  exit 2
fi
exit 0   # 성공은 조용히
```

**`.claude/hooks/check-done.sh`** — 끝내기 전에 테스트와 WORK_LOG 확인

```bash
#!/usr/bin/env bash
input=$(cat)
# 이미 한 번 되돌려 보낸 뒤 다시 멈추는 경우 무한 반복을 막는다
[[ "$(jq -r '.stop_hook_active // false' <<<"$input")" == "true" ]] && exit 0

cd "$CLAUDE_PROJECT_DIR" || exit 0
changed=$(git status --porcelain | awk '{print $2}')
[[ -z "$changed" ]] && exit 0

code_changed=$(echo "$changed" | command grep -E '^(BE|ai|FE|scripts|docker-compose|\.github|monitoring)/?' || true)
[[ -z "$code_changed" ]] && exit 0

if ! echo "$changed" | command grep -q '^WORK_LOG.md$'; then
  echo "아직 끝내지 말 것: 코드가 바뀌었는데 WORK_LOG.md에 항목이 없다. CLAUDE.md 형식(배경/작업 내역/결과/변경된 파일 목록, 근거는 파일:줄·명령·수치)으로 맨 위에 추가한다." >&2
  exit 2
fi

if echo "$code_changed" | command grep -q '^BE/'; then
  if ! out=$( (cd BE && ./gradlew test -q) 2>&1 ); then
    echo "아직 끝내지 말 것: BE 테스트 실패. 기준선은 308개 통과다." >&2
    echo "$out" | tail -60 >&2
    exit 2
  fi
fi
exit 0
```

모든 훅 스크립트에 `chmod +x .claude/hooks/*.sh`.

**`.claude/agents/plan-verifier.md`** — 만든 쪽과 분리된 검증 에이전트

```markdown
---
name: plan-verifier
description: docs/PRE_CICD.md 또는 docs/IMPROVEMENT_PLAN.md의 한 단계가 끝났다고 보고되면 사용한다. 해당 단계의 완료 조건을 실제로 실행해 PASS/FAIL을 판정한다. 코드를 수정하지 않는다.
tools: Read, Grep, Glob, Bash
---

너는 구현자가 아니라 검증자다. 파일을 수정하지 않는다.

1. 요청받은 문서와 단계 번호를 읽고 "현재 상태 근거", "결과 검증", "완료 조건"을 뽑는다.
2. `git diff main...HEAD`로 실제 변경을 확인한다. 문서가 요구하지 않은 파일 변경이 있으면 목록으로 보고한다.
3. "결과 검증"에 적힌 명령을 실제로 실행한다. 실행할 수 없으면 이유를 적는다. 추측으로 통과시키지 않는다.
4. 7단계 표의 `[측정: ___]` 칸이 채워졌다면, 그 값이 어디서 나왔는지(명령·로그) WORK_LOG.md에서 확인한다. 근거 없는 수치는 FAIL.
5. 출력 형식:
   - 판정: PASS / FAIL
   - 실행한 명령과 결과 요약
   - FAIL 사유(파일:줄 근거 포함)
   - 문서 범위를 벗어난 변경
```

**`CLAUDE.md`에 추가할 섹션** ("현재 진행 상황" 바로 아래)

```markdown
## 작업 지시서

- 진행 중인 작업은 `docs/PRE_CICD.md`(먼저) → `docs/IMPROVEMENT_PLAN.md` 순서다
- 단계 하나를 끝내면 `plan-verifier` 에이전트로 검증받고 PASS일 때만 다음 단계로 간다
- 완료 정의: `cd BE && ./gradlew test` + `./scripts/verify_stack.sh` 통과
- `.claude/hooks/`가 규칙 위반을 차단한다. 차단 메시지가 나오면 우회하지 말고 메시지대로 고친다
```

**하네스를 키우는 방법 (래칫)**
- CLI가 같은 실수를 두 번 하면, 그 실수를 기계적으로 잡는 검사를 훅에 추가한다. 예: 측정값을 근거 없이 채운다 → Stop 훅에서 WORK_LOG의 수치 옆에 명령어가 있는지 검사.
- 훅이 6주 넘게 한 번도 차단하지 않았다면 남길 이유가 있는지 다시 본다.
- `CLAUDE.md`는 짧게 유지한다. 길어지면 세부는 문서로 빼고 링크만 남긴다.

---

## 부록. 이 문서가 끝난 뒤 CD 문서에서 할 일 (참고)

- 이미지 레지스트리(ECR) 푸시 잡을 `ci.yml` 뒤에 추가 — `main` 머지 시에만
- AWS 자격 증명은 GitHub OIDC + IAM Role(액세스 키를 시크릿에 두지 않음)
- EC2 배포 스크립트: IMPROVEMENT_PLAN 11번(포트 2개 번갈아 기동 → 헬스체크 → nginx 전환)
- 운영 `.env`는 SSM Parameter Store에서 받아 생성(`TEST_LOGIN_ENABLED`·`APP_SEED_ENABLED`는 성능측정 환경 true, 운영 false)
- 배포 성공 조건: BE `/actuator/health/readiness` 200, AI `/api/v1/health/ready` 200
- `Jenkinsfile` 삭제, `exec/포팅_매뉴얼.md` 갱신
