package com.github.havonte1.adapter.out.webscraper.cardmarket

/**
 * Abstraction for fetching raw HTML from CardMarket.
 * Allows injection of test implementations.
 */
interface CardMarketWebFetcherPort {
    /**
     * Returns the raw HTML content for the given search string.
     */
    fun fetch(searchString: String): String
}
