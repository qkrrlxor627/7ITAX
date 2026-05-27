# 7iTAX 네트워크 통신 구조

## 1. 전체 아키텍처

```
┌────────────────────────────────────────────────────────┐
│                     Android App                        │
│                                                        │
│  ┌────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  UI Screen  │→│  ViewModel   │→│  Repository    │  │
│  │  (Compose)  │←│  (StateFlow) │←│  (Impl)        │  │
│  └────────────┘  └──────────────┘  └───────┬───────┘  │
│                                            │           │
│                                   ┌────────▼────────┐  │
│                                   │  Retrofit + Gson │  │
│                                   └────────┬────────┘  │
│                                            │           │
│                                   ┌────────▼────────┐  │
│                                   │     OkHttp       │  │
│                                   │  (Interceptor)   │  │
│                                   └────────┬────────┘  │
└────────────────────────────────────────────┼───────────┘
                                             │
                                     HTTPS (인터넷)
                                     LTE / WiFi
                                             │
┌────────────────────────────────────────────▼───────────┐
│                     EC2 서버                            │
│                                                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Spring Boot (:8080)                 │   │
│  │                                                 │   │
│  │   /api/auth/*          인증 API                  │   │
│  │   /api/cards/*         카드 API                  │   │
│  │   /api/payments/*      결제 API                  │   │
│  │   /api/book-entries/*  간편장부 API               │   │
│  │   /api/classification  세금 분류 API              │   │
│  │   /api/tax-calendar/*  세금 달력 API              │   │
│  │   /api/tax-estimation  세금 추정 API              │   │
│  │   /api/export/*        내보내기 API               │   │
│  └─────────────────────────────────────────────────┘   │
│                          │                             │
│                  ┌───────▼───────┐                     │
│                  │   Database    │                     │
│                  │  (MySQL 등)   │                     │
│                  └───────────────┘                     │
└────────────────────────────────────────────────────────┘
```

---

## 2. Base URL 설정

| 환경 | Base URL | 용도 |
|------|----------|------|
| DEBUG (에뮬레이터) | `http://10.0.2.2:8080/api/` | 로컬 PC 서버 접근 (에뮬레이터 전용) |
| DEBUG (실기기) | `http://{EC2 공인IP}:8080/api/` | EC2 서버 접근 (변경 필요) |
| RELEASE | `https://api.taxsave.app/api/` | 프로덕션 서버 (도메인 미확정) |

> **참고**: `10.0.2.2`는 Android 에뮬레이터에서 호스트 PC의 localhost를 가리키는 특수 IP.
> 실기기에서는 사용 불가하므로 EC2 서버의 공인 IP 또는 도메인을 사용해야 함.

설정 파일: `app/.../util/Constants.kt`

---

## 3. 요청/응답 흐름

### 3.1 요청 흐름 (앱 → 서버)

```
① UI에서 사용자 입력
     │
② ViewModel이 Repository 메서드 호출
     │
③ Repository가 AuthApi (Retrofit 인터페이스) 호출
     │
④ Retrofit이 Kotlin data class → JSON 직렬화 (Gson)
     │
⑤ OkHttp Interceptor가 헤더 자동 추가
     │  Authorization: Bearer {accessToken}
     │  Content-Type: application/json
     │
⑥ OkHttp가 HTTP 요청 전송 (인터넷 경유)
     │
⑦ EC2 서버가 요청 수신 및 처리
```

### 3.2 응답 흐름 (서버 → 앱)

```
① EC2 서버가 JSON 응답 반환
     │
② OkHttp가 응답 수신
     │
③ HttpLoggingInterceptor가 Logcat에 로깅 (DEBUG만)
     │
④ Retrofit이 JSON → Kotlin data class 역직렬화 (Gson)
     │
⑤ Repository가 응답 처리 (토큰 저장 등)
     │
⑥ ViewModel이 UI 상태 업데이트 (StateFlow)
     │
⑦ Compose UI 자동 리컴포지션
```

---

## 4. 공통 응답 형식

모든 API 응답은 아래 형식으로 래핑됨:

```json
// 성공
{
  "status": "success",
  "message": "OK",
  "data": { ... }
}

// 실패
{
  "status": "fail",
  "errorCode": "ERROR_CODE",
  "message": "에러 메시지",
  "data": null
}
```

Kotlin 매핑: `ApiResponse<T>` (data/model/common/ApiResponse.kt)

---

## 5. 인증 토큰 관리

### 5.1 토큰 저장

```
EncryptedSharedPreferences (AES256-GCM 암호화)
  ├── access_token   : JWT 액세스 토큰 (API 인증용)
  ├── refresh_token  : 리프레시 토큰 (토큰 갱신용)
  ├── user_id        : 서버 사용자 ID
  └── phone_number   : 휴대폰 번호 (재로그인용)
```

### 5.2 자동 토큰 삽입 (OkHttp Interceptor)

```
모든 HTTP 요청
     │
     ▼
OkHttp Interceptor
     │
     ├── SecureStorage에 accessToken 있음?
     │     → Authorization: Bearer {accessToken} 헤더 추가
     │
     └── 없음 (DEBUG 모드)?
           → Authorization: Bearer dev_access_token_placeholder
```

설정 파일: `data/remote/ApiClient.kt`

### 5.3 토큰 갱신 흐름

```
API 호출 → 401 TOKEN_EXPIRED 응답
     │
     ▼
POST /api/auth/reissue  { refreshToken: "..." }
     │
     ├── 성공 → 새 accessToken, refreshToken 저장 → 원래 요청 재시도
     │
     └── 실패 → 로그아웃 처리 → 로그인 화면으로
```

---

## 6. 인증 API 통신 상세

### 6.1 신규 사용자 플로우

```
[1] 본인인증
    POST /api/auth/verify-identity
    Body: { name, birthDate(YYYY-MM-DD), gender(M/F), phoneNumber }
    Response: { userId, isNewUser, requiresPinSetup, requiresConsent }
         │
[2] PIN 설정 (토큰 발급)
    POST /api/auth/setup-pin?userId={userId}
    Body: { pin }
    Response: { accessToken, refreshToken }
         │
[3] 약관 동의 (JWT 필요)
    POST /api/auth/consents
    Header: Authorization: Bearer {accessToken}
    Body: [{ consentType: "SERVICE", agreed: true }, ...]
    Response: { status: "success" }
         │
[4] 홈 화면 진입
```

### 6.2 재방문 사용자 플로우

```
[1] 앱 실행 → SecureStorage에 phoneNumber 존재 확인
         │
[2] PIN 로그인
    POST /api/auth/login
    Body: { phoneNumber, pin }
    Response: { accessToken, refreshToken }
         │
[3] 홈 화면 진입
```

### 6.3 로그아웃

```
POST /api/auth/logout
Header: Authorization: Bearer {accessToken}
Response: { status: "success" }
    │
    ▼
SecureStorage 전체 삭제 → 로그인 화면으로
```

---

## 7. 네트워크 설정 요약

| 항목 | 값 |
|------|-----|
| HTTP 클라이언트 | OkHttp 4.x |
| REST 클라이언트 | Retrofit 2.11.0 |
| JSON 변환기 | Gson |
| 연결 타임아웃 | 10초 |
| 읽기 타임아웃 | 10초 |
| 쓰기 타임아웃 | 10초 |
| 로깅 (DEBUG) | BODY (요청/응답 전체) |
| 로깅 (RELEASE) | NONE |
| 토큰 저장소 | EncryptedSharedPreferences (AES256) |

---

## 8. 주요 파일 위치

```
app/src/main/java/com/ssafy/seveniTax/
├── data/
│   ├── local/
│   │   └── SecureStorage.kt          # 암호화 저장소 (토큰, PIN 등)
│   ├── model/
│   │   ├── auth/                      # Auth 요청/응답 DTO
│   │   │   ├── VerifyIdentityRequest.kt
│   │   │   ├── VerifyIdentityResponse.kt
│   │   │   ├── SetupPinRequest.kt
│   │   │   ├── LoginRequest.kt
│   │   │   ├── TokenResponse.kt
│   │   │   ├── ConsentItem.kt
│   │   │   ├── ReissueRequest.kt
│   │   │   └── TermItem.kt
│   │   └── common/
│   │       └── ApiResponse.kt         # 공통 응답 래퍼
│   ├── remote/
│   │   ├── ApiClient.kt              # OkHttp 설정 (Interceptor, 타임아웃)
│   │   └── AuthApi.kt                # Retrofit 인터페이스 (엔드포인트 정의)
│   └── repository/
│       ├── AuthRepository.kt         # 인터페이스
│       └── AuthRepositoryImpl.kt     # 구현 (API 호출 + 토큰 관리)
├── di/
│   └── NetworkModule.kt              # Hilt DI (Retrofit, OkHttp, API 주입)
├── util/
│   └── Constants.kt                  # Base URL, 키 상수
└── viewmodel/
    └── AuthViewModel.kt              # UI 상태 관리 + API 호출 트리거
```
