package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
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
        logger.debug { "Searching for collectables with query='$searchString'" }

        val cached = searchResultRepository.findByQuery(searchString)
        if (cached != null) {
            // TTL of 1 hour
            val ttl = java.time.Duration.ofHours(1)
            val now = java.time.Instant.now()
            val cachedAt = cached.cachedAt
            if (cachedAt != null && cachedAt.isAfter(now.minus(ttl))) {
                logger.debug { "Cache hit (fresh) for query='$searchString' – returning ${cached.products.size} products" }
                return cached.products
            }
            // otherwise treat as miss and refresh
            logger.debug { "Cache stale for query='$searchString' – refreshing" }
        }

        // Cache miss or stale – invoke the scraper
        logger.debug { "Cache miss for query='$searchString' – invoking scraper" }
        // Run blocking call to suspend scraper
        val scraped: List<Product> = kotlinx.coroutines.runBlocking { scraperPort.search(searchString) }

        // Store the full search result for future calls, with cache timestamp
        val searchResult = SearchResult(query = searchString, products = scraped, cachedAt = java.time.Instant.now())
        searchResultRepository.save(searchResult)

        logger.debug { "Scrape completed and cached (${scraped.size} products) for query='$searchString'" }
        return scraped
    }
}
