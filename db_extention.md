# Change Request: extend the database - Quicksearch Import

## Summary

The app's database needs to be populated with the Pokemon catalog data from the SQLite `quicksearch.db` database. The data should be automatically imported into the existing PostgreSQL `watcher` schema tables (`series`, `product_set`, `products`) on application startup.

The import must be:
- **Automatic**: Runs on every app startup
- **Idempotent**: Skips import if data already exists (checked via `source_id` column)
- **Preserving original IDs**: SQLite TEXT IDs are preserved in a new `source_id` column

## Data Mapping

| SQLite Table | PostgreSQL Table | Notes |
|--------------|-----------------|-------|
| `qs_pokemon_series` | `series` + `product_name_translations` | 20 records |
| `qs_pokemon_sets` | `product_set` + `product_name_translations` | 192 records |
| `qs_pokemon_cards` | `products` + `product_name_translations` | 1,861 records |

### SQLite Schema (source data)

```sql
-- qs_pokemon_series
CREATE TABLE qs_pokemon_series (
    id TEXT not null,          -- e.g., "xy", "sv", "col"
    name_de TEXT not null,
    name_en TEXT not null,
    name_fr TEXT not null,
    primary key (id)
);

-- qs_pokemon_sets
CREATE TABLE qs_pokemon_sets (
    id TEXT not null,           -- e.g., "xy1", "sv1"
    abbreviation TEXT not null,
    cm_product_id TEXT not null,
    code TEXT not null,
    name_de TEXT not null,
    name_en TEXT not null,
    name_fr TEXT not null,
    official NUMBER not null,
    tcgp_id TEXT not null,
    total NUMBER not null,
    series_id TEXT not null,    -- foreign key to qs_pokemon_series.id
    primary key (id)
);

-- qs_pokemon_cards
CREATE TABLE qs_pokemon_cards (
    id TEXT not null,           -- e.g., "xy1-1"
    cm_page_id TEXT not null,
    cm_product_id TEXT not null,
    code TEXT not null,
    name_de TEXT not null,
    name_en TEXT not null,
    name_fr TEXT not null,
    set_id TEXT not null,       -- foreign key to qs_pokemon_sets.id
    tcgp_id TEXT not null,
    primary key (id)
);
```

## Implementation Plan

### Step 1: Database Schema Changes (Liquibase)

Add `source_id` VARCHAR column to preserve SQLite TEXT IDs while using auto-generated BIGINT keys:

| Table | New Column | Type | Purpose |
|-------|------------|------|---------|
| `watcher.series` | `source_id` | VARCHAR(50) | Store SQLite series ID (e.g., "xy", "sv") |
| `watcher.product_set` | `source_id` | VARCHAR(50) | Store SQLite set ID (e.g., "xy1", "sv1") |
| `watcher.products` | `source_id` | VARCHAR(50) | Store SQLite card ID (e.g., "xy1-1") |

**New file:** `src/main/resources/db/changelog/20260305-add-source-id-columns.xml`

### Step 2: Add SQLite JDBC Driver

Add SQLite JDBC driver to read the source database directly:

**File:** `build.gradle.kts`

```kotlin
// SQLite JDBC driver for reading quicksearch.db
implementation("org.xerial:sqlite-jdbc:3.45.1.0")
```

### Step 3: Create JPA Repositories

Create repository interfaces for accessing the new tables:

**New files:**
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/SeriesJpaRepository.kt`
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/ProductSetJpaRepository.kt`
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/NameTranslationJpaRepository.kt`

### Step 4: Create Import Runner

Create a Spring `ApplicationRunner` that imports data on startup:

**New file:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunner.kt`

**Behavior:**
- Implements `ApplicationRunner` with `@Order(Ordered.LOWEST_PRECEDENCE)`
- Runs after Liquibase migrations complete
- **Idempotency check**: Queries `source_id IS NOT NULL` to skip if data exists
- **Import Logic**:
  1. Open connection to SQLite: `jdbc:sqlite:quicksearch.db`
  2. **Series**: 
     - Read from `qs_pokemon_series`
     - Insert into `series` table (auto-generates BIGINT id)
     - Store original ID in `source_id` column
     - Insert 3 translations (de/en/fr) into `product_name_translations`
  3. **Sets**:
     - Read from `qs_pokemon_sets`
     - Map `series_id` TEXT → BIGINT using series source_id lookup
     - Insert into `product_set` table (auto-generates BIGINT id)
     - Store original ID in `source_id` column
     - Insert 3 translations into `product_name_translations`
  4. **Cards**:
     - Read from `qs_pokemon_cards`
     - Map `set_id` TEXT → BIGINT using set source_id lookup
     - Insert into `products` table (uses external_id, stores source_id)
     - Insert 3 translations into `product_name_translations`

### Step 5: Files Summary

| File | Change Type |
|------|-------------|
| `build.gradle.kts` | Modify - add sqlite-jdbc |
| `src/main/resources/db/changelog/20260305-add-source-id-columns.xml` | New - Liquibase changeset |
| `src/main/resources/db/changelog/db.changelog-master.yaml` | Modify - include new changeset |
| `src/main/kotlin/.../persistence/repository/SeriesJpaRepository.kt` | New |
| `src/main/kotlin/.../persistence/repository/ProductSetJpaRepository.kt` | New |
| `src/main/kotlin/.../persistence/repository/NameTranslationJpaRepository.kt` | New |
| `src/main/kotlin/.../persistence/QuicksearchImportRunner.kt` | New |

## Step 6: Integration Tests

Create integration tests to verify the import functionality:

**New file:** `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunnerIT.kt`

**Test Class:** `QuicksearchImportRunnerIT`

| Test Method | Description |
|-------------|-------------|
| `import runs on startup` | Verify import runner executes after Liquibase migrations |
| `series imported correctly` | Verify all 20 series are imported with correct source_id |
| `sets imported correctly` | Verify all 192 sets are imported, correctly linked to series |
| `cards imported correctly` | Verify cards are imported, correctly linked to sets |
| `translations imported correctly` | Verify de/en/fr translations exist for all entities |
| `import is idempotent` | Second startup does not duplicate data |
| `relationships preserved` | Verify series → sets → cards relationships work via JPA |

**Test Configuration:**
- Uses `@Testcontainers` with PostgreSQL
- Copies `quicksearch.db` to test resources or uses Spring's resource handling
- Uses `@DirtiesContext` to test fresh startup scenarios
- Tagged with `@Tag("integration")`

## Usage

After implementation, the quicksearch data is automatically imported on first startup:

```bash
# Start the application - data import happens automatically
./gradlew bootRun

# Or via Docker
docker compose up
```

**First startup:** Imports ~2,073 records (20 series + 192 sets + 1,861 cards + translations)

**Subsequent startups:** Skips import (idempotent) - no changes to database

## Verification

```bash
# Run all tests including integration tests
./gradlew check

# Run only integration tests
./gradlew test --tests "*IT"

# Verify database contents
psql -U postgres -d tcgwatcherdb -c "SELECT COUNT(*) FROM watcher.series;"
psql -U postgres -d tcgwatcherdb -c "SELECT COUNT(*) FROM watcher.product_set;"
psql -U postgres -d tcgwatcherdb -c "SELECT COUNT(*) FROM watcher.products;"
```

## Build Verification

Project builds successfully with:
```bash
./gradlew build -x test -x integrationTest
```

All tests pass:
```bash
./gradlew check
```

**Expected test results:** All tests pass including new integration tests

## Previous Work (Obsolete)

The previous implementation attempt created:
- `src/main/resources/db/import/quicksearch_data.sql` - Manual SQL import (not used)
- `db_extention.md` (old version) - Referenced non-existent changelogs

These files are now obsolete and can be deleted after the new implementation is complete.
