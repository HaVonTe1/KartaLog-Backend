package com.github.havonte1.adapter.inbound.rest

import com.github.havonte1.adapter.inbound.rest.model.ProductDTO
import com.github.havonte1.domain.model.Product
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Instant

/**
 * Stateless mapper converting between the OpenAPI generated DTO ([ProductDTO])
 * and the core domain model ([Product]).
 *
 * Implemented as a Kotlin `object` (singleton) because it holds no mutable state.
 */
object CollectablesMapper {

    /** Convert a generated DTO into a domain [Product]. */
    fun toDomain(dto: ProductDTO): Product = Product(
        id = dto.id?.toLong() ?: 0L,
        externalId = dto.externalId ?: 0L,
        setName = dto.setName,
        rarity = dto.rarity,
        imageUrl = dto.imageUrl?.toString(),
        createdAt = dto.createdAt?.toInstant(),
        updatedAt = dto.updatedAt?.toInstant(),
        localizedStrings = mutableSetOf()
    )
}
