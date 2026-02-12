package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Playwright‑based implementation of {@link CardMarketScraperPort}.
 * Scrapes CardMarket product listings and maps them to {@link Product} domain objects.
 */
@Component
class CardMarketScraperAdapter(
    private val webFetcher: CardMarketWebFetcherPort = CardMarketWebFetcher(),
    private val contentParser: CardMarketContentParser = CardMarketContentParser(),
    private val mapper: CardMarketProductMapper = CardMarketProductMapper()
) :
    CardMarketScraperPort {
    private val logger = KotlinLogging.logger {}

    override suspend fun search(
        searchString: String,
        locale: String,
        game: String
    ): List<Product> {
        logger.info { "Scraping CardMarket for $searchString" }

        val content = webFetcher.fetch(searchString, locale, game)
        val result = contentParser.extractProductsFromHtml(content)
        return mapper.toProducts(result)
    }
}
