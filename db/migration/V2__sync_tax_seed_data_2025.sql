-- ============================================================
-- V2: 2025년 세금 기준 데이터 전체 동기화
-- 실행 대상: 기존 운영 DB (Docker init 이후 변경분 반영)
-- 멱등성 보장: 몇 번을 실행해도 동일 결과
-- ============================================================

BEGIN;

-- ============================================================
-- 1. tax_brackets (종합소득세 세율 구간) — 8개
-- 소득세법 §55①
-- ============================================================
DELETE FROM tax_brackets WHERE year = 2025;

INSERT INTO tax_brackets (bracket_max, bracket_min, progressive_deduction, rate, year) VALUES
(14000000,    0,          0,        0.06, 2025),
(50000000,    14000001,   1260000,  0.15, 2025),
(88000000,    50000001,   5760000,  0.24, 2025),
(150000000,   88000001,   15440000, 0.35, 2025),
(300000000,   150000001,  19940000, 0.38, 2025),
(500000000,   300000001,  25940000, 0.40, 2025),
(1000000000,  500000001,  35940000, 0.42, 2025),
(9999999999,  1000000001, 65940000, 0.45, 2025);

-- ============================================================
-- 2. tax_parameters (세금 정책 파라미터) — 21개
-- UPSERT: 있으면 UPDATE, 없으면 INSERT
-- ============================================================
INSERT INTO tax_parameters ("year", category, param_key, param_value, description, legal_basis, created_at, updated_at) VALUES
(2025, 'LOCAL_TAX',         'rate',              '0.10',        '지방소득세율',                                          '지방세법 §92',              NOW(), NOW()),
(2025, 'VAT',               'rate',              '0.10',        '부가가치세율',                                          '부가가치세법 §30',          NOW(), NOW()),
(2025, 'BASIC_DEDUCTION',   'personal',          '1500000',     '기본공제 본인',                                         '소득세법 §50',              NOW(), NOW()),
(2025, 'NORAN_DEDUCTION',   'threshold',         '40000000',    '노란우산공제 소득분기점 (4천만원)',                      '소기업소상공인공제부금법',   NOW(), NOW()),
(2025, 'NORAN_DEDUCTION',   'threshold_high',    '100000000',   '노란우산공제 소득분기점 (1억원)',                        '소기업소상공인공제부금법',   NOW(), NOW()),
(2025, 'NORAN_DEDUCTION',   'limit_low',         '5000000',     '사업소득 4천만 이하 공제한도',                           '소기업소상공인공제부금법',   NOW(), NOW()),
(2025, 'NORAN_DEDUCTION',   'limit_high',        '3000000',     '사업소득 4천만~1억 공제한도',                            '소기업소상공인공제부금법',   NOW(), NOW()),
(2025, 'NORAN_DEDUCTION',   'limit_top',         '2000000',     '사업소득 1억 초과 공제한도',                             '소기업소상공인공제부금법',   NOW(), NOW()),
(2025, 'PENSION_CREDIT',    'threshold',         '45000000',    '연금저축 소득분기점 (종합소득금액 기준, 사업소득자)',     '소득세법 §59조의3',         NOW(), NOW()),
(2025, 'PENSION_CREDIT',    'rate_low',          '0.15',        '소득 이하 세액공제율',                                  '소득세법 §59조의3',         NOW(), NOW()),
(2025, 'PENSION_CREDIT',    'rate_high',         '0.12',        '소득 초과 세액공제율',                                  '소득세법 §59조의3',         NOW(), NOW()),
(2025, 'PENSION_CREDIT',    'limit',             '6000000',     '연금저축 연간 한도',                                    '소득세법 §59조의3',         NOW(), NOW()),
(2025, 'SME_REDUCTION',     'rate',              '0.30',        '소기업 세액감면율 (지식기반산업)',                        '조특법 §7②',               NOW(), NOW()),
(2025, 'SME_REDUCTION',     'eligible_codes',    '72,62,63,58', '감면대상 업종코드 앞2자리 (SW,IT,정보통신,출판)',         '조특법 §7①',               NOW(), NOW()),
(2025, 'BOOKKEEPING_CREDIT','rate',              '0.20',        '간편장부 기장 세액공제율 (산출세액의 20%)',               '소득세법 §56조의2',         NOW(), NOW()),
(2025, 'BOOKKEEPING_CREDIT','limit',             '1000000',     '기장세액공제 한도 (연 100만원)',                          '소득세법 §56조의2',         NOW(), NOW()),
(2025, 'ENTERTAINMENT',     'annual_limit',      '24000000',    '접대비 연간 기본한도',                                   '소득세법 §35, 시행령 §78',  NOW(), NOW()),
(2025, 'ENTERTAINMENT',     'revenue_rate',      '0.002',       '접대비 수입금액 비례 추가한도율 (수입×0.2%)',             '소득세법 시행령 §79',       NOW(), NOW()),
(2025, 'VEHICLE',           'annual_limit',      '15000000',    '차량유지비 연간 한도',                                   '소득세법 §33의2',           NOW(), NOW()),
(2025, 'VEHICLE',           'no_insurance_ratio', '0.5',        '업무전용보험 미가입 시 업무사용비율 강제',                '소득세법 시행령 §78의3',    NOW(), NOW()),
(2025, 'EDUCATION',         'recommended_limit', '1500000',     '교육훈련비 추천한도',                                    '소득세법 §19',              NOW(), NOW())
ON CONFLICT ("year", category, param_key)
DO UPDATE SET
    param_value = EXCLUDED.param_value,
    description = EXCLUDED.description,
    legal_basis = EXCLUDED.legal_basis,
    updated_at  = NOW();

-- ============================================================
-- 3. tax_deadlines (세금 신고 기한) — 6개
-- ============================================================
INSERT INTO tax_deadlines ("year", name, description, deadline_month, deadline_day, created_at, updated_at) VALUES
(2025, '부가가치세 확정신고 (2기)', '7~12월 매출·매입 부가세 확정신고 및 납부',    1,  25, NOW(), NOW()),
(2025, '부가가치세 예정신고 (1기)', '1~3월 매출·매입 부가세 예정신고 및 납부',     4,  25, NOW(), NOW()),
(2025, '종합소득세 신고',           '전년도 사업소득 종합소득세 확정신고 및 납부',  5,  31, NOW(), NOW()),
(2025, '지방소득세 신고',           '종합소득세 기준 지방소득세 신고 및 납부',      5,  31, NOW(), NOW()),
(2025, '부가가치세 확정신고 (1기)', '1~6월 매출·매입 부가세 확정신고 및 납부',     7,  25, NOW(), NOW()),
(2025, '부가가치세 예정신고 (2기)', '7~9월 매출·매입 부가세 예정신고 및 납부',     10, 25, NOW(), NOW())
ON CONFLICT ("year", name)
DO UPDATE SET
    description    = EXCLUDED.description,
    deadline_month = EXCLUDED.deadline_month,
    deadline_day   = EXCLUDED.deadline_day,
    updated_at     = NOW();

-- ============================================================
-- 4. mcc_tax_rule — 접대비 MCC annual_limit 동기화
-- ============================================================
UPDATE mcc_tax_rule
   SET annual_limit = 24000000,
       updated_at   = NOW()
 WHERE tax_category = '접대비'
   AND annual_limit IS DISTINCT FROM 24000000;

-- ============================================================
-- 5. tax_limit — 접대비 연간기본한도 동기화
-- ============================================================
UPDATE tax_limit
   SET limit_amount  = 24000000,
       legal_basis   = '소득세법§35, 시행령§78(2,400만원)'
 WHERE tax_category  = '접대비'
   AND limit_type    = '연간기본한도'
   AND limit_amount IS DISTINCT FROM 24000000;

COMMIT;
