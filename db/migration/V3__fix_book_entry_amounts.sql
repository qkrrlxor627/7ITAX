-- 간편장부 금액 수정: 공급가액(supply_price) → 원래 결제금액(supply_price + vat_amount)
-- 기존 데이터는 부가세 차감된 공급가액이 저장되어 있어 원래 금액으로 복원

UPDATE book_entries SET income_amount = supply_price + vat_amount WHERE entry_type = 'INCOME';
UPDATE book_entries SET expense_amount = supply_price + vat_amount WHERE entry_type = 'EXPENSE';
UPDATE book_entries SET fixed_asset_amount = supply_price + vat_amount WHERE entry_type = 'ASSET';
