# 7iTAX Frontend Architecture Design

> Kotlin + Jetpack Compose + WebView(React) 하이브리드 아키텍처 설계서  
> SSAFY 14기 광주 2반 C203팀 | 2025.07 | v1.0

---

## 1. 프로젝트 개요

### 1.1 서비스 소개

7iTAX는 프리랜서 개발자(업종코드 722000) 대상 세금 관리 핀테크 앱입니다. 본 설계서는 1차 개발 범위인 **Auth(인증), Pay(생성/카드등록)** 3개 핵심 기능의 프론트엔드 아키텍처를 다룹니다.

**1차 개발 범위 (3개 기능):**

| # | 기능 | 처리 방식 | 설명 |
|---|------|-----------|------|
| 1 | Auth (인증) | Native | 온보딩, 로그인, PIN, 생체인증 |
| 2 | Pay 생성 | Native | Pay 계좌 개설 |
| 3 | Pay 카드 등록 | Native | 결제 카드 등록 |

> Pay 결제, 홈, 간편장부, 세금 대시보드, AI 추천, 마이페이지는 2차 개발 범위로 확장 예정입니다.

### 1.2 기술 스택

| 구분 | 기술 | 버전 / 비고 |
|------|------|-------------|
| Native Shell | Kotlin + Jetpack Compose | 앱 뼈대, 보안 화면, 선언형 UI |
| 디자인 패턴 | MVVM | View(Compose) → ViewModel(StateFlow) → Model(Repository) |
| 비동기 처리 | Kotlin Coroutine | viewModelScope, suspend fun 기반 |
| Repository | Interface + Impl | DI @Binds 바인딩, 테스트 용이 |
| 상태관리 (Native) | ViewModel + StateFlow | MVVM ViewModel, Coroutine으로 비동기 상태 관리 |
| HTTP 통신 (Native) | Retrofit 2.11.0 + OkHttp | 네이티브 측 API 통신 |
| 로컬 저장 | EncryptedSharedPreferences | 토큰, PIN 보안 저장 |
| DI | Hilt (KSP) | 의존성 주입 |
| WebView | React (TypeScript) | 2차 범위 — 콘텐츠/데이터 표시 화면 SPA |
| WebView 서빙 | Nginx | 2차 범위 — Static files + Reverse Proxy |
| 컨테이너 | Docker (Docker Compose) | EC2 위 전체 서비스 컨테이너화 |
| CI/CD | Jenkins | GitLab Webhook → Jenkins 자동 빌드/배포 |
| 소스 관리 | GitLab | 코드 저장소, Webhook 트리거 |
| Target SDK | Android 16 (API 36) | minSdk 28 (Galaxy S10 기준) |

### 1.3 프론트엔드 범위

본 아키텍처는 모바일 프론트엔드 영역만 다룹니다.

- **프론트엔드**: Kotlin + Compose + WebView 하이브리드 앱 (본 문서)
- **백엔드**: Java 17 + Spring Boot 3.x REST API
- **AI 서비스**: FastAPI + LangChain + ChromaDB (RAG 기반 세금 분류) → GMS LLM API 연동
- **데이터베이스**: PostgreSQL + Redis + ChromaDB (벡터 DB)

---

## 2. 아키텍처 설계

### 2.1 하이브리드 아키텍처 개요

Kotlin Native Shell + WebView(React SPA) 하이브리드 구조를 채택합니다. Native 측은 **Jetpack Compose + Coroutine + MVVM** 조합을 핵심 기술로 사용하며, 보안/디바이스 접근이 필요한 화면은 Compose로, 콘텐츠/데이터 표시 화면은 WebView로 처리하여 보안성과 개발 생산성을 동시에 확보합니다.

```
┌─ EC2 ─────────────────────────────────────────────────────────────┐
│  ┌─ Docker Compose ─────────────────────────────────────────────┐ │
│  │                                                               │ │
│  │  ┌─────────┐   Static files    ┌──────────────────────┐      │ │
│  │  │         │ ──────────────── → │  React SPA (빌드파일) │      │ │
│  │  │  Nginx  │                   └──────────────────────┘      │ │
│  │  │         │   Reverse Proxy   ┌──────────┐  ┌───────────┐  │ │
│  │  │         │ ──────────────── → │  Spring  │  │  FastAPI  │──┼─┼──→ GMS LLM API
│  │  └────▲────┘                   │  Boot    │  │  (AI)     │  │ │
│  │       │                        └──────────┘  └───────────┘  │ │
│  │       │                              │                       │ │
│  │       │                   ┌──────────▼───────────────┐      │ │
│  │       │                   │ PostgreSQL + Redis        │      │ │
│  │       │                   │ + ChromaDB (벡터 DB)      │      │ │
│  │       │                   └──────────────────────────┘      │ │
│  │       │                                                      │ │
│  │  ┌────┴─────┐   Webhook   ┌──────────┐  git push  ┌───────┐ │ │
│  │  │ Jenkins  │ ◄─────────  │  GitLab  │ ◄────────  │ Dev   │ │ │
│  │  │ (CI/CD)  │             │          │            │       │ │ │
│  │  └──────────┘             └──────────┘            └───────┘ │ │
│  └──────────────────────────────────────────────────────────────┘ │
└──────────┬────────────────────────────────────────────────────────┘
           │ WebView (HTTPS)
┌──────────┴────────────┐
│  User (Kotlin App)    │
│  Compose + WebView    │
└───────────────────────┘
```

### 2.2 핵심 기술: Jetpack Compose + Coroutine + MVVM

Native(Kotlin) 측의 3가지 핵심 기술과 각각의 역할:

```
┌─────────────────────────────────────────────────────────────────┐
│                        MVVM 패턴                                │
│                                                                 │
│  ┌─────────────┐      ┌──────────────┐      ┌──────────────┐   │
│  │    View      │      │  ViewModel   │      │    Model     │   │
│  │  (Compose)   │─────→│  (StateFlow) │─────→│ (Repository) │   │
│  │             │←─────│              │←─────│              │   │
│  └─────────────┘      └──────────────┘      └──────────────┘   │
│   Jetpack Compose       Coroutine로           Coroutine로      │
│   가 상태 변화를         비동기 처리             API 호출         │
│   자동 감지 → 리렌더     (viewModelScope)       (suspend fun)   │
└─────────────────────────────────────────────────────────────────┘
```

#### Jetpack Compose — 선언형 UI

기존 XML Layout 방식은 "버튼을 만들어 → 텍스트를 바꿔 → 색상을 바꿔" 명령형이었다. Compose는 "이 상태일 때 화면은 이렇게 생겼다"고 선언만 하면, 상태가 바뀔 때 알아서 다시 그린다.

```kotlin
// 상태가 바뀌면 Compose가 자동으로 UI를 다시 그림
@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.loading) {
        LoadingOverlay()       // loading = true면 로딩 표시
    }

    TaxButton(text = "SSAFY로 시작하기") {
        viewModel.login()      // 버튼 눌림 → ViewModel에 전달만
    }

    uiState.error?.let {
        ErrorDialog(message = it)  // error 있으면 에러 다이얼로그
    }
}
```

**선택 이유:**
- Google 공식 권장 UI 프레임워크 (Play Store 상위 1000개 앱 중 60% 사용)
- XML 대비 코드량 약 40% 감소
- StateFlow와 결합하면 상태 → UI 자동 반영 (수동 UI 업데이트 코드 불필요)

#### Coroutine — 비동기 처리

네트워크 호출, DB 접근 같은 오래 걸리는 작업을 메인 스레드를 막지 않고 처리한다. `suspend fun`으로 비동기 코드를 동기처럼 읽을 수 있게 작성한다.

```kotlin
// ❌ 콜백 지옥 (Coroutine 없이)
api.login(code, object : Callback {
    override fun onSuccess(token: Token) {
        storage.save(token, object : Callback {
            override fun onSuccess() {
                runOnUiThread { showHome() }
            }
        })
    }
    override fun onFailure(e: Exception) {
        runOnUiThread { showError(e) }
    }
})

// ✅ Coroutine (동기처럼 읽히는 비동기 코드)
fun login(code: String) = viewModelScope.launch {
    _uiState.update { it.copy(loading = true) }
    val token = authRepository.login(code)    // suspend fun — 알아서 백그라운드
    secureStorage.saveToken(token)            // suspend fun — 알아서 백그라운드
    _uiState.update { it.copy(isLoggedIn = true) }  // 메인 스레드에서 UI 업데이트
}
```

**선택 이유:**
- 콜백 지옥 제거 → 코드 가독성 대폭 향상
- `viewModelScope`: ViewModel 파괴 시 자동으로 Coroutine 취소 (메모리 누수 방지)
- Retrofit, Room 등 주요 라이브러리가 모두 Coroutine 네이티브 지원

#### MVVM — 디자인 패턴

| 레이어 | 역할 | 기술 | 7iTAX 예시 |
|--------|------|------|-----------|
| View | UI 렌더링, 이벤트 감지 | Jetpack Compose | LoginScreen.kt |
| ViewModel | 비즈니스 로직, 상태 관리 | StateFlow + Coroutine | AuthViewModel.kt |
| Model | 데이터 접근 | Repository + Retrofit | AuthRepository.kt |

View는 ViewModel만, ViewModel은 Model만 안다. 역방향 의존 금지.

**MVVM을 선택한 이유:**

| 비교 항목 | MVVM | MVP | MVI |
|-----------|------|-----|-----|
| Compose 호환 | StateFlow observe → 자동 recompose | View 인터페이스 직접 참조 (명령형) | 가능하지만 보일러플레이트 과다 |
| Google 권장 | 공식 권장 (Android Architecture Guide) | 레거시 (XML 시절) | 부분 권장 |
| 비동기 처리 | Coroutine + StateFlow 자연스러운 결합 | Coroutine 사용 가능하나 구조적 이점 적음 | Coroutine + reduce 패턴 조합 |
| 상태 관리 | StateFlow 단순 명확 | Presenter에서 수동 관리 | 단일 State 강제 (복잡도 증가) |
| 프로젝트 적합성 | 중소 규모 최적 | Compose와 맞지 않음 | 대규모/복잡 상태에 유리 |

#### 3가지 기술이 함께 동작하는 흐름

```
[사용자 버튼 클릭]
    │
    ▼ Jetpack Compose가 이벤트 감지
┌─────────────────────┐
│  View (Compose)     │  viewModel.login() 호출
└──────────┬──────────┘
           │
           ▼ Coroutine이 비동기 처리
┌─────────────────────┐
│  ViewModel          │  viewModelScope.launch {
│  (StateFlow)        │      repository.login()    ← suspend fun
│                     │      _uiState.update(...)   ← 상태 변경
└──────────┬──────────┘  }
           │
           ▼ Coroutine이 네트워크 호출
┌─────────────────────┐
│  Model (Repository)  │  suspend fun login()
│  → Retrofit API      │  → 백그라운드 스레드에서 서버 통신
└──────────┬──────────┘
           │
           ▼ 상태 변경 → Compose가 자동 감지
┌─────────────────────┐
│  View (Compose)     │  collectAsStateWithLifecycle()
│                     │  → 상태 바뀌면 알아서 UI 다시 그림
└─────────────────────┘
```

### 2.3 Native / WebView 화면 분리 기준

| 기준 | Native (Compose) | WebView (React) |
|------|-------------------|-----------------|
| 보안 | PIN 입력, 생체인증, 토큰 관리 | - |
| 디바이스 API | 카메라, 푸시, Keystore | - |
| 콘텐츠 표시 | - | 차트, 목록, 필터, 폼 |
| 업데이트 주기 | 앱 배포 필요 | 서버 배포로 즉시 반영 |
| UX 성능 | 네이티브 전환 애니메이션 | 웹 기반 SPA 라우팅 |

### 2.4 화면별 배치 (1차 범위)

**전체 Native (Compose) — 1차 범위는 모두 네이티브**

| 화면 | 네이티브 이유 |
|------|-------------|
| Splash | 토큰 검증, 앱 초기화 |
| 온보딩 전체 (10화면) | 본인인증, SMS, PIN 설정 (보안) |
| Pay 가입 (4화면) | 약관동의, 본인인증, 계좌 개설 |
| 카드 등록 (7화면) | 카드 정보 입력, 소유자 인증, SMS |

> 홈(WebView), Pay 결제, 거래 내역 등은 2차 개발 범위입니다.

### 2.5 데이터 흐름

**1차 범위 — 전체 Native 흐름:**
```
User Action → Compose UI → ViewModel → Repository(Interface) → RepositoryImpl → Retrofit → Backend
Backend → Retrofit → RepositoryImpl → ViewModel(StateFlow) → Compose UI(recompose)
```

> WebView 흐름 및 Bridge 통신은 2차 범위(홈 화면 등) 개발 시 적용 예정입니다.

---

## 3. 패키지 구조

### 3.1 Native (Kotlin) 디렉토리 구조

```
app/src/main/java/com/ssafy/seveniTax/
├── di/                        # Hilt 의존성 주입
│   ├── AppModule.kt               # SecureStorage 등 싱글톤
│   ├── NetworkModule.kt           # Retrofit, OkHttp, API Interface
│   └── RepositoryModule.kt        # Interface → Impl 바인딩 (@Binds)
│
├── ui/                        # Compose UI
│   ├── navigation/
│   │   ├── NavGraph.kt            # 전체 네비게이션 그래프
│   │   └── Route.kt               # sealed class 라우트 정의
│   ├── auth/                      # 온보딩 + 회원가입 (10화면)
│   │   ├── SplashScreen.kt
│   │   ├── PhoneInputScreen.kt
│   │   ├── ResidentNumberScreen.kt
│   │   ├── CarrierSelectScreen.kt
│   │   ├── NameInputScreen.kt
│   │   ├── TermsScreen.kt
│   │   ├── SmsVerifyScreen.kt
│   │   ├── PinSetupScreen.kt
│   │   ├── PinConfirmScreen.kt
│   │   └── AuthSuccessScreen.kt
│   ├── pay/                       # Pay 가입 (4화면)
│   │   ├── PayIntroScreen.kt         # Pay 가입 안내
│   │   ├── PayTermsScreen.kt         # Pay 약관 동의
│   │   ├── PayVerifyScreen.kt        # 본인 인증
│   │   └── PayCompleteScreen.kt      # 가입 완료
│   ├── card/                      # 카드 등록 (7화면)
│   │   ├── CardListScreen.kt         # 카드 관리 (목록)
│   │   ├── CardTypeSelectScreen.kt   # 일반/사업자 카드 선택
│   │   ├── CardInputScreen.kt        # 카드 정보 입력
│   │   ├── CardOwnerVerifyScreen.kt  # 소유자 인증
│   │   ├── CardSmsScreen.kt          # SMS 인증
│   │   ├── CardCompleteScreen.kt     # 등록 완료
│   │   └── CardChangeScreen.kt       # 대표 카드 변경
│   ├── components/                # 공통 Compose 컴포넌트
│   │   ├── TaxButton.kt
│   │   ├── TaxInput.kt
│   │   ├── TaxHeader.kt
│   │   ├── PinKeypad.kt
│   │   ├── PinIndicator.kt
│   │   ├── CardInputForm.kt
│   │   └── LoadingOverlay.kt
│   └── theme/                     # Material3 테마
│       ├── Color.kt
│       ├── Type.kt
│       └── Theme.kt
│
├── viewmodel/                 # ViewModel (상태 관리)
│   ├── AuthViewModel.kt
│   ├── PayViewModel.kt
│   └── CardViewModel.kt
│
├── data/                      # 데이터 레이어
│   ├── repository/                # Interface + Impl 분리
│   │   ├── AuthRepository.kt         # interface
│   │   ├── AuthRepositoryImpl.kt     # 구현체
│   │   ├── PayRepository.kt          # interface
│   │   ├── PayRepositoryImpl.kt      # 구현체
│   │   ├── CardRepository.kt         # interface
│   │   └── CardRepositoryImpl.kt     # 구현체
│   ├── remote/                    # Retrofit API
│   │   ├── ApiClient.kt              # OkHttp + Interceptor (토큰 주입)
│   │   ├── AuthApi.kt
│   │   ├── PayApi.kt
│   │   └── CardApi.kt
│   ├── local/                     # 로컬 저장
│   │   └── SecureStorage.kt          # EncryptedSharedPreferences
│   └── model/                     # 데이터 모델
│       ├── common/
│       │   └── ApiResponse.kt        # 공통 래퍼
│       ├── auth/                      # Auth 관련 모델
│       ├── pay/                       # Pay 관련 모델
│       └── card/                      # Card 관련 모델
│
├── bridge/                    # Native ↔ WebView 통신 (2차 범위)
│   ├── WebBridge.kt
│   └── BridgeEvent.kt
│
└── util/                      # 유틸
    ├── BiometricHelper.kt
    └── Constants.kt
```

### 3.2 WebView (React SPA) 디렉토리 구조

> 2차 개발 범위 (홈 대시보드, 거래 내역 등). 1차에서는 사용하지 않습니다.

### 3.3 폴더 역할 요약

**Native (Kotlin)**

| 폴더 | 역할 | 포함 내용 |
|------|------|-----------|
| di/ | 의존성 주입 | AppModule, NetworkModule, RepositoryModule |
| ui/ | Compose 화면 + 컴포넌트 | auth(10), pay(4), card(7), components, theme |
| viewmodel/ | 화면 상태 관리 | AuthViewModel, PayViewModel, CardViewModel |
| data/ | 데이터 접근 레이어 | repository(Interface+Impl), remote(API), local, model |
| bridge/ | WebView 통신 (2차) | JavaScriptInterface, 이벤트 타입 |
| util/ | 유틸리티 | 생체인증, 상수 |

---

## 4. 네비게이션 설계

### 4.1 네비게이션 구조

Compose Navigation 기반으로, 인증 상태에 따라 분기합니다. 1차 범위는 전체 Native Compose입니다.

```
NavGraph (Compose Navigation)
  └── 토큰 확인
        ├── 없음 → Auth 플로우 (10화면)
        │             ├── Splash → PhoneInput → ResidentNumber
        │             ├── CarrierSelect → NameInput → Terms
        │             ├── SmsVerify → PinSetup → PinConfirm
        │             └── AuthSuccess → PayIntro
        │
        └── 있음 → PayIntro
                      ├── Pay 가입 → PayTerms → PayVerify → PayComplete → CardList
                      └── 카드 등록 → CardTypeSelect → CardInput → CardOwnerVerify
                                       → CardSms → CardComplete → CardList
```

### 4.2 Auth 플로우 (10화면)

| # | 화면명 | 컴포넌트 | 설명 |
|---|--------|----------|------|
| 0 | Splash | SplashScreen.kt | 앱 진입 + 토큰 확인 |
| 1 | 휴대폰 입력 | PhoneInputScreen.kt | 전화번호 입력 |
| 2 | 주민번호 | ResidentNumberScreen.kt | 생년월일 + 뒷자리 1자리 |
| 3 | 통신사 선택 | CarrierSelectScreen.kt | SKT/KT/LGU + 알뜰폰 |
| 4 | 이름 입력 | NameInputScreen.kt | 실명 입력 → register() 호출 |
| 5 | 약관 동의 | TermsScreen.kt | 필수/선택 약관 동의 |
| 6 | SMS 인증 | SmsVerifyScreen.kt | 6자리 인증번호 입력 |
| 7 | PIN 설정 | PinSetupScreen.kt | 간편비밀번호 6자리 |
| 8 | PIN 확인 | PinConfirmScreen.kt | 재입력 확인 |
| 9 | 인증 성공 | AuthSuccessScreen.kt | ✓ → PayIntro 이동 |

### 4.3 Pay + Card 플로우

**Pay 가입 (4화면)**

| # | 화면명 | 컴포넌트 | 설명 |
|---|--------|----------|------|
| 1 | Pay 안내 | PayIntroScreen.kt | 가입 안내 + 시작 |
| 2 | Pay 약관 | PayTermsScreen.kt | Pay 전용 약관 동의 |
| 3 | 본인 인증 | PayVerifyScreen.kt | PASS/신분증 인증 |
| 4 | 가입 완료 | PayCompleteScreen.kt | → 카드 등록으로 이동 |

**카드 등록 (7화면)**

| # | 화면명 | 컴포넌트 | 설명 |
|---|--------|----------|------|
| 1 | 카드 목록 | CardListScreen.kt | 등록된 카드 관리 |
| 2 | 유형 선택 | CardTypeSelectScreen.kt | 일반/사업자 카드 |
| 3 | 정보 입력 | CardInputScreen.kt | 카드번호, MM/YY, CVC |
| 4 | 소유자 인증 | CardOwnerVerifyScreen.kt | 본인 여부 확인 |
| 5 | SMS 인증 | CardSmsScreen.kt | 인증번호 입력 |
| 6 | 등록 완료 | CardCompleteScreen.kt | → 카드 목록 |
| 7 | 카드 변경 | CardChangeScreen.kt | 대표 카드 변경 |

---

## 5. Native ↔ WebView Bridge 설계

> 2차 개발 범위 (홈 WebView 등). 1차에서는 전체 Native 구현이므로 Bridge를 사용하지 않습니다. 구조만 정의해두고 2차에서 활성화합니다.

### 5.2 Bridge 인터페이스

**Native → WebView (Kotlin이 Web에 데이터 전달)**

```kotlin
// Kotlin 측
webView.evaluateJavascript(
    "window.onNativeEvent(${Json.encodeToString(event)})",
    null
)
// 사용 예: 토큰 전달, 생체인증 결과 콜백, 결제 완료 알림
```

**WebView → Native (Web이 Native 기능 호출)**

```kotlin
// Kotlin 측 — WebBridge.kt
class WebBridge(
    private val onNavigate: (String) -> Unit,
    private val onRequestBiometric: (String) -> Unit,
    private val onRequestToken: () -> String
) {
    @JavascriptInterface
    fun navigateToNative(route: String) = onNavigate(route)

    @JavascriptInterface
    fun requestBiometric(callbackId: String) = onRequestBiometric(callbackId)

    @JavascriptInterface
    fun getAccessToken(): String = onRequestToken()

    @JavascriptInterface
    fun log(message: String) = Log.d("WebBridge", message)
}
```

```typescript
// React 측 — nativeBridge.ts
interface AndroidBridge {
  navigateToNative(route: string): void;
  requestBiometric(callbackId: string): void;
  getAccessToken(): string;
  log(message: string): void;
}

declare global {
  interface Window {
    AndroidBridge: AndroidBridge;
    onNativeEvent: (event: BridgeEvent) => void;
  }
}

export const bridge = {
  goToNative: (route: string) =>
    window.AndroidBridge?.navigateToNative(route),
  requestBiometric: () =>
    new Promise<boolean>((resolve) => {
      const id = `bio_${Date.now()}`;
      window[id] = (ok: boolean) => { resolve(ok); delete window[id]; };
      window.AndroidBridge?.requestBiometric(id);
    }),
  getToken: () => window.AndroidBridge?.getAccessToken() ?? '',
};
```

### 5.3 Bridge 통신 시나리오

| 시나리오 | 방향 | 흐름 |
|----------|------|------|
| 앱 시작 시 토큰 전달 | Native → Web | Kotlin이 토큰을 WebView에 주입 |
| 홈에서 결제 버튼 클릭 | Web → Native | React에서 bridge.goToNative("/pay/transfer") 호출 |
| 홈에서 카드 등록 클릭 | Web → Native | React에서 bridge.goToNative("/pay/card/register") 호출 |
| 생체인증 요청 | Web → Native → Web | React 요청 → Kotlin BiometricPrompt → 결과 콜백 |
| 결제 완료 후 복귀 | Native → Web | Kotlin이 window.onNativeEvent({type: "payComplete"}) 호출 |
| 로그아웃 | Web → Native | React에서 bridge.goToNative("/logout") → 토큰 삭제 → AuthGraph 이동 |

---

## 6. 상태 관리 설계

### 6.1 Native 측 (ViewModel + StateFlow)

인증, 결제 등 네이티브 화면의 상태를 관리합니다.

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(code: String) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true) }
        authRepository.login(code)
            .onSuccess { token ->
                secureStorage.saveToken(token)
                _uiState.update { it.copy(loading = false, isLoggedIn = true) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.message) }
            }
    }
}

data class AuthUiState(
    val loading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val loginStep: Int = 0
)
```

**Native ViewModel 목록:**

| ViewModel | 주입 의존성 | 주요 함수 |
|-----------|-----------|-----------|
| AuthViewModel | AuthRepository | checkAutoLogin, setPhone, setResidentNumber, setCarrier, setName, register, loadTerms, toggleAgreement, submitTerms, requestSms, verifySms, setPin, confirmPin |
| PayViewModel | PayRepository | checkPayAccount, loadAccounts, loadBalance, agreePayTerms, verifyIdentity, createAccount |
| CardViewModel | CardRepository | loadCards, selectCardType, createCard, requestOwnerVerify, verifySms, activateCard, setCardPurpose, deleteCard |

> ViewModel은 Repository **Interface**에만 의존한다. Impl은 Hilt(@Binds)가 주입.

### 6.2 WebView 측 (Redux Toolkit)

> 2차 개발 범위. 1차에서는 WebView 화면이 없으므로 미사용.

### 6.3 호출 흐름 예시

**Native 화면 (회원가입 — 약관 동의):**

```kotlin
@Composable
fun TermsScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadTerms() }

    // 약관 목록
    uiState.terms.forEach { term ->
        TermCheckItem(
            term = term,
            checked = uiState.agreements[term.id] == true,
            onToggle = { viewModel.toggleAgreement(term.id) }
        )
    }

    // 동의 버튼
    TaxButton(
        text = "동의하고 본인 인증하기",
        enabled = uiState.terms.filter { it.required }.all { uiState.agreements[it.id] == true }
    ) {
        viewModel.submitTerms()
    }

    // 성공 시 다음 화면
    LaunchedEffect(uiState.userStatus) {
        if (uiState.userStatus == "pending_kyc") onNext()
    }
}
```

---

## 7. API 통신 설계

### 7.1 Native 측 (Retrofit + OkHttp)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideOkHttp(secureStorage: SecureStorage): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = secureStorage.getAccessToken()
                val request = chain.request().newBuilder()
                    .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
```

### 7.2 WebView 측 (Axios)

> 2차 개발 범위. 1차에서는 전체 Native이므로 Axios를 사용하지 않습니다.

### 7.3 API 엔드포인트 목록 (1차 범위)

**Auth (8개)**

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /auth/register | 회원가입 |
| POST | /auth/login | 로그인 (토큰 발급) |
| POST | /auth/refresh | 토큰 갱신 |
| POST | /auth/logout | 로그아웃 |
| GET | /terms | 약관 목록 조회 |
| POST | /terms/agree | 약관 동의 제출 |
| POST | /verification/phone/request | SMS 인증번호 발송 |
| POST | /verification/phone/confirm | SMS 인증번호 확인 |

**Pay (4개)**

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /banking/accounts | 계좌 개설 |
| GET | /banking/accounts | 계좌 목록 조회 |
| GET | /banking/accounts/{id} | 계좌 상세 조회 |
| GET | /banking/accounts/{id}/balance | 잔액 조회 |

**Card (5개)**

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /cards | 카드 발급 신청 |
| POST | /cards/{id}/activate | 카드 활성화 (SMS 코드) |
| PATCH | /cards/{id}/purpose | 카드 결제 목적 설정 |
| GET | /cards | 카드 목록 조회 |
| DELETE | /cards/{id} | 카드 해지 |

---

## 8. 컴포넌트 설계

### 8.1 Native 공통 컴포넌트 (Compose)

| 컴포넌트 | Parameters | 설명 |
|----------|------------|------|
| TaxButton | text, onClick, variant(Primary/Secondary), enabled | 보라색 버튼 / 아웃라인 |
| TaxInput | value, onValueChange, placeholder, error, keyboardType | 텍스트 입력 + 에러 표시 |
| TaxHeader | title, onBack | 보라색 헤더(#2B2B6B) + 뒤로가기 |
| PinKeypad | onNumberClick, onDelete | 3x4 숫자 키패드 |
| PinIndicator | length, filled | 6개 원형 (빈○ / 채움●) |
| CardInputForm | cardNumber, expiry, cvc, onChange... | 카드 정보 입력 폼 |
| LoadingOverlay | visible, message | 전체화면 로딩 오버레이 |

### 8.2 WebView 공통 컴포넌트 (React)

> 2차 개발 범위. 홈 화면 등 WebView 화면 개발 시 작성.

### 8.3 디자인 시스템

Native(Compose) 디자인 토큰:

| 토큰 | 값 | 용도 |
|------|-----|------|
| primary | #2B2B6B | 브랜드 메인 컬러, 헤더 배경 |
| accent | #534AB7 | CTA 버튼, 강조 요소 |
| success | #1D9E75 | 성공, 수입 표시 |
| danger | #E24B4A | 에러, 지출 표시 |
| background | #FFFFFF | 기본 배경 |
| surface | #F5F5FC | 카드, 섹션 배경 |
| textPrimary | #1A1A1A | 본문 텍스트 |
| textSecondary | #8B95A1 | 보조 텍스트, 힌트 |

---

## 9. 에러 처리 전략

### 9.1 에러 분류

| 에러 유형 | Native 처리 |
|-----------|-------------|
| 401 Unauthorized | OkHttp Interceptor → refresh → 실패시 Auth 플로우 이동 |
| 400 Validation | ViewModel에서 에러 상태 → UI 에러 표시 |
| 409 Duplicate | "이미 가입된 계정" 안내 |
| 429 Rate Limit | "잠시 후 다시 시도" 안내 |
| 500 Server Error | Interceptor에서 공통 처리 |
| Network Error | Offline 감지 + 재시도 |

### 9.2 에러 처리 흐름

```
[Native 화면 에러]
  API 에러 → OkHttp Interceptor
    ├── 401 → refreshToken() → 성공: 재시도 / 실패: Auth 플로우 이동
    └── 기타 → ViewModel.uiState.error → Compose UI 에러 표시
```

---

## 10. 빌드 및 환경 설정

### 10.1 개발 환경

| 항목 | 설정 |
|------|------|
| Android Studio | Ladybug 이상 |
| JDK | Java 17 |
| Kotlin | 2.1.10 |
| Compose BOM | 2026.02.01 |
| Hilt | 2.57.1 (KSP) |
| Retrofit | 2.11.0 |
| Navigation Compose | 2.9.7 |
| Lifecycle | 2.9.0 |
| compileSdkVersion | 36 |
| targetSdkVersion | 36 |
| minSdkVersion | 28 (Android 9 Pie — Galaxy S10) |
| Target Device | Galaxy S10 (360 x 760dp) |

### 10.2 환경 변수 관리

**Native (Kotlin):**

```kotlin
object Constants {
    val API_BASE_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:8080"
    else
        "https://api.7itax.com"

    val AI_API_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:8000"
    else
        "https://ai.7itax.com"

    val WEBVIEW_BASE_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:3000"        // 로컬 React dev server
    else
        "https://7itax.com"            // Nginx에서 서빙되는 React SPA
}
```

**WebView (React):**

```bash
# .env.development (로컬 개발 시)
VITE_API_BASE_URL=http://localhost:8080
VITE_AI_API_URL=http://localhost:8000

# .env.production (Nginx 프록시 사용 — 상대 경로)
# baseURL 별도 설정 불필요, Nginx가 /api/ → Spring Boot, /ai/ → FastAPI로 프록시
```

### 10.3 WebView SPA 빌드 & 배포

WebView는 앱에 번들링하지 않고, **Nginx 서버에서 서빙**합니다. 앱의 WebView는 서버 URL을 로드합니다.

```
webView.loadUrl(Constants.WEBVIEW_BASE_URL)
// 프로덕션: "https://7itax.com"
// 개발: "http://10.0.2.2:3000"
```

**배포 흐름 (GitLab → Jenkins CI/CD):**

```
Developer → git push → GitLab → Webhook → Jenkins → Docker Build → EC2 배포
```

```bash
# Jenkins Pipeline 주요 단계

# 1. GitLab에서 Webhook 트리거 → Jenkins 자동 시작

# 2. React SPA 빌드
cd webview/
npm install && npm run build

# 3. Docker 이미지 빌드 (Nginx + 빌드 결과물 포함)
docker build -t 7itax-web .

# 4. Docker Compose로 전체 서비스 배포
docker-compose up -d
```

**Nginx 설정 예시:**

```nginx
server {
    listen 80;

    # React SPA 정적 파일 서빙
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;    # SPA fallback
    }

    # Spring Boot API 리버스 프록시
    location /api/ {
        proxy_pass http://spring-boot:8080;
    }

    # FastAPI AI 서비스 리버스 프록시
    location /ai/ {
        proxy_pass http://fastapi:8000;
    }
}
```

**번들링 대신 서버 호스팅을 선택한 이유:**
- WebView 화면 수정 시 앱 재배포 없이 서버만 배포하면 즉시 반영
- 7주 개발 기간 동안 프론트 수정 빈도가 높을 것으로 예상
- EC2 + Docker + Nginx 인프라가 이미 구축되어 있음

---

## 11. 코딩 컨벤션

### 11.1 Kotlin 명명 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 파일명 | PascalCase | LoginScreen.kt, AuthViewModel.kt |
| 클래스 | PascalCase | AuthViewModel, SecureStorage |
| 함수 | camelCase | verifyPin(), createPay(), registerCard() |
| Composable | PascalCase | TaxButton(), PinKeypad(), CardInputForm() |
| 상수 | SCREAMING_SNAKE | API_BASE_URL, MAX_PIN_LENGTH |
| 패키지 | lowercase | com.ssafy.seveniTax.ui.auth |

### 11.2 React (TypeScript) 명명 규칙

> 2차 개발 범위 (WebView 화면). 홈 화면 등 개발 시 적용.

### 11.3 Compose 파일 구조

```kotlin
// 1. package + imports
// 2. @Composable fun ScreenName(onBack, onNext, viewModel)
//    - val viewModel: XxxViewModel = hiltViewModel()
//    - val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//    - LaunchedEffect(Unit) { viewModel.loadData() }
//    - UI 구성 (Scaffold, Column 등)
// 3. @Preview
// 4. 내부 Composable (private)
```

### 11.4 React 파일 구조

> 2차 개발 범위.

---

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성자 | 박기택 (Frontend Developer) |
| 프로젝트 | 7iTAX — 프리랜서 개발자 세금 관리 앱 |
| 팀 | SSAFY 14기 광주 2반 C203 |
| 버전 | v1.0 (Initial) |
| 작성일 | 2025.07 |
