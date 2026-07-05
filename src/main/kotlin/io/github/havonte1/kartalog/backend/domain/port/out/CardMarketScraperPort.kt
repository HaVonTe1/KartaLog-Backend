package io.github.havonte1.kartalog.backend.domain.port.out

import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.Product
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.havonte1.kartalog.backend.domain.model.SearchResult

interface CardMarketScraperPort {
    suspend fun search(
        searchString: String,
        locale: Locale,
        genre: Genre,
    ): SearchResult

    suspend fun fetchProductDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): Product?
}
