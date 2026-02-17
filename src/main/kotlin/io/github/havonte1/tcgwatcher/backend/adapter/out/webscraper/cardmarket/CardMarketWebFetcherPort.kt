package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

interface CardMarketWebFetcherPort {
    fun fetch(searchString: String, locale: String, game: String): Result<String>
    fun fetchDetails(detailsUrl: String): Result<String>
}
