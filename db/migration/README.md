# DB 마이그레이션 적용 전략

이 디렉토리의 `V*.sql`은 **Flyway 네이밍이지만 이 프로젝트에는 Flyway/Liquibase가 없다.**
따라서 이 파일들은 자동으로 실행되지 않는다. 대신 아래 방식으로 그 내용이 적용된다.

## 스키마·시드 부트스트랩

- **신규 볼륨(`docker compose up`)**: PostgreSQL이 `docker-entrypoint-initdb.d`에 마운트된
  `db/init/01_schema.sql`(스키마) + `db/init/02_seed_data.sql`(시드)을 최초 1회 실행한다.
- **엔티티/컬럼**: JPA `ddl-auto=update`가 신규 컬럼(잔액·원장·`pay_pin_hash` 등)을 자동 생성한다.

## `V*.sql`의 실제 적용 경로

| 파일 | 내용 | 자동 적용 방식 |
|---|---|---|
| `V2__sync_tax_seed_data_2025.sql` | 2025년 세금 기준 데이터(세율구간·파라미터·기한 등) | `db/init/02_seed_data.sql`에 동일 데이터가 포함되어 신규 볼륨에서 적용되고, 런타임에 `TaxBracketDataInitializer`·`TaxParameterDataInitializer`·`TaxDeadlineDataInitializer`(모두 멱등)가 매 기동 시 보장한다. |
| `V3__fix_book_entry_amounts.sql` | 간편장부 금액을 원 결제금액(`supply_price + vat_amount`)으로 정규화 | `BookEntryAmountBackfillInitializer`(`config/`)가 매 기동 시 **멱등 백필**한다. 이미 정규화된 행(`= supply_price + vat_amount`)은 건드리지 않아 신규 DB에서는 no-op, 레거시 행만 1회 보정된다. |

즉 두 마이그레이션의 내용은 신규 볼륨과 기존/지속 볼륨 모두에서 런타임에 자동 반영된다.

## 레거시 운영 DB에 수동 적용하려면(선택)

런타임 백필/시드 이니셜라이저로 충분하지만, SQL을 직접 적용하려면:

```bash
# compose의 postgres 컨테이너에 직접 실행
docker exec -i tax7i-postgres psql -U ssafy -d tax7i < db/migration/V2__sync_tax_seed_data_2025.sql
docker exec -i tax7i-postgres psql -U ssafy -d tax7i < db/migration/V3__fix_book_entry_amounts.sql
```

두 스크립트 모두 멱등이라 여러 번 실행해도 안전하다.

## 향후 Flyway 도입 시

현재는 `db/init`(initdb) + `ddl-auto=update` + 멱등 이니셜라이저 조합을 쓴다. 정식 마이그레이션 이력
관리가 필요하면 Flyway를 도입하되, `01_schema.sql`을 `V1__`로 승격하고 `ddl-auto=validate`로 전환하는
스키마 관리 일원화가 선행되어야 한다(현재 하이브리드와의 충돌 방지).
