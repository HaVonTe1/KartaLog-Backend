package io.github.havonte1.tcgwatcher.backend.domain.port.out

import io.github.havonte1.tcgwatcher.backend.domain.model.Product

interface CardMarketScraperPort {
    suspend fun search(
        searchString: String,
        locale: String,
        game: String,
    ): List<Product>

    suspend fun fetchProductDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String,
    ): Product?
}
