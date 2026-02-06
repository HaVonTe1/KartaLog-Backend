package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import io.github.oshai.kotlinlogging.KotlinLogging

import org.springframework.stereotype.Component

/**
 * Playwright‑based implementation of {@link CardMarketScraperPort}.
 * Scrapes CardMarket product listings and maps them to {@link Product} domain objects.
 */
@Component
class CardMarketScraperAdapter(
    private val webFetcher: CardMarketWebFetcher = CardMarketWebFetcher(),
    private val contentParser: CardMarketContentParser = CardMarketContentParser()
) :
    CardMarketScraperPort {
    private val logger = KotlinLogging.logger {}

    /**
     * Executes a search on CardMarket and returns a list of products.
     * Only externalId, setName, rarity and imageUrl are populated; other fields remain null.
     */
    override fun search(searchString: String): List<Product> {
        logger.info { "Scraping CardMarket for $searchString" }

        val content = webFetcher.fetch(searchString)
        val products = contentParser.extractProductsFromHtml( content)
        return products
    }

}
