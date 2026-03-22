package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class CollectablesAdapter(
    private val collectablesService: SearchUseCase,
) : CollectablesApi {
    private val logger = KotlinLogging.logger {}

    @RateLimiter(name = "apiRateLimiter")
    override suspend fun listCollectables(
        query: String,
        genre: String,
        type: String,
        locale: String,
        ifNoneMatch: String?,
    ): ResponseEntity<List<ProductDTO>> {
        logger.debug {
            "listCollectables called with  query={$query} locale={$locale} game=$genre"
        }
        val existingCachedAt = collectablesService.getSearchCachedAt(query)
        val existingETag = existingCachedAt?.epochSecond?.toString()

        if (ifNoneMatch != null && ifNoneMatch == existingETag) {
            logger.debug { "ETag match, returning 304 for query='$query'" }
            return ResponseEntity
                .status(HttpStatus.NOT_MODIFIED)
                .eTag(existingETag)
                .build()
        }

        val results = collectablesService.search(query, locale, genre)
        val dtoList: List<ProductDTO> = results.map { CollectablesMapper.toDto(it, locale) }
        val newCachedAt = collectablesService.getSearchCachedAt(query)
        val newETag = newCachedAt?.epochSecond?.toString()

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .eTag(newETag)
            .body(dtoList)
    }

    @RateLimiter(name = "apiRateLimiter")
    override suspend fun getProductDetails(
        cmId: String,
        setname: String,
        genre: String,
        type: String,
        lang: String,
        ifNoneMatch: String?,
    ): ResponseEntity<ProductDetailsDTO> {
        val existingUpdatedAt = collectablesService.getProductUpdatedAt(cmId)
        val existingETag = existingUpdatedAt?.epochSecond?.toString()

        if (ifNoneMatch != null && ifNoneMatch == existingETag) {
            logger.debug { "ETag match, returning 304 for cmId=$cmId" }
            return ResponseEntity
                .status(HttpStatus.NOT_MODIFIED)
                .eTag(existingETag)
                .build()
        }

        val productDetails = collectablesService.fetchProductDetails(cmId, genre, type, lang, setname)
        if (productDetails != null) {
            val dto = CollectablesMapper.toDetailDto(productDetails, lang)
            val newUpdatedAt = collectablesService.getProductUpdatedAt(cmId)
            val newETag = newUpdatedAt?.epochSecond?.toString()

            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .eTag(newETag)
                .body(dto)
        }
        return ResponseEntity.notFound().build()
    }
}
