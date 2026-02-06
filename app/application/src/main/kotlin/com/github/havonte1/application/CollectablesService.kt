package com.github.havonte1.application

import org.springframework.stereotype.Service

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.sun.org.slf4j.internal.LoggerFactory
import io.github.oshai.kotlinlogging.KotlinLogging

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
        logger.info { "${"Searching CardMarket for '{}'"} $searchString" }
        return scraperPort.search(searchString)
    }

}
