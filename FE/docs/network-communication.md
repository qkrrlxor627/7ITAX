# 7iTAX 프론트-백엔드 통신 구조

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (FE)                      │
│                                                         │
│  UI (Compose Screen)                                    │
│       ↕ StateFlow                                       │
│  ViewModel                                              │
│       ↕ suspend fun                                     │
│  Repository                                             │
│       ↕ suspend fun                                     │
│  Api Interface (Retrofit)                               │
│       ↕ HTTP Request/Response                           │
│  OkHttpClient (Interceptor: 토큰 자동 주입, 로깅)        │
│       ↕                                                 │
└───────────── HTTPS ─────────────────────────────────────┘
                    ↕
┌───────────── Nginx (Reverse Proxy) ─────────────────────┐
│  https://j14c203.p.ssafy.io                             │
│       /api/**  → tax-backend:8080                       │
│       /ai/**   → ai-server                              │
└─────────────────────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────────────────────┐
│               Spring Boot (BE)  :8080                    │
│                                                         │
│  Controller → Service → Repository → DB                 │
│                                                         │
│  응답 형식:                                              │
│  { "status": "success", "message": "", "data": {...} }  │
└─────────────────────────────────────────────────────────┘
```

---

## 1. Base URL 설정

**파일:** `util/Constants.kt`

```kotlin
val API_BASE_URL = "https://j14c203.p.ssafy.io/api/"
```

이 URL이 Retrofit의 baseUrl로 주입된다.

**파일:** `di/NetworkModule.kt`

```kotlin
Retrofit.Builder()
    .baseUrl(Constants.API_BASE_URL)   // ← 여기서 연결
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

---

## 2. HTTP 요청 흐름 (프론트 → 백)

```
사용자 액션 (버튼 클릭 등)
    ↓
ViewModel에서 함수 호출 (viewModelScope.launch)
    ↓
Repository.함수() 호출
    ↓
Api 인터페이스의 suspend fun 실행
    ↓
Retrofit이 HTTP 요청 생성
    ↓
OkHttp Interceptor가 Authorization 헤더 자동 추가
    ↓
HTTPS 요청 전송 → https://j14c203.p.ssafy.io/api/...
```

### 토큰 자동 주입 (ApiClient.kt)

모든 요청에 OkHttp Interceptor가 동작한다:

```kotlin
// 1. SecureStorage에서 저장된 토큰 조회
val token = secureStorage.getAccessToken()

// 2. 요청 헤더에 자동 추가
request.addHeader("Authorization", "Bearer $token")
```

---

## 3. HTTP 응답 흐름 (백 → 프론트)

```
서버 응답 (JSON)
    ↓
OkHttp가 수신
    ↓
Gson이 ApiResponse<T>로 자동 역직렬화
    ↓
Repository에서 response.body() 추출
    ↓
ViewModel에서 _uiState.update { ... }
    ↓
Compose UI가 StateFlow 변경 감지 → 화면 갱신
```

### 공통 응답 형식 (ApiResponse)

```json
{
  "status": "success",
  "message": "",
  "errorCode": null,
  "data": { ... }
}
```

```kotlin
data class ApiResponse<T>(
    val status: String,
    val message: String = "",
    val errorCode: String? = null,
    val data: T? = null
)
```

### 페이징 응답 (PageResponse)

```json
{
  "status": "success",
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

---

## 4. 인증 흐름

```
[신규 유저]
POST /api/auth/verify-identity  →  userId 획득
POST /api/auth/setup-pin        →  accessToken, refreshToken 획득 & 저장
POST /api/auth/consents         →  약관 동의 (토큰 필요)

[기존 유저]
POST /api/auth/login            →  accessToken, refreshToken 획득 & 저장

[토큰 갱신]
POST /api/auth/reissue          →  새 accessToken 획득

[로그아웃]
POST /api/auth/logout           →  서버 토큰 무효화 + 로컬 삭제
```

### 토큰 저장소: SecureStorage (EncryptedSharedPreferences)

```
AES256 암호화로 로컬 저장:
├── access_token    ← API 호출 시 Bearer 토큰으로 사용
├── refresh_token   ← 토큰 만료 시 재발급용
├── user_id
├── phone_number
└── pay_enrolled
```

---

## 5. API 엔드포인트 목록

### 인증 (AuthApi) — 공개/인증 혼합

| Method | Path | 인증 | 설명 |
|--------|------|:----:|------|
| POST | `/api/auth/verify-identity` | X | 본인인증 (Mock) |
| POST | `/api/auth/setup-pin` | X | PIN 설정 + 토큰 발급 |
| POST | `/api/auth/login` | X | 로그인 + 토큰 발급 |
| POST | `/api/auth/consents` | O | 약관 동의 저장 |
| POST | `/api/auth/reissue` | X | 토큰 재발급 |
| POST | `/api/auth/logout` | O | 로그아웃 |

### 페이/계좌 (PayApi) — 인증 필요

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/banking/accounts` | 계좌 생성 |
| GET | `/api/banking/accounts` | 계좌 목록 |
| GET | `/api/banking/accounts/{id}` | 계좌 상세 |
| GET | `/api/banking/accounts/{id}/balance` | 잔액 조회 |

### 카드 (CardApi) — 인증 필요

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/cards` | 카드 등록 |
| POST | `/api/cards/{id}/activate` | 카드 활성화 |
| PATCH | `/api/cards/{id}/purpose` | 용도 설정 |
| GET | `/api/cards` | 카드 목록 |
| DELETE | `/api/cards/{id}` | 카드 삭제 |

### 간편장부 (BookEntryApi) — 인증 필요

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/book-entries` | 장부 생성 |
| GET | `/api/book-entries` | 장부 목록 (페이징) |
| GET | `/api/book-entries/{id}` | 장부 상세 |
| GET | `/api/book-entries/unconfirmed-count` | 미확인 건수 |
| PATCH | `/api/book-entries/{id}/confirm` | 확인 처리 |
| PATCH | `/api/book-entries/{id}/category` | 카테고리 변경 |
| PATCH | `/api/book-entries/{id}/personal` | 개인용 표시 |
| PATCH | `/api/book-entries/{id}/business` | 사업용 표시 |

### 세금 (TaxEstimationApi, TaxCalendarApi) — 인증 필요

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/tax-estimation` | 예상 세금 계산 |
| GET | `/api/tax-calendar/deadlines` | 세금 납부 일정 |

### 세목 분류 (ClassificationApi) — 인증 필요

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/classification` | AI 세목 자동 분류 |

### 내보내기 (ExportApi) — 인증 필요

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/export/book-entries` | 장부 CSV 내보내기 |
| GET | `/api/export/vat` | 부가세 CSV |
| GET | `/api/export/income-tax` | 소득세 CSV |

---

## 6. 코드 레이어별 역할

| 레이어 | 위치 | 역할 |
|--------|------|------|
| **Constants** | `util/Constants.kt` | Base URL 정의 |
| **ApiClient** | `data/remote/ApiClient.kt` | OkHttp 설정 (토큰 주입, 로깅, 타임아웃) |
| **NetworkModule** | `di/NetworkModule.kt` | Hilt DI로 Retrofit + Api 인스턴스 제공 |
| **Api Interface** | `data/remote/*Api.kt` | Retrofit 엔드포인트 정의 (@GET, @POST 등) |
| **Repository** | `data/repository/*Impl.kt` | API 호출 + 응답 처리 + 토큰 저장 |
| **ViewModel** | `viewmodel/*.kt` | UI 상태 관리 + Repository 호출 |
| **Screen** | `ui/**/*Screen.kt` | 사용자 인터랙션 + 상태 표시 |

---

## 7. 에러 처리 흐름

```
API 호출 실패
    ↓
Repository에서 catch
    ├── response.errorBody()?.string()  → 서버 에러 메시지
    └── Exception.message               → 네트워크 에러
    ↓
ViewModel에서 _uiState.update { errorMessage = "..." }
    ↓
Screen에서 에러 메시지 표시
```

---

## 8. 요청/응답 예시

### 본인인증 요청

```
→ POST https://j14c203.p.ssafy.io/api/auth/verify-identity
→ Headers: Content-Type: application/json
→ Body:
{
  "name": "홍길동",
  "birthDate": "1990-01-01",
  "gender": "M",
  "phoneNumber": "01012345678"
}

← 200 OK
← Body:
{
  "status": "success",
  "data": {
    "userId": 1,
    "isNewUser": true,
    "requiresPinSetup": true,
    "requiresConsent": true
  }
}
```

### 인증 필요 API 요청 (카드 목록)

```
→ GET https://j14c203.p.ssafy.io/api/cards
→ Headers:
    Content-Type: application/json
    Authorization: Bearer eyJhbGciOiJIUz...  ← OkHttp Interceptor가 자동 추가

← 200 OK
← Body:
{
  "status": "success",
  "data": [
    { "cardId": 1, "cardName": "테스트카드", "cardType": "CREDIT", ... }
  ]
}
```
