package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

object CollectablesMapper {
    fun toDto(product: io.github.havonte1.tcgwatcher.backend.domain.model.Product): io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO {
        return io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO(
            id = product.id,
            externalId = product.externalId,
            setName = product.setName,
            rarity = product.rarity,
            imageUrl = product.imageUrl?.let { java.net.URI.create(it) },
            createdAt = product.createdAt?.let { java.time.OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) },
            updatedAt = product.updatedAt?.let { java.time.OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) }
        )
    }
}
