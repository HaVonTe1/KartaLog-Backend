package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductDetailsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

interface ProductDetailsUseCase {
    suspend fun fetchProductDetails(cmId: String, genre: String, type: String, lang: String, setname: String): Product?
}

@Service
class ProductDetailsServiceImpl(
    private val scraperPort: CardMarketScraperPort,
    private val productDetailsRepository: ProductDetailsRepository
) : ProductDetailsUseCase {
    
    private val logger = KotlinLogging.logger {}
    
    override suspend fun fetchProductDetails(cmId: String, genre: String, type: String, lang: String, setname: String): Product? {
        logger.info { "Fetching product details for cmId=$cmId" }
        
        val existingProduct = productDetailsRepository.findByCmIdAndGenreAndTypeAndLangAndSetname(cmId, genre, type, lang, setname)
        
        if (existingProduct != null) {
            val newProduct = scraperPort.fetchProductDetails(cmId, genre, type, lang, setname)
            
            if (newProduct != null && hasChanges(existingProduct, newProduct)) {
                logger.debug { "Changes detected, updating product" }
                return productDetailsRepository.save(newProduct)
            } else {
                logger.debug { "No changes detected, returning cached product" }
                return existingProduct
            }
        } else {
            val newProduct = scraperPort.fetchProductDetails(cmId, genre, type, lang, setname)
            if (newProduct != null) {
                logger.debug { "New product, persisting to database" }
                return productDetailsRepository.save(newProduct)
            }
        }
        
        return null
    }
    
    private fun hasChanges(oldProduct: Product, newProduct: Product): Boolean {
        if (oldProduct.price != newProduct.price) return true
        if (oldProduct.rarity != newProduct.rarity) return true
        if (oldProduct.detailsUrl != newProduct.detailsUrl) return true
        
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
