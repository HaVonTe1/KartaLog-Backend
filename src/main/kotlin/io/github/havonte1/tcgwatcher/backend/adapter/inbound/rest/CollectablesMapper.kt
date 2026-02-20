package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDetailsDTO
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

    fun toDetailDto(product: Product): ProductDetailsDTO {
        return ProductDetailsDTO(
            id = product.cmId,
            externalId = product.externalId,
            setName = product.setName,
            rarity = product.rarity,
            imageUrl = product.imgLink?.let { java.net.URI.create(it) },
            type = product.type,
            genre = product.genre,
            detailsUrl = listOfNotNull(product.genre, "product", product.type, product.setName, product.cmId).joinToString("/").let { java.net.URI.create(it) },

            sellOffers = product.sellOffers?.map { sellOffer ->
                io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.SellOfferDTO(
                    sellerName = sellOffer.sellerName,
                    sellerLocation = sellOffer.sellerLocation,
                    productLanguage = sellOffer.productLanguage,
                    special = sellOffer.special,
                    condition = sellOffer.condition,
                    amount = sellOffer.amount,
                    price = sellOffer.price
                )
            }
            )
    }
}
