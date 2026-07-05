package io.github.havonte1.kartalog.backend.adapter.inbound.rest

import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.LanguagePricingDTO
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.ProductAttributeDTO
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.ProductDetailsDTO
import io.github.havonte1.kartalog.backend.adapter.inbound.rest.model.SellOfferDTO
import io.github.havonte1.kartalog.backend.config.GenreConfig
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.Product
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object CollectablesMapper {
    fun toDto(
        product: Product,
        locale: Locale
    ): ProductDTO =
        ProductDTO(
            externalId = product.externalId,
            cmId = product.cmId ?: "",
            displayName = product.names[locale],
            genre = product.genre.name,
            type = product.type.cmIdentifier,
            setCode = product.set?.cmCode,
            setName = product.set?.names[locale],
            seriesId = product.series?.seriesId ?: product.set?.seriesId,
            code = product.codeInfo?.value,
            price = product.price ?: "",
            imageUrl = product.imgLink?.let { URI.create(it) },
        )

    fun toDetailDto(
        product: Product,
        locale: Locale,
    ): ProductDetailsDTO =
        ProductDetailsDTO(
            externalId = product.externalId,
            cmId = product.cmId ?: "",
            genre = product.genre.identifier,
            type = product.type.cmIdentifier,
            detailsUrl =
                "${GenreConfig.buildDetailsUrlBase(product.genre, locale, product.type)}/${product.set?.cmCode}/${product.cmId}"
                    .let { URLEncoder.encode(it, StandardCharsets.UTF_8) }
                    .let { URI.create(it) },
            price = product.price ?: "",
            setName = product.set?.names[locale],
            setCode = product.set?.cmCode,
            seriesId = product.series?.seriesId ?: product.set?.seriesId,
            seriesName = product.series?.names?.get(locale) ?: product.set?.seriesNames?.get(locale),
            code = product.codeInfo?.value,
            codeValid = product.codeInfo?.valid,
            rarity = product.rarity,
            imageUrl = product.imgLink?.let { URI.create(it) },
            priceTrend = product.priceTrendInfo?.value ?: "",
            sellOffers =
                product.sellOffers?.map { sellOffer ->
                    SellOfferDTO(
                        sellerName = sellOffer.sellerName,
                        sellerLocation = sellOffer.sellerLocation,
                        productLanguage = sellOffer.productLanguage,
                        special = sellOffer.special,
                        condition = sellOffer.condition,
                        amount = sellOffer.amount,
                        price = sellOffer.price,
                    )
                },
            languagePricing =
                product.languagePricing.map { lp ->
                    LanguagePricingDTO(
                        locale = lp.locale.code,
                        price = lp.price,
                        priceTrend = lp.priceTrend,
                        priceTrendValid = lp.priceTrendValid,
                    )
                },
            productAttributes =
                product.productAttributes.map { pa ->
                    ProductAttributeDTO(
                        attributeName = pa.attributeName,
                        value = pa.value,
                        attributeType = pa.attributeType.name,
                    )
                },
            releaseDate = product.releaseDate,
            cardNumber = product.cardNumber,
        )
}
