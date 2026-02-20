package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectablesAdapter(
    private val collectablesService: SearchUseCase
) : CollectablesApi {

    private val logger = KotlinLogging.logger {}

    @RateLimiter(name = "apiRateLimiter")
    override suspend fun listCollectables(
        query: String,
        page: Int,
        size: Int,
        locale: String,
        game: String
    ): ResponseEntity<List<ProductDTO>> {

        require(page >= 0) { "Page index must be non-negative" }
        require(size > 0) { "Page size must be positive" }
        require(query.isNotBlank()) { "Query must not be blank" }
        logger.debug {
            "listCollectables called with page={$page}, size={$size}, query={$query} locale={$locale} game=$game"
        }
        val results = collectablesService.search(query, locale, game)

        val from = (page * size).coerceAtMost(results.size)
        val to = ((page + 1) * size).coerceAtMost(results.size)
        val pageSlice = results.subList(from, to)
        val dtoList: List<ProductDTO> = pageSlice.map { CollectablesMapper.toDto(it) }
        return ResponseEntity(dtoList, HttpStatus.OK)
    }

    @RateLimiter(name = "apiRateLimiter")
    override suspend fun getProductDetails(
        cmId: String,
        setname: String,
        genre: String,
        type: String,
        lang: String
    ): ResponseEntity<ProductDetailsDTO> {
        val productDetails = collectablesService.fetchProductDetails(cmId, genre, type, lang, setname)
        if(productDetails!=null) {
            return ResponseEntity.ok(CollectablesMapper.toDetailDto(product = productDetails))
        }
        return ResponseEntity.notFound().build()
    }
}
