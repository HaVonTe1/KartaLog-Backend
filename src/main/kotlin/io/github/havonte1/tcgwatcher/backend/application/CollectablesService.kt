package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.config.CacheConfig
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.withTimeout
import org.springframework.stereotype.Service

/**
 * Spring service implementing the [SearchUseCase].
 * Delegates the search to the configured [CardMarketScraperPort] and returns the scraped products.
 */
@Service
class CollectablesService(
    private val scraperPort: CardMarketScraperPort,
    private val searchResultRepository: SearchResultRepository,
    private val productRepository: ProductRepository,
    private val cacheConfig: CacheConfig
) : SearchUseCase {

    private val logger = KotlinLogging.logger {}

    /** Returns cards for the given query, using cached results when available. */
    override suspend fun search(searchString: String, locale: String, game: String): List<Product> {
        logger.debug { "Searching for collectables with query='$searchString'" }

        val cached = searchResultRepository.findByQuery(searchString)
        if (cached != null) {
            val ttl = java.time.Duration.ofHours(cacheConfig.ttlHours)
            val now = java.time.Instant.now()
            val cachedAt = cached.cachedAt
            if (cachedAt != null && now.isBefore(cachedAt.plus(ttl))) {
                logger.debug {
                    "Cache hit (fresh) for query='$searchString' – returning ${cached.products.size} products"
                }
                return cached.products
            }
            logger.debug { "Cache stale for query='$searchString' – refreshing" }
        }

        // Cache miss or stale – invoke the scraper
        logger.debug { "Cache miss for query='$searchString' – invoking scraper" }
        // Run blocking call to suspend scraper
        val scraped: List<Product> = withTimeout(30_000) {
            scraperPort.search(searchString, locale, game)
        }

        // Store the full search result for future calls, with cache timestamp
        val searchResult = SearchResult(query = searchString, products = scraped, cachedAt = java.time.Instant.now())

        // Persist synchronously to ensure caching works reliably
        searchResultRepository.save(searchResult)

        logger.debug { "Scrape completed and cached (${scraped.size} products) for query='$searchString'" }
        return scraped
    }


    override suspend fun fetchProductDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): Product? {
        logger.info { "Fetching product details for cmId=$cmId" }

        val existingProduct =
            productRepository.findByCmId(cmId)

        if (existingProduct != null) {
            val newProduct = scraperPort.fetchProductDetails(cmId, genre, type, lang, setname)

            if (newProduct != null && hasChanges(existingProduct, newProduct)) {
                logger.debug { "Changes detected, updating product" }
                return productRepository.save(newProduct)
            } else {
                logger.debug { "No changes detected, returning cached product" }
                return existingProduct
            }
        } else {
            val newProduct = scraperPort.fetchProductDetails(cmId, genre, type, lang, setname)
            if (newProduct != null) {
                logger.debug { "New product, persisting to database" }
                return productRepository.save(newProduct)
            }
        }

        return null
    }

    private fun hasChanges(oldProduct: Product, newProduct: Product): Boolean {
        if (oldProduct.price != newProduct.price) return true
        if (oldProduct.rarity != newProduct.rarity) return true

        val oldSellOffersMap = oldProduct.sellOffers?.associate { it.sellerName to it } ?: emptyMap()
        val newSellOffersMap = newProduct.sellOffers?.associate { it.sellerName to it } ?: emptyMap()

        if (oldSellOffersMap.keys != newSellOffersMap.keys) return true

        oldSellOffersMap.forEach { (name, oldOffer) ->
            val newOffer = newSellOffersMap[name]
            if (newOffer == null || oldOffer.price != newOffer.price || oldOffer.amount != newOffer.amount) {
                return true
            }
        }

        return false
    }
}
