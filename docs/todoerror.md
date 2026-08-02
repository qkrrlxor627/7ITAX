# 7iTAX 프로젝트 로컬 구동 불가 원인 정리

## 프로젝트 구성

| 컴포넌트 | 기술 스택 | 포트 |
|----------|----------|------|
| **BE** (백엔드) | Spring Boot 3.5 / Java 17 | 8080 |
| **FE** (프론트엔드) | Android (Kotlin + Jetpack Compose) | - |
| **ai** (AI 서비스) | FastAPI / Python 3.11 + Claude API | 8000 |
| **DB** | PostgreSQL 16 | 5432 |
| **Cache** | Redis 7 | 6379 |

---

## 구동 불가 원인 전체 목록

### 1. Android 앱 → PC에서 직접 실행 불가 (심각도: 높음)
- FE가 **Android 네이티브 앱** (Kotlin + Jetpack Compose)
- Android Studio + AVD 에뮬레이터 또는 실제 Android 디바이스 필요
- compileSdk 36, minSdk 28 → Android Studio 최신 버전 + SDK 설치 필요
- **해결**: Android Studio 설치하면 가능

### 2. API URL이 SSAFY 원격 서버로 하드코딩 (심각도: 높음)
- `FE/app/src/main/java/com/ssafy/seveniTax/util/Constants.kt`에서 API_BASE_URL이 `https://j14c203.p.ssafy.io/api/`로 고정
- DEBUG/RELEASE 모두 동일한 원격 주소 → localhost 백엔드를 바라보지 않음
- SSAFY 프로젝트 서버는 **교육 과정 종료 후 서버 회수**되어 접근 불가
- **해결**: URL을 `http://10.0.2.2:8080/api/` (에뮬레이터 localhost)로 수정

### 3. SSAFY OAuth 서버 접근 불가 (심각도: 높음)
- 인증이 **SSAFY OAuth 2.0** (`project.ssafy.com`) 기반
- SSAFY 과정 외부에서는 OAuth 서버 접근 불가
- 이 인증 없이는 사용자 로그인/가입 자체가 불가능
- 환경변수: `SSAFY_CLIENT_ID`, `SSAFY_CLIENT_SECRET`, `SSAFY_REDIRECT_URI`
- **해결**: 자체 인증 로직으로 대체 필요

### 4. Anthropic API Key 없음 (심각도: 높음)
- AI 서비스가 **Claude API** (claude-opus-4-7, claude-haiku-4-5-20251001) 사용
- `ANTHROPIC_API_KEY` 환경변수 필수 → 유료 API 키 없으면 AI 기능 전체 불가
- 임베딩 모델 `BAAI/bge-m3` (~2GB) + PyTorch (~2GB) 다운로드도 필요
- **해결**: Anthropic API 키 발급 필요 (유료), 또는 AI 기능 포기하고 BE만 운영

### 5. Solapi SMS API Key 없음 (심각도: 중간)
- 본인인증(OTP)에 **Solapi SMS** 서비스 사용
- 환경변수: `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, `SOLAPI_SENDER_PHONE`
- 키 없으면 회원가입/본인인증 불가
- **해결**: `application.yaml`에서 `app.test-login.enabled=true`로 설정하여 우회

### 6. AES/JWT 시크릿 미설정 (심각도: 중간)
- `AES_ENCRYPTION_KEY` → 민감 데이터 암호화용, 없으면 백엔드 구동 실패
- `JWT_SECRET` → JWT 토큰 발급용, 없으면 인증 체계 불가
- **해결**: 임의의 32바이트 이상 문자열 생성하여 환경변수 설정

### 7. Docker 미설치 - PostgreSQL/Redis (심각도: 중간)
- docker-compose.yml로 PostgreSQL 16, Redis 7 컨테이너 실행 필요
- Docker Desktop이 Windows에 설치되어 있어야 함
- DB 초기화 SQL (`db/init/01_schema.sql`, `02_seed_data.sql`)은 프로젝트에 포함됨
- **해결**: Docker Desktop 설치 후 `docker-compose up postgres redis`

### 8. 임베딩 모델 다운로드 필요 (심각도: 낮음)
- HuggingFace `BAAI/bge-m3` 모델 ~2GB
- PyTorch CPU 버전 ~2GB
- 총 ~4GB 디스크 + 다운로드 시간 필요
- **해결**: 시간/용량만 있으면 자동 다운로드 가능

---

## 필요한 환경변수 전체 목록

```env
# === 백엔드 (BE) ===
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tax7i
DB_USERNAME=ssafy
DB_PASSWORD=ssafy
REDIS_HOST=localhost
REDIS_PORT=6379
AES_ENCRYPTION_KEY=<32바이트 Base64 인코딩 키>
JWT_SECRET=<32바이트 이상 시크릿>
AI_SERVICE_BASE_URL=http://localhost:8000
CORS_ALLOWED_ORIGINS=http://localhost:3000

# SSAFY OAuth (현재 접근 불가)
SSAFY_CLIENT_ID=<SSAFY OAuth ID>
SSAFY_CLIENT_SECRET=<SSAFY OAuth Secret>
SSAFY_REDIRECT_URI=<Redirect URL>

# SMS (없으면 test-login 모드 사용)
SOLAPI_API_KEY=<Solapi API Key>
SOLAPI_API_SECRET=<Solapi API Secret>
SOLAPI_SENDER_PHONE=<발신번호>

# Firebase (선택사항, 비활성화 가능)
FCM_ENABLED=false

# === AI 서비스 ===
ANTHROPIC_API_KEY=<Anthropic API Key>
CLAUDE_MODEL_OPUS=claude-opus-4-7
CLAUDE_MODEL_HAIKU=claude-haiku-4-5-20251001
EMBEDDING_MODEL=BAAI/bge-m3
EMBEDDING_DEVICE=cpu
RAG_ENABLED=true
CHROMA_PERSIST_DIRECTORY=./data/chroma
BACKEND_BASE_URL=http://localhost:8080
CACHE_ENABLED=true
CACHE_THRESHOLD=0.95
CACHE_MAX_ENTRIES=10000
CACHE_TTL_HOURS=24
DEBUG=false
ALLOWED_ORIGINS=["http://localhost:3000"]
```

---

## 빌드 환경 요구사항

| 컴포넌트 | 필요 환경 |
|----------|----------|
| BE | JDK 17 + Gradle |
| FE (Android) | Android Studio + JDK 21 + Android SDK 28-36 |
| ai | Python 3.11 + pip/venv + ~4GB 디스크 |
| Infra | Docker Desktop |

---

## 로컬 구동 최소 조건 (단계별)

1. **Docker Desktop** 설치 → `docker-compose up postgres redis` (DB/캐시)
2. **환경변수 파일** 생성 → AES_ENCRYPTION_KEY, JWT_SECRET 임의 생성
3. **SSAFY OAuth 대체** → test-login 모드 활성화 또는 자체 인증 로직 구현
4. **SMS 우회** → `app.test-login.enabled=true` 설정
5. **BE 실행** → JDK 17 + `./gradlew bootRun` (환경변수 주입)
6. **AI 서비스** → Anthropic API Key 필요 (없으면 AI 기능 포기, BE만 운영)
7. **Android 앱** → Android Studio에서 `Constants.kt`의 URL을 `http://10.0.2.2:8080/api/`로 수정 후 에뮬레이터 실행
