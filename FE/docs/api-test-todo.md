# FE API 테스트 TODO

## 인증 (Auth)
- [ ] POST /auth/verify-identity — 본인인증 (서버 500 해결 대기)
- [ ] POST /auth/setup-pin — PIN 설정
- [ ] POST /auth/login — PIN 로그인
- [ ] POST /auth/reissue — 토큰 재발급
- [ ] POST /auth/logout — 로그아웃 (Authorization 헤더 전달 확인)

## 카드 (Card)
- [ ] GET /cards/accounts — 계좌 목록 조회
- [ ] GET /cards/products — 카드 상품 목록 조회
- [ ] POST /cards — 카드 생성 (OTP 해결 대기)
- [ ] GET /cards — 카드 목록 조회
- [ ] GET /cards/{id} — 카드 상세 조회
- [ ] PATCH /cards/{id}/default — 기본 카드 변경
- [ ] DELETE /cards/{id} — 카드 삭제

## 간편장부 (BookEntry)
- [ ] GET /book-entries — 장부 목록 조회 (필터 파라미터 포함)
- [ ] GET /book-entries/{id} — 장부 상세 조회
- [ ] GET /book-entries/unconfirmed-count — 미확인 건수
- [ ] PATCH /book-entries/{id}/confirm — 거래 확인
- [ ] PATCH /book-entries/{id}/category — 카테고리 변경

## 세금 캘린더 (TaxCalendar)
- [x] GET /tax-calendar/deadlines — 일정 조회 ✅ 연동 완료

## 세금 추정 (TaxEstimation)
- [ ] GET /tax-estimation — 연간 세금 추정

## 경비 분류 (Classification)
- [ ] POST /classification — AI 경비 분류

## 내보내기 (Export)
- [ ] GET /export/book-entries — 장부 CSV 내보내기
- [ ] GET /export/vat — 부가세 CSV 내보내기
- [ ] GET /export/income-tax — 소득세 CSV 내보내기

## 계좌 (Banking)
- [ ] POST /banking/accounts — 계좌 생성
- [ ] GET /banking/accounts — 계좌 목록
- [ ] GET /banking/accounts/{id} — 계좌 상세
- [ ] GET /banking/accounts/{id}/balance — 잔액 조회

## 블로커
- **서버 500** — verify-identity 에러로 신규 가입 불가 → 토큰 발급 불가 → 대부분 API 테스트 불가
- **OTP** — SMS 비용 문제로 목업 중 → 카드 생성 테스트 불가

## 우선순위
1. 서버 500 해결 → 인증 테스트
2. 인증 통과 → 카드/장부/세금 API 테스트
3. OTP 해결 → 카드 생성 테스트

---

*작성일: 2026-03-26*
