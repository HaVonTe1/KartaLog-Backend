package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.resilience.annotation.ConcurrencyLimit
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectablesAdapter(
    private val collectablesService: SearchUseCase
) : CollectablesApi {

    private val logger = KotlinLogging.logger {}


    @ConcurrencyLimit(limit = 10)
    override suspend fun listCollectables(
        query: String,
        genre: String,
        type: String,
        locale: String
    ): ResponseEntity<List<ProductDTO>> {

        require(query.isNotBlank()) { "Query must not be blank" }
        logger.debug {
            "listCollectables called with  query={$query} locale={$locale} game=$genre"
        }
        val results = collectablesService.search(query, locale, genre)

        val dtoList: List<ProductDTO> = results.map { CollectablesMapper.toDto(it, locale) }
        return ResponseEntity(dtoList, HttpStatus.OK)
    }

    @ConcurrencyLimit(limit = 10)
    override suspend fun getProductDetails(
        cmId: String,
        setname: String,
        genre: String,
        type: String,
        lang: String
    ): ResponseEntity<ProductDetailsDTO> {
        val productDetails = collectablesService.fetchProductDetails(cmId, genre, type, lang, setname)
        if(productDetails!=null) {
            return ResponseEntity.ok(CollectablesMapper.toDetailDto(product = productDetails, lang))
        }
        return ResponseEntity.notFound().build()
    }
}
