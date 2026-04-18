# Quick Task 260418-uhb: Add series data to ProductDTO from set mapping

**Completed:** 2026-04-18

## Problem
ProductDTO seriesId was always empty. Gallery page doesn't provide series data, but sets belong to series, and quicksearch/CSV import has the mapping.

## Solution
Derive series data from the set→series relationship in the database:

1. **Domain model:** Added `seriesId` and `seriesNames` fields to `ProductSet` data class
2. **ProductMapper:** Updated `toProductSet()` to extract series info from ProductSetEntity.series
3. **CollectablesMapper:** Updated to use `product.set?.seriesId` as fallback when `product.series` is null

## Files Modified

- `domain/model/Product.kt` - Added seriesId, seriesNames to ProductSet
- `adapter/out/persistence/mapper/ProductMapper.kt` - Extract series from set entity
- `adapter/inbound/rest/CollectablesMapper.kt` - Use set.seriesId as fallback

## Verification
- ✓ Compiles
- ✓ Detekt + Ktlint pass
- ✓ All tests pass

---

*Quick task complete: 2026-04-18*