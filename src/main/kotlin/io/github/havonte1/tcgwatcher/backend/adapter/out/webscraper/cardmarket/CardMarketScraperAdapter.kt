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
    private val webFetcher: CardMarketWebFetcherPort,
    private val contentParser: CardMarketContentParser = CardMarketContentParser(),
    private val mapper: CardMarketProductMapper = CardMarketProductMapper(),
) : CardMarketScraperPort {
    private val logger = KotlinLogging.logger {}

    override suspend fun search(
        searchString: String,
        locale: String,
        game: String,
    ): List<Product> {
        logger.info { "Scraping CardMarket for $searchString" }

        val fetchResult = webFetcher.fetch(searchString, locale, game)
        val content =
            fetchResult.getOrElse {
                logger.warn { "Failed to fetch CardMarket page: ${it.message}" }
                return emptyList()
            }
        val result = contentParser.parseGalaryPage(content)
        return mapper.toProducts(result)
    }

    override suspend fun fetchProductDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String,
    ): Product? {
        logger.info { "Fetching product details for $cmId" }

        val fetchResult = webFetcher.fetchDetails(cmId, genre, type, lang, setname)
        val content =
            fetchResult.getOrElse {
                logger.warn { "Failed to fetch CardMarket detail page: ${it.message}" }
                return null
            }

        val detailsDto = contentParser.parseProductDetails(content, cmId, genre, type, lang, setname)
        return mapper.toProductDetails(detailsDto)
    }
}
