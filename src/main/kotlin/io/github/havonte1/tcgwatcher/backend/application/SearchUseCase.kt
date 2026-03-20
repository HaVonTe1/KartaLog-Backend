package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import java.time.Instant

interface SearchUseCase {

    suspend fun search(searchString: String, locale: String, game: String): List<Product>

    suspend fun fetchProductDetails(cmId: String, genre: String, type: String, lang: String, setname: String): Product?

    suspend fun getSearchCachedAt(searchString: String): Instant?

    suspend fun getProductUpdatedAt(cmId: String): Instant?
}
