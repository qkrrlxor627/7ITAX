# Auth API 트러블슈팅

## 1. verify-identity 500 에러

### 증상
```
POST /api/auth/verify-identity
{"name":"홍길동","birthDate":"19900101","gender":"M","phoneNumber":"01012345678"}

→ 500 {"status":"fail","errorCode":"INTERNAL_SERVER_ERROR","message":"서버 내부 오류가 발생했습니다."}
```

### 원인
`birthDate` 형식 문제. BE `AuthService.java:60`에서 `LocalDate.parse(result.birthDate())`를 호출하는데, `LocalDate.parse()`는 ISO 형식(`yyyy-MM-dd`)만 파싱 가능.

- `19900101` → **500 에러** (파싱 실패)
- `1990-01-01` → **정상 동작**

### 해결
FE `AuthViewModel.kt:296`의 `buildBirthDate()` 함수에서 이미 ISO 형식으로 변환 중:
```kotlin
return "$century$yy-$mm-$dd"  // "1990-01-01"
```
FE 앱에서는 문제없음. curl 테스트 시 ISO 형식으로 보내야 함.

### 검증
```bash
curl -X POST https://j14c203.p.ssafy.io/api/auth/verify-identity \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"name":"홍길동","birthDate":"1990-01-01","gender":"M","phoneNumber":"01012345678"}'

→ 200 {"status":"success","data":{"userId":19,"isNewUser":true,"requiresPinSetup":true,"verifyToken":"..."}}
```

---

## 2. Invalid UTF-8 middle byte 에러

### 증상
BE 로그에 `Invalid UTF-8 middle byte 0xd7` 에러 발생.

### 원인
FE에서 API 요청 시 `Content-Type` 헤더에 charset이 명시되지 않아 한글 데이터 인코딩 문제 발생 가능.

### 해결
`ApiClient.kt`의 OkHttp Interceptor에 `Content-Type: application/json; charset=UTF-8` 헤더 명시 추가.

```kotlin
.addInterceptor { chain ->
    val requestBuilder = originalRequest.newBuilder()
        .header("Content-Type", "application/json; charset=UTF-8")
    // ...
}
```

### 커밋
`a182af5` - fix: API 요청에 Content-Type: application/json; charset=UTF-8 명시

---

## 3. test-login 비활성화

### 증상
```
POST /api/auth/test-login
→ 403 {"message":"테스트 로그인이 비활성화되어 있습니다."}
```

### 원인
BE `application.yml`에서 `app.test-login.enabled=false` 설정.

### 해결
BE 팀에 `app.test-login.enabled=true` 요청하거나, 실제 인증 플로우(verify-identity → setup-pin → login)로 진행.

---

## 4. SMS 인증 미연동

### 현재 상태
- BE에 `/api/sms/send`, `/api/sms/verify` 엔드포인트 존재
- FE `SmsVerificationScreen.kt:46`에서 `// TODO: 서버에 인증번호 검증 API 호출` 상태
- **6자리 아무 숫자 입력 시 검증 없이 통과** (목업)

### 인증 플로우 현황
| 단계 | API 연동 | 상태 |
|------|---------|------|
| 본인인증 정보 입력 | verify-identity | 실제 연동 |
| SMS 인증 | sms/send, sms/verify | **목업** |
| PIN 설정 | setup-pin | 실제 연동 |
| PIN 로그인 | login | 실제 연동 |

---

## 5. 전체 인증 플로우 테스트 (curl)

```bash
# 1. 본인인증
curl -X POST https://j14c203.p.ssafy.io/api/auth/verify-identity \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"name":"홍길동","birthDate":"1990-01-01","gender":"M","phoneNumber":"01012345678"}'
# → verifyToken 획득

# 2. PIN 설정
curl -X POST https://j14c203.p.ssafy.io/api/auth/setup-pin \
  -H "Content-Type: application/json; charset=UTF-8" \
  -H "X-Verify-Token: {verifyToken}" \
  -d '{"pin":"123456"}'
# → accessToken, refreshToken 획득

# 3. PIN 로그인
curl -X POST https://j14c203.p.ssafy.io/api/auth/login \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"phoneNumber":"01012345678","pin":"123456"}'
# → accessToken, refreshToken 획득

# 4. 인증 필요한 API 호출
curl https://j14c203.p.ssafy.io/api/book-entries \
  -H "Authorization: Bearer {accessToken}"
```

---

*작성일: 2026-03-26*
