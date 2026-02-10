package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Spring service implementing the [SearchUseCase].
 * Delegates the search to the configured [CardMarketScraperPort] and returns the scraped products.
 */
@Service
class CollectablesService(
    private val scraperPort: CardMarketScraperPort,
    private val searchResultRepository: SearchResultRepository
) : SearchUseCase {

    private val logger = KotlinLogging.logger {}

    /** Returns cards for the given query, using cached results when available. */
    override fun search(searchString: String): List<Product> {
        logger.info { "Searching for collectables with query='$searchString'" }

        val cached = searchResultRepository.findByQuery(searchString)
        if (cached != null) {

            logger.debug { "Cache hit for query='$searchString' – returning ${cached.products.size} products" }

            //TODO: check the lastUpdated timestamp - if its older than X than do the webscraping and update the products
            return cached.products
        }

        // 2️⃣ Cache miss – invoke the scraper
        logger.debug { "Cache miss for query='$searchString' – invoking scraper" }
        val scraped: List<Product> = scraperPort.search(searchString)

        // Store the full search result for future calls
        val searchResult = SearchResult(query = searchString, products = scraped)
        searchResultRepository.save(searchResult)

        logger.info { "Scrape completed and cached (${scraped.size} products) for query='$searchString'" }
        return scraped
    }
}
