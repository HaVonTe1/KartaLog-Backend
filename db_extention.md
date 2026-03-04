## Change Request: extend the database

### Summary

The app's database needs to be populated with the catalog data from all pokemon cards. The db schema was extended and adjusted. The data has been exported to SQL file for loading into PostgreSQL.

### Completed Work

#### 1. Liquibase Changelogs Created

- **20260303-add-series-sets-tables.xml**: Added `qs_pokemon_series`, `qs_pokemon_sets`, and `qs_pokemon_cards` tables to the `quicksearch` schema
- **20260303-extend-products-table.xml**: Extended the `products` table with `set_id` and `series_id` columns

#### 2. Product Entity Extended

- Added `setId: Long?` field to `ProductEntity`
- Added `seriesId: Long?` field to `ProductEntity`
- Added corresponding fields to `Product` domain model

#### 3. Product Mapper Updated

- Updated `ProductMapper.toEntity()` to map `setId` and `seriesId`
- Updated `ProductMapper.toDomain()` to map `setId` and `seriesId` from entity

#### 4. Data Export SQL Created

- Created `src/main/resources/db/import/quicksearch_data.sql` with INSERT statements for all three tables
- Only records with non-null names and code are loaded (incomplete data is skipped)

### Files Modified

| File | Change |
|------|--------|
| `src/main/resources/db/changelog/db.changelog-master.yaml` | Added new changelog includes |
| `src/main/resources/db/changelog/20260303-add-series-sets-tables.xml` | New file - series/sets/cards tables |
| `src/main/resources/db/changelog/20260303-extend-products-table.xml` | New file - extend products table |
| `src/main/resources/db/import/quicksearch_data.sql` | New file - data export SQL |
| `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Product.kt` | Added setId, seriesId fields |
| `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/ProductEntity.kt` | Added setId, seriesId fields |
| `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/mapper/ProductMapper.kt` | Updated mapper methods |

### Usage

To load the exported data into PostgreSQL:

```bash
psql -U postgres -d tcgwatcherdb -f src/main/resources/db/import/quicksearch_data.sql
```

### Build Verification

Project builds successfully with:
```bash
./gradlew build -x test -x integrationTest
```

### Test Results

All unit tests pass:
```bash
./gradlew test
```

**7 tests completed, 0 failed**
