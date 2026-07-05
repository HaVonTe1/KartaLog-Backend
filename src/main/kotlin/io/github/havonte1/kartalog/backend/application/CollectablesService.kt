package io.github.havonte1.kartalog.backend.application

import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.Product
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.havonte1.kartalog.backend.domain.model.SearchResult
import io.github.havonte1.kartalog.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.kartalog.backend.domain.port.out.ProductRepository
import io.github.havonte1.kartalog.backend.domain.port.out.SearchResultRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

private const val DEFAULT_PAGE_LIMIT = 30

@Service
class CollectablesService(
    private val scraper: CardMarketScraperPort,
    private val searchResultRepository: SearchResultRepository,
    private val productRepository: ProductRepository,
) : SearchUseCase {
    private val logger = KotlinLogging.logger {}

    @Cacheable("listCache")
    override suspend fun search(
        searchString: String,
        locale: Locale,
        genre: Genre,
    ): SearchResponse {
        logger.debug {
            "Searching for collectables with " +
                "query='$searchString' locale=$locale genre=$genre  -- no cache hit" }

        val searchResult: SearchResult =
            scraper.search(searchString, locale, genre)

        val savedResult = searchResultRepository.save(searchResult)

        val productCount = savedResult.products.size
        logger.debug { "Scrape completed and cached ($productCount products) for query='$searchString'" }
        return SearchResponse(
            products = savedResult.products,
            cachedAt = savedResult.cachedAt?: Instant.now()
        )
    }

    @Cacheable("detailsCache")
    override suspend fun fetchProductDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): Product? {
        logger.info { "Fetching product details for cmId=$cmId" }

        val existingProduct = productRepository.findByCmId(cmId)

        val actualSetName =
            setname.ifBlank {
                if (existingProduct != null && !existingProduct.set?.cmCode.isNullOrBlank()) {
                    existingProduct.set.cmCode
                } else {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "no setname provided")
                }
            }

        if (existingProduct != null) {
            val newProduct = scraper.fetchProductDetails(cmId, genre, type, locale, actualSetName)

            if (newProduct != null && hasChanges(existingProduct, newProduct)) {
                logger.debug { "Changes detected, updating product" }
                return productRepository.save(newProduct)
            } else {
                logger.debug { "No changes detected, returning cached product" }
                return existingProduct
            }
        } else {
            val newProduct = scraper.fetchProductDetails(cmId, genre, type, locale, actualSetName)
            if (newProduct != null) {
                logger.debug { "New product, persisting to database" }
                return productRepository.save(newProduct)
            }
        }

        return existingProduct
    }

    override suspend fun getSearchCachedAt(searchString: String, locale: Locale, genre: Genre): Instant? =
        searchResultRepository.getCachedAtByQueryLocaleAndGenre(
            query = searchString, language = locale.code, genre = genre.identifier)

    override suspend fun getProductUpdatedAt(cmId: String): Instant? =
        productRepository.findByCmId(cmId)?.updatedAt

    private fun hasChanges(
        oldProduct: Product,
        newProduct: Product,
    ): Boolean {
        if (oldProduct.price != newProduct.price) return true
        if (oldProduct.releaseDate != newProduct.releaseDate) return true
        if (oldProduct.cardNumber != newProduct.cardNumber) return true

        val oldLanguagePricing = oldProduct.languagePricing.associate { it.locale.code to it }
        val newLanguagePricing = newProduct.languagePricing.associate { it.locale.code to it }
        if (oldLanguagePricing.keys != newLanguagePricing.keys) return true
        oldLanguagePricing.forEach { (_, oldPricing) ->
            val newPricing = newLanguagePricing[oldPricing.locale.code]
            val priceChanged = oldPricing.price != newPricing?.price
            val trendChanged = oldPricing.priceTrend != newPricing?.priceTrend
            if (newPricing == null || priceChanged || trendChanged) {
                return true
            }
        }

        val oldAttributes = oldProduct.productAttributes.associate { it.attributeName to it }
        val newAttributes = newProduct.productAttributes.associate { it.attributeName to it }
        if (oldAttributes.keys != newAttributes.keys) return true
        oldAttributes.forEach { (_, oldAttr) ->
            val newAttr = newAttributes[oldAttr.attributeName]
            if (newAttr == null || oldAttr.value != newAttr.value) {
                return true
            }
        }

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
