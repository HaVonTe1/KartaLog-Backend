package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SellOfferEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SeriesEntity
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.LanguagePricing
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductAttribute
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductAttributeType
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSeries
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import io.github.havonte1.tcgwatcher.backend.domain.model.SellOffer
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity
import org.springframework.stereotype.Component

@Component
class ProductMapper {
    fun toEntity(
        product: Product,
        set: ProductSetEntity,
    ): ProductEntity {
        val entity =
            ProductEntity(
                id = product.id,
                externalId = product.externalId,
                sourceId = product.sourceId,
                productSet = set,
                setId = set.id,
                seriesId = product.series?.seriesId,
                rarity = product.rarity,
                codeInfo = product.codeInfo?.value,
                codeInfoValid = product.codeInfo?.valid,
                genre = product.genre.identifier,
                type = product.type.cmIdentifier,
                cmId = product.cmId,
                imgLink = product.imgLink,
                price = product.price,
                priceTrend = product.priceTrendInfo?.value,
                priceTrendValid = product.priceTrendInfo?.valid,
                releaseDate = product.releaseDate,
                cardNumber = product.cardNumber,
                languagePricing = serializeLanguagePricing(product.languagePricing),
                productAttributes = serializeProductAttributes(product.productAttributes),
            )
        product.names.forEach { (locale, name) ->
            val translation =
                NameTranslationEntity(
                    id = 0,
                    product = entity,
                    languageCode = locale.code,
                    name = name,
                )
            entity.nameTranslations.add(translation)
        }
        product.sellOffers?.forEach { sellOffer ->
            val sellOfferEntity =
                SellOfferEntity(
                    id = entity.id,
                    product = entity,
                    sellerName = sellOffer.sellerName,
                    sellerLocation = sellOffer.sellerLocation,
                    productLanguage = sellOffer.productLanguage,
                    special = sellOffer.special,
                    condition = sellOffer.condition,
                    amount = sellOffer.amount,
                    price = sellOffer.price,
                )
            entity.sellOffers.add(sellOfferEntity)
        }
        return entity
    }

    fun toSellOfferEntity(sellOffer: SellOffer, productEntity: ProductEntity): SellOfferEntity {
        return SellOfferEntity(
            id = sellOffer.sellOfferId,
            sellerName = sellOffer.sellerName,
            sellerLocation = sellOffer.sellerLocation,
            productLanguage = sellOffer.productLanguage,
            special = sellOffer.special,
            condition = sellOffer.condition,
            amount = sellOffer.amount,
            price = sellOffer.price,
            product = productEntity,
        )
    }

    private fun serializeLanguagePricing(languagePricing: List<LanguagePricing>): String? {
        if (languagePricing.isEmpty()) return null
        return languagePricing.joinToString(";") {
            "${it.locale.code}|${it.price}|${it.priceTrend}|${it.priceTrendValid}"
        }
    }

    private fun deserializeLanguagePricing(data: String?): List<LanguagePricing> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                val locale = Locale.entries.find { it.code == parts[0] }
                if (locale != null) {
                    LanguagePricing(
                        locale = locale,
                        price = parts[1],
                        priceTrend = parts[2],
                        priceTrendValid = parts[3].toBooleanStrictOrNull() ?: false,
                    )
                } else null
            } else null
        }
    }

    private fun serializeProductAttributes(attributes: List<ProductAttribute>): String? {
        if (attributes.isEmpty()) return null
        return attributes.joinToString(";") { "${it.attributeName}|${it.attributeType.name}|${it.value}" }
    }

    private fun deserializeProductAttributes(data: String?): List<ProductAttribute> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                ProductAttribute(
                    attributeName = parts[0],
                    attributeType = ProductAttributeType.entries.find { it.name == parts[1] }
                        ?: ProductAttributeType.RARITY,
                    value = parts[2],
                )
            } else null
        }
    }

    fun toProductSet(entity: ProductSetEntity?): ProductSet? {
        if (entity == null || entity.id == null) {
            return null
        }
        return ProductSet(
            setId = entity.id!!,
            cmCode = entity.cmProductCode ?: "",
            names =
                entity.nameTranslations.associate {
                    Locale.fromId(it.languageCode) to
                            it.name
                },
            seriesId = entity.series?.id,
            seriesNames =
                entity.series?.nameTranslations?.associate {
                    Locale.fromId(it.languageCode) to it.name
                } ?: emptyMap(),
        )
    }

    fun toProductSeries(entity: SeriesEntity?): ProductSeries? {
        if (entity == null || entity.id == null) {
            return null
        }
        return ProductSeries(
            seriesId = entity.id!!,
            names = entity.nameTranslations.associate { Locale.valueOf(it.languageCode) to it.name })
    }

    fun toDomain(entity: ProductEntity): Product {
        val namesMap = entity.nameTranslations.associate { Locale.fromId(it.languageCode) to it.name }
        val sellOffers =
            entity.sellOffers.map { sellOffer ->
                SellOffer(
                    sellOfferId = sellOffer.id?:0L,
                    sellerName = sellOffer.sellerName,
                    sellerLocation = sellOffer.sellerLocation,
                    productLanguage = sellOffer.productLanguage,
                    special = sellOffer.special,
                    condition = sellOffer.condition,
                    amount = sellOffer.amount,
                    price = sellOffer.price,
                )
            }
        return Product(
            id = entity.id,
            externalId = entity.externalId,
            sourceId = entity.sourceId,
            set = toProductSet(entity.productSet),
            series = toProductSeries(entity.series),
            rarity = entity.rarity,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            names = namesMap,
            codeInfo = if (entity.codeInfo != null) StringWithValidity(entity.codeInfo, entity.codeInfoValid) else null,
            genre = Genre.fromId(entity.genre),
            type = ProductType.fromId(entity.type),
            cmId = entity.cmId,
            imgLink = entity.imgLink,
            price = entity.price,
            priceTrendInfo =
                if (entity.priceTrend != null) {
                    StringWithValidity(
                        entity.priceTrend,
                        entity.priceTrendValid,
                    )
                } else {
                    null
                },
            sellOffers = sellOffers,
            languagePricing = deserializeLanguagePricing(entity.languagePricing),
            productAttributes = deserializeProductAttributes(entity.productAttributes),
            releaseDate = entity.releaseDate,
            cardNumber = entity.cardNumber,
        )
    }
}
