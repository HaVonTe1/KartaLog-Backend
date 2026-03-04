-- Data exported from quicksearch.db SQLite database
-- This file can be loaded into PostgreSQL watcher schema
-- Only records with non-null names and code are loaded

-- Series table
INSERT INTO watcher.qs_pokemon_series (id, name_de, name_en, name_fr)
SELECT id, name_de, name_en, name_fr
FROM quicksearch.qs_pokemon_series
WHERE name_de IS NOT NULL
  AND name_en IS NOT NULL
  AND name_fr IS NOT NULL;

-- Sets table
INSERT INTO watcher.qs_pokemon_sets (id, abbreviation, cm_product_id, code, name_de, name_en, name_fr, official, tcgp_id, total, series_id)
SELECT id, abbreviation, cm_product_id, code, name_de, name_en, name_fr, official, tcgp_id, total, series_id
FROM quicksearch.qs_pokemon_sets
WHERE name_de IS NOT NULL
  AND name_en IS NOT NULL
  AND name_fr IS NOT NULL
  AND code IS NOT NULL;

-- Cards table
INSERT INTO watcher.qs_pokemon_cards (id, cm_page_id, cm_product_id, code, name_de, name_en, name_fr, set_id, tcgp_id)
SELECT id, cm_page_id, cm_product_id, code, name_de, name_en, name_fr, set_id, tcgp_id
FROM quicksearch.qs_pokemon_cards
WHERE name_de IS NOT NULL
  AND name_en IS NOT NULL
  AND name_fr IS NOT NULL
  AND code IS NOT NULL;
