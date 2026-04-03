package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import java.time.Instant

data class SearchResponse(
    val products: List<Product>,
    val cachedAt: Instant
)

interface SearchUseCase {
    suspend fun search(
        searchString: String,
        locale: Locale,
        genre: Genre,
    ): SearchResponse

    suspend fun fetchProductDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): Product?

    suspend fun getSearchCachedAt(searchString: String, locale: Locale, genre: Genre): Instant?

    suspend fun getProductUpdatedAt(cmId: String): Instant?
}
