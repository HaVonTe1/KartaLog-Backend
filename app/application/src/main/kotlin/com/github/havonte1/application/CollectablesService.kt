package com.github.havonte1.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort

/**
 * Spring service implementing the [SearchUseCase].
 * Delegates the search to the configured [CardMarketScraperPort] and returns the scraped products.
 */
@Service
class CollectablesService(
    private val scraperPort: CardMarketScraperPort
) : SearchUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /** Returns cards scraped from CardMarket for the given search string. */
    override fun search(searchString: String): List<Product> {
        logger.info("Searching CardMarket for '{}'", searchString)
        return scraperPort.search(searchString)
    }
}
}
