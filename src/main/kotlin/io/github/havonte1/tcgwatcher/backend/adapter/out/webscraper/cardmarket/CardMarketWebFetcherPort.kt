package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

interface CardMarketWebFetcherPort {
    fun fetch(searchString: String, locale: String, game: String): Result<String>
    fun fetchDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): Result<String>
}
