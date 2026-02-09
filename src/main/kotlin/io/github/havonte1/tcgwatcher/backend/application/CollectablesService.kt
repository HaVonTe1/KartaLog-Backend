package io.github.havonte1.tcgwatcher.backend.application


import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Spring service implementing the [SearchUseCase].
 * Delegates the search to the configured [CardMarketScraperPort] and returns the scraped products.
 */
@Service
class CollectablesService(
    private val scraperPort: CardMarketScraperPort
) : SearchUseCase {

    private val logger = KotlinLogging.logger {}

    /** Returns cards scraped from CardMarket for the given search string. */
    override fun search(searchString: String): List<Product> {
        logger.info { "Searching CardMarket for '$searchString'" }
        return scraperPort.search(searchString)
    }
}
