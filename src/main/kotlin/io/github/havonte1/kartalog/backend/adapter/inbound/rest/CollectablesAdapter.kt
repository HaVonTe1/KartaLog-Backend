package io.github.havonte1.kartalog.backend.adapter.inbound.rest

import io.github.havonte1.kartalog.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.GenreSchema
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.LocaleSchema
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.ProductTypeSchema
import io.github.havonte1.kartalog.backend.application.SearchResponse
import io.github.havonte1.kartalog.backend.application.SearchUseCase
import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class CollectablesAdapter(
    private val collectablesService: SearchUseCase,
    private val meterRegistry: MeterRegistry,
) : CollectablesApi {
    private val logger = KotlinLogging.logger {}

    override suspend fun listCollectables(
        query: String,
        genre: GenreSchema,
        type: ProductTypeSchema,
        locale: LocaleSchema,
    ): ResponseEntity<List<ProductDTO>> {
        logger.debug {
            "listCollectables called with query={$query} locale={$locale} genre=$genre"
        }

        val genreEnum =
            Genre.fromId(genre.value)
        val localeEnum =
            Locale.fromId(locale.value)

        val searchTimer = Timer.builder("api.search.duration").register(meterRegistry)
        val startTime = System.currentTimeMillis()

        //-----------------------------------
        val searchResponse: SearchResponse = collectablesService.search(
            query,
            localeEnum,
            genreEnum,
        )
        //-----------------------------------
        searchTimer.record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        meterRegistry.counter("api.search.requests").increment()

        val dtoList: List<ProductDTO> = searchResponse.products.map { CollectablesMapper.toDto(it, localeEnum) }
        val newETag = searchResponse.cachedAt.epochSecond.toString()

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .eTag(newETag)
            .body(dtoList)
    }

    override suspend fun getProductDetails(
        cmId: String,
        genre: GenreSchema,
        type: ProductTypeSchema,
        lang: LocaleSchema,
        setname: String,
    ): ResponseEntity<ProductDetailsDTO> {

        val genreEnum = Genre.fromId(genre.value)
        val localeEnum = Locale.fromId(lang.value)
        val typeEnum = ProductType.fromId(type.value)

        val detailsTimer = Timer.builder("api.details.duration").register(meterRegistry)
        val startTime = System.currentTimeMillis()
        val productDetails = collectablesService.fetchProductDetails(cmId, genreEnum, typeEnum, localeEnum, setname)
        detailsTimer.record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        meterRegistry.counter("api.details.requests").increment()
        if (productDetails != null) {
            val dto = CollectablesMapper.toDetailDto(productDetails, localeEnum)
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
