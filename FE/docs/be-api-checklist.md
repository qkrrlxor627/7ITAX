# BE API 전체 체크리스트

BE 컨트롤러 기반 전체 API 목록. FE 연동 여부 + 테스트 상태.

## Auth (/api/auth)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | /verify-identity | 본인인증 | ✅ | ❌ 서버 500 |
| POST | /setup-pin | PIN 설정 | ✅ | ❌ |
| POST | /login | PIN 로그인 | ✅ | ❌ |
| POST | /reissue | 토큰 재발급 | ✅ | ❌ |
| POST | /logout | 로그아웃 | ✅ | ❌ |
| POST | /test-login | 테스트 로그인 | ❌ | ❌ 비활성화 |

## Card (/api/cards)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| GET | /merchants | 가맹점 목록 | ❌ | ❌ |
| GET | /accounts | 내 계좌 목록 | ✅ | ❌ |
| GET | /products | 카드 상품 목록 | ✅ | ❌ |
| POST | / | 카드 생성 | ✅ | ❌ OTP 대기 |
| GET | / | 카드 목록 | ✅ | ❌ |
| GET | /{cardId} | 카드 상세 | ✅ | ❌ |
| PATCH | /{cardId}/default | 기본 카드 변경 | ✅ | ❌ |
| DELETE | /{cardId} | 카드 삭제 | ✅ | ❌ |
| POST | /{cardId}/activate | 카드 활성화 | ❌ | ❌ |
| PATCH | /{cardId}/purpose | 카드 용도 변경 | ❌ | ❌ |
| POST | /{cardId}/payment | 카드 결제 | ❌ | ❌ |
| POST | /{cardId}/payment/cancel | 결제 취소 | ❌ | ❌ |
| GET | /{cardId}/transactions | 거래 내역 | ❌ | ❌ |
| GET | /{cardId}/billing | 청구서 | ❌ | ❌ |

## BookEntry (/api/book-entries)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | / | 거래 생성 | ✅ | ❌ |
| POST | /income | 수입 생성 | ❌ | ❌ |
| GET | /summary | 장부 요약 | ❌ | ❌ |
| GET | / | 장부 목록 | ✅ | ❌ |
| GET | /{entryId} | 거래 상세 | ✅ | ❌ |
| GET | /unconfirmed-count | 미확인 건수 | ✅ | ❌ |
| PATCH | /{entryId}/confirm | 거래 확인 | ✅ | ❌ |
| PATCH | /{entryId}/category | 카테고리 변경 | ✅ | ❌ |
| PATCH | /{entryId}/note | 메모 수정 | ❌ | ❌ |
| PATCH | /{entryId}/personal | 개인용 표시 | ✅ | ❌ |
| PATCH | /{entryId}/business | 사업용 표시 | ✅ | ❌ |

## Classification (/api/classification)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | / | AI 경비 분류 | ✅ | ❌ |

## TaxCalendar (/api/tax-calendar)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| GET | /deadlines | 세금 일정 | ✅ | ✅ 완료 |

## TaxEstimation (/api/tax-estimation)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| GET | / | 연간 세금 추정 | ✅ | ❌ |
| GET | /monthly | 월별 세금 추정 | ❌ | ❌ |

## Export (/api/export)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| GET | /book-entries | 장부 CSV | ✅ | ❌ |
| GET | /vat | 부가세 CSV | ✅ | ❌ |
| GET | /income-tax | 소득세 CSV | ✅ | ❌ |
| GET | /local-tax | 지방세 CSV | ❌ | ❌ |

## Banking (/api/banking/accounts)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | / | 계좌 생성 | ✅ | ❌ |
| GET | / | 계좌 목록 | ✅ | ❌ |
| GET | /{accountId} | 계좌 상세 | ✅ | ❌ |
| GET | /{accountId}/balance | 잔액 조회 | ✅ | ❌ |

## SMS (/api/sms)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | /send | OTP 발송 | ❌ | ❌ 비용 문제 |
| POST | /verify | OTP 검증 | ❌ | ❌ 비용 문제 |

## Payment (/api/payments)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | /authorize | 결제 승인 | ❌ | ❌ |
| POST | /{id}/capture | 결제 확정 | ❌ | ❌ |
| POST | /{id}/cancel | 결제 취소 | ❌ | ❌ |
| GET | /{id} | 결제 상세 | ❌ | ❌ |
| GET | / | 결제 목록 | ❌ | ❌ |
| POST | /qr | QR 결제 | ❌ | ❌ |
| POST | /qr/token | QR 토큰 생성 | ❌ | ❌ |
| GET | /qr/token/{token} | QR 정보 조회 | ❌ | ❌ |
| POST | /qr/token/{token}/confirm | QR 결제 확인 | ❌ | ❌ |
| GET | /qr/token/{token}/status | QR 상태 | ❌ | ❌ |
| GET | /qr/token/{token}/events | QR 이벤트 (SSE) | ❌ | ❌ |

## Tax (/api/tax)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | /calculate | 세금 계산 | ❌ | ❌ |
| GET | /savings | 절세 추천 | ❌ | ❌ |
| POST | /returns | 신고서 생성 | ❌ | ❌ |
| GET | /returns | 신고서 목록 | ❌ | ❌ |
| GET | /returns/{id} | 신고서 상세 | ❌ | ❌ |
| PUT | /returns/{id} | 신고서 수정 | ❌ | ❌ |
| POST | /returns/{id}/submit | 신고서 제출 | ❌ | ❌ |
| GET | /returns/{id}/payment-status | 납부 상태 | ❌ | ❌ |
| POST | /returns/{id}/pay | 국세 납부 | ❌ | ❌ |
| POST | /returns/{id}/pay-local | 지방세 납부 | ❌ | ❌ |
| GET | /returns/{id}/pdf | 신고서 PDF | ❌ | ❌ |
| GET | /returns/{id}/receipt | 영수증 PDF | ❌ | ❌ |

## VAT (/api/tax/vat-returns)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | / | 부가세 신고서 생성 | ❌ | ❌ |
| GET | / | 부가세 신고서 목록 | ❌ | ❌ |
| GET | /{id} | 부가세 신고서 상세 | ❌ | ❌ |
| POST | /{id}/submit | 부가세 신고서 제출 | ❌ | ❌ |

## Transfer (/api/transfers)
| 메서드 | 경로 | 설명 | FE 연동 | 테스트 |
|--------|------|------|:---:|:---:|
| POST | /p2p | P2P 송금 | ❌ | ❌ |
| POST | /withdraw | 출금 | ❌ | ❌ |
| GET | / | 송금 목록 | ❌ | ❌ |
| GET | /{transferId} | 송금 상세 | ❌ | ❌ |

---

## 요약

| 구분 | 총 API | FE 연동 | 테스트 완료 |
|------|:---:|:---:|:---:|
| Auth | 6 | 5 | 0 |
| Card | 14 | 8 | 0 |
| BookEntry | 11 | 8 | 0 |
| Classification | 1 | 1 | 0 |
| TaxCalendar | 1 | 1 | **1** |
| TaxEstimation | 2 | 1 | 0 |
| Export | 4 | 3 | 0 |
| Banking | 4 | 4 | 0 |
| SMS | 2 | 0 | 0 |
| Payment | 11 | 0 | 0 |
| Tax | 12 | 0 | 0 |
| VAT | 4 | 0 | 0 |
| Transfer | 4 | 0 | 0 |
| **합계** | **76** | **31** | **1** |

---

*작성일: 2026-03-26*
