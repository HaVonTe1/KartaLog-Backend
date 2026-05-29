package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SeriesEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSeries
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProductRepositoryAdapterSeriesTest {
    private val mapper = ProductMapper()

    @Test
    fun `mapper correctly maps series when entity has series loaded`() {
        val seriesEntity = SeriesEntity(id = 1L, sourceId = "cardmarket")
        val productEntity =
            ProductEntity(
                id = 100L,
                externalId = 1L,
                sourceId = "cardmarket",
                setId = 2L,
                seriesId = 1L,
                type = "Singles",
                genre = "POKEMON",
                series = seriesEntity,
            )

        val domain = mapper.toDomain(productEntity)

        assertNotNull(domain.series, "Series should be mapped from entity.series")
        assertEquals(1L, domain.series?.seriesId)
    }

    @Test
    fun `mapper correctly maps productSet when entity has productSet loaded`() {
        val productSetEntity = ProductSetEntity(id = 2L, cmProductCode = "SWSH3")
        val productEntity =
            ProductEntity(
                id = 100L,
                externalId = 1L,
                sourceId = "cardmarket",
                setId = 2L,
                type = "Singles",
                genre = "POKEMON",
                productSet = productSetEntity,
            )

        val domain = mapper.toDomain(productEntity)

        assertNotNull(domain.set, "ProductSet should be mapped from entity.productSet")
        assertEquals(2L, domain.set?.setId)
        assertEquals("SWSH3", domain.set?.cmCode)
    }

    @Test
    fun `product without series relationship returns null series despite seriesId`() {
        val productEntity =
            ProductEntity(
                id = 100L,
                externalId = 1L,
                sourceId = "cardmarket",
                seriesId = 1L,
                type = "Singles",
                genre = "POKEMON",
            )

        val domain = mapper.toDomain(productEntity)

        assertNull(domain.series, "Series should be null when entity.series is not loaded (lazy)")
    }

    @Test
    fun `product without productSet relationship returns null set despite setId`() {
        val productEntity =
            ProductEntity(
                id = 100L,
                externalId = 1L,
                sourceId = "cardmarket",
                setId = 2L,
                type = "Singles",
                genre = "POKEMON",
            )

        val domain = mapper.toDomain(productEntity)

        assertNull(domain.set, "ProductSet should be null when entity.productSet is not loaded (lazy)")
    }
}
