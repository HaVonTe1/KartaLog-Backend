package com.github.havonte1.domain.port.out

import com.github.havonte1.domain.model.Product

/**
 * Outbound port for scraping CardMarket product listings.
 *
 * Implementations of this port are responsible for querying the CardMarket
 * service and returning domain [Product] objects that match the provided search string.
 */
interface CardMarketScraperPort {
    /**
     * Searches CardMarket for products that match the given [searchString].
     *
     * @param searchString the query used to search for products.
     * @return a list of matching [Product] instances; may be empty if no matches.
     */
    fun search(searchString: String): List<Product>
}
