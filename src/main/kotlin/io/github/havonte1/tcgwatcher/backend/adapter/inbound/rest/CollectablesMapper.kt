package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.SellOfferDTO
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder

object CollectablesMapper {
    fun toDto(
        product: Product,
        locale: String,
    ): ProductDTO {

        return ProductDTO(
            externalId = product.externalId,
            cmId = product.cmId?:"",
            genre = product.genre,
            type = product.type,
            setName = product.set?.names[locale],
            setCode = product.set?.cmCode,
            seriesId = product.series?.seriesId,
            seriesName = product.series?.names?.get(locale),
            rarity = product.rarity,
            code = product.codeInfo?.value,
            codeValid = product.codeInfo?.valid,
            price = product.price ?: "",
            priceTrend = product.priceTrendInfo?.value ?: "",
            imageUrl = product.imgLink?.let { URI.create(it) }
        )
    }

    fun toDetailDto(product: Product,locale: String): ProductDetailsDTO {
        return ProductDetailsDTO(
            externalId = product.externalId,
            cmId = product.cmId?:"",
            genre = product.genre,
            type = product.type,
            detailsUrl = listOfNotNull(
                product.genre,
                "product",
                product.type,
                product.set?.cmCode,
                product.cmId
            ).joinToString("/") { URLEncoder.encode(it, "UTF-8") }.let { URI.create(it) },
            price = product.price ?: "",
            setName = product.set?.names[locale],
            setCode = product.set?.cmCode,
            seriesId = product.series?.seriesId,
            seriesName = product.series?.names?.get(locale),
            code = product.codeInfo?.value,
            codeValid = product.codeInfo?.valid,
            rarity = product.rarity,
            imageUrl = product.imgLink?.let { URI.create(it) },
            priceTrend = product.priceTrendInfo?.value ?: "",
            sellOffers = product.sellOffers?.map { sellOffer ->
                SellOfferDTO(
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
