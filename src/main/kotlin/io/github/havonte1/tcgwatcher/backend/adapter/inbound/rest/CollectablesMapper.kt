package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.domain.model.Product

object CollectablesMapper {
    fun toDto(
        product: Product
    ): ProductDTO {
        return ProductDTO(
            id = product.cmId,
            externalId = product.externalId,
            setName = product.setName,
            rarity = product.rarity,
            imageUrl = product.imgLink?.let { java.net.URI.create(it) },
            type = product.type,
            genre = product.genre,
        )
    }
}
