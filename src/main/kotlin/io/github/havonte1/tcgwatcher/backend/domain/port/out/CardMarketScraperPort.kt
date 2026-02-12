package io.github.havonte1.tcgwatcher.backend.domain.port.out

import io.github.havonte1.tcgwatcher.backend.domain.model.Product

/**
 * Outbound port for scraping CardMarket product listings.
 *
 * Implementations of this port are responsible for querying the CardMarket
 * service and returning domain [Product] objects that match the provided search string.
 */
interface CardMarketScraperPort {
    /**
     * Searches CardMarket for products that match the given [searchString] within the specified locale and game.
     * If [locale] or [game] are null, defaults are used (environment variables or "de"/"Pokemon").
     */
    suspend fun search(searchString: String, locale: String, game: String): List<Product>


}
