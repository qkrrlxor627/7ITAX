# 71TAX ERD (Entity Relationship Diagram)

> **DB**: PostgreSQL | **DDL**: Hibernate auto-update | **PII 암호화**: AES (name, phoneNumber, businessRegNumber)

---

## 테이블 관계도

```
users (1) ─── (*) user_consents
users (1) ─── (1) business_profiles
users (1) ─── (*) cards
users (1) ─── (*) payments
users (1) ─── (*) transfers (sender)
users (1) ─── (*) transfers (receiver)
cards (1) ─── (*) card_transactions
cards (1) ─── (*) payments
cards (1) ─── (*) transfers (sender_card / receiver_card)
payments (1) ─── (0..1) book_entries
```

---

## 1. users

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| ci | VARCHAR(128) | UNIQUE, NOT NULL | 암호화된 CI |
| di | VARCHAR(128) | UNIQUE, NOT NULL | 암호화된 DI |
| name | VARCHAR(512) | | AES 암호화 |
| birth_date | DATE | | |
| gender | VARCHAR | | |
| phone_number | VARCHAR(512) | | AES 암호화 |
| phone_last4 | VARCHAR(4) | | 검색용 |
| pin_hash | VARCHAR(200) | | 해시된 PIN |
| biometric_enabled | BOOLEAN | NOT NULL, default: false | |
| device_id | VARCHAR | | |
| kyc_verified | BOOLEAN | NOT NULL, default: false | |
| is_business | BOOLEAN | NOT NULL, default: false | |
| status | VARCHAR | NOT NULL | ACTIVE / SUSPENDED / WITHDRAWN |
| ssafy_user_key | VARCHAR(100) | | SSAFY 금융 API 키 |
| last_login_at | DATETIME | | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**인덱스**: `idx_user_phone_last4` (phone_last4)

---

## 2. user_consents

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| user_id | BIGINT | FK -> users, NOT NULL | |
| consent_type | VARCHAR | NOT NULL | SERVICE / PRIVACY / FINANCIAL |
| consented | BOOLEAN | NOT NULL | |
| consented_at | DATETIME | | |
| revoked_at | DATETIME | | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**UNIQUE**: (user_id, consent_type)

---

## 3. business_profiles

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| user_id | BIGINT | FK -> users, UNIQUE | 1:1 관계 |
| business_reg_number | VARCHAR(512) | | AES 암호화 |
| business_name | VARCHAR | | |
| industry_code | VARCHAR | | |
| nts_verified | BOOLEAN | NOT NULL, default: false | 국세청 인증 |
| nts_verified_at | DATETIME | | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

## 4. cards

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| user_id | BIGINT | FK -> users, NOT NULL | |
| card_name | VARCHAR | NOT NULL | |
| card_type | VARCHAR | NOT NULL | PERSONAL / BUSINESS |
| last4_digits | VARCHAR(4) | NOT NULL | |
| is_default | BOOLEAN | NOT NULL, default: false | |
| ssafy_account_no | VARCHAR | NOT NULL | SSAFY 계좌번호 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**인덱스**: `idx_card_user_id` (user_id)

---

## 5. card_transactions

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| card_id | BIGINT | FK -> cards, NOT NULL | |
| transaction_type | VARCHAR | NOT NULL | CHARGE / PAYMENT / REFUND |
| amount | BIGINT | NOT NULL | |
| balance_after | BIGINT | NOT NULL | |
| description | VARCHAR | | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

## 6. payments

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| user_id | BIGINT | FK -> users, NOT NULL | |
| card_id | BIGINT | FK -> cards, NOT NULL | |
| amount | BIGINT | NOT NULL | |
| currency | VARCHAR | NOT NULL | |
| merchant_name | VARCHAR | NOT NULL | |
| merchant_category_code | VARCHAR | | MCC 코드 |
| payment_method | VARCHAR | NOT NULL | ONLINE / OFFLINE |
| purpose | VARCHAR | NOT NULL | PERSONAL / BUSINESS |
| status | VARCHAR | NOT NULL | AUTHORIZED / CAPTURED / CANCELLED / DECLINED |
| authorization_code | VARCHAR | | |
| cancelled_amount | BIGINT | | |
| cancel_reason | VARCHAR | | |
| authorized_at | DATETIME | | |
| captured_at | DATETIME | | |
| cancelled_at | DATETIME | | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**인덱스**: `idx_payment_user_id`, `idx_payment_card_id`, `idx_payment_status`, `idx_payment_user_status`

---

## 7. book_entries

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| user_id | BIGINT | NOT NULL | FK 제약 없음 (직접 ID) |
| payment_id | BIGINT | UNIQUE | payments 연결 |
| entry_date | DATE | NOT NULL | |
| description | VARCHAR(500) | | |
| merchant_name | VARCHAR(200) | | |
| entry_type | VARCHAR(10) | NOT NULL | INCOME / EXPENSE / ASSET |
| income_amount | BIGINT | NOT NULL, default: 0 | |
| expense_amount | BIGINT | NOT NULL, default: 0 | |
| fixed_asset_amount | BIGINT | NOT NULL, default: 0 | |
| vat_amount | BIGINT | NOT NULL, default: 0 | |
| supply_price | BIGINT | NOT NULL, default: 0 | |
| category_code | VARCHAR(10) | | 세목 코드 |
| category_name | VARCHAR(50) | | 세목 이름 |
| is_business_expense | BOOLEAN | NOT NULL, default: true | |
| is_vat_deductible | BOOLEAN | NOT NULL, default: true | |
| confirmed | BOOLEAN | NOT NULL, default: false | |
| confirmed_at | DATETIME | | |
| note | VARCHAR(500) | | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**인덱스**: `idx_be_user_date` (user_id, entry_date DESC), `idx_be_user_confirmed` (user_id, confirmed)

---

## 8. transfers

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| sender_user_id | BIGINT | FK -> users, NOT NULL | |
| receiver_user_id | BIGINT | FK -> users | nullable (출금 시) |
| sender_card_id | BIGINT | FK -> cards, NOT NULL | |
| receiver_card_id | BIGINT | FK -> cards | nullable |
| transfer_type | VARCHAR | NOT NULL | P2P / WITHDRAW |
| amount | BIGINT | NOT NULL | |
| status | VARCHAR | NOT NULL | COMPLETED / FAILED |
| description | VARCHAR | | |
| target_account_no | VARCHAR | | 외부 계좌 (출금 시) |
| ssafy_transaction_unique_no | VARCHAR | | SSAFY 거래번호 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**인덱스**: `idx_transfer_sender_user`, `idx_transfer_receiver_user`

---

## 9. merchant (가맹점 마스터)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| merchant_name | VARCHAR(200) | NOT NULL | |
| mcc | VARCHAR(10) | NOT NULL | 가맹점 분류 코드 |
| category | VARCHAR(100) | | |
| example_names | TEXT | | |
| is_domestic | BOOLEAN | NOT NULL, default: true | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**인덱스**: `idx_merchant_mcc`, `idx_merchant_name`

---

## 10. mcc_tax_rule (세목 분류 규칙)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| mcc | VARCHAR(10) | NOT NULL | |
| tier | VARCHAR(1) | NOT NULL | A=자동, B=조건부 |
| condition_expr | TEXT | | 조건식 |
| tax_category | VARCHAR(50) | NOT NULL | |
| vat_deductible | VARCHAR(30) | NOT NULL | |
| amount_threshold | BIGINT | | 금액 기준 |
| remark | TEXT | | |
| legal_basis | VARCHAR(200) | | 법적 근거 |
| annual_limit | BIGINT | | 연간 한도 |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**인덱스**: `idx_mcc_tax_rule_mcc`, `idx_mcc_tax_rule_tier`

---

## 11. merchant_keyword_mapping (키워드 분류)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | |
| keyword | VARCHAR(100) | NOT NULL | |
| mcc | VARCHAR(10) | NOT NULL, default: "5817" | |
| tax_category | VARCHAR(50) | NOT NULL | |
| is_domestic | BOOLEAN | NOT NULL, default: true | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**인덱스**: `idx_mkm_keyword`, `idx_mkm_tax_category`

---

## ENUM 값 정리

| ENUM | 값 |
|------|-----|
| UserStatus | ACTIVE, SUSPENDED, WITHDRAWN |
| ConsentType | SERVICE, PRIVACY, FINANCIAL |
| CardType | PERSONAL, BUSINESS |
| CardTransactionType | CHARGE, PAYMENT, REFUND |
| PaymentMethod | ONLINE, OFFLINE |
| PaymentPurpose | PERSONAL, BUSINESS |
| PaymentStatus | AUTHORIZED, CAPTURED, CANCELLED, DECLINED |
| TransferType | P2P, WITHDRAW |
| TransferStatus | COMPLETED, FAILED |
| EntryType | INCOME, EXPENSE, ASSET |
