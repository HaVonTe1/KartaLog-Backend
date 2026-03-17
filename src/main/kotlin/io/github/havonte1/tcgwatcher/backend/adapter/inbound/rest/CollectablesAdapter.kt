package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.util.ETagUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class CollectablesAdapter(
    private val collectablesService: SearchUseCase
) : CollectablesApi {

    private val logger = KotlinLogging.logger {}


    @RateLimiter(name = "apiRateLimiter")
    override suspend fun listCollectables(
        query: String,
        genre: String,
        type: String,
        locale: String
    ): ResponseEntity<List<ProductDTO>> {
        logger.debug {
            "listCollectables called with  query={$query} locale={$locale} game=$genre"
        }
        val results = collectablesService.search(query, locale, genre)

        val dtoList: List<ProductDTO> = results.map { CollectablesMapper.toDto(it, locale) }
        val eTag = ETagUtil.computeWeakETag(dtoList)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .eTag(eTag)
            .body(dtoList)
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
        if (productDetails != null) {
            val dto = CollectablesMapper.toDetailDto(productDetails, lang)
            val eTag = ETagUtil.computeWeakETag(dto)

            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .eTag(eTag)
                .body(dto)
        }
        return ResponseEntity.notFound().build()
    }
}
