package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType

interface CardMarketWebFetcherPort {
    suspend fun fetch(
        searchString: String,
        locale: Locale,
        genre: Genre,
        page: Int,
    ): Result<String>

    suspend fun fetchDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): Result<String>
}
