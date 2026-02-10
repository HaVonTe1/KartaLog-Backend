package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.domain.model.Product

object CollectablesMapper {
    fun toDto(
        product: Product
    ): ProductDTO {
        return ProductDTO(
            id = product.id,
            externalId = product.externalId,
            setName = product.setName,
            rarity = product.rarity,
            imageUrl = product.imgLink?.let { java.net.URI.create(it) },
            createdAt = product.createdAt?.let { java.time.OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) },
            updatedAt = product.updatedAt?.let { java.time.OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) }
        )
    }
}
