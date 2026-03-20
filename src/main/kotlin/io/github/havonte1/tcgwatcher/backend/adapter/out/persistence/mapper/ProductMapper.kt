package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SellOfferEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SeriesEntity
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSeries
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import io.github.havonte1.tcgwatcher.backend.domain.model.SellOffer
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity
import org.springframework.stereotype.Component

@Component
class ProductMapper {
    fun toEntity(product: Product, set: ProductSetEntity): ProductEntity {
        val entity = ProductEntity(
            id = product.id,
            externalId = product.externalId,
            sourceId = product.sourceId,
            productSet = set,
            setId = set.id,
            seriesId = product.series?.seriesId,
            rarity = product.rarity,
            codeInfo = product.codeInfo?.value,
            codeInfoValid = product.codeInfo?.valid,
            genre = product.genre,
            type = product.type,
            cmId = product.cmId,
            imgLink = product.imgLink,
            price = product.price,
            priceTrend = product.priceTrendInfo?.value,
            priceTrendValid = product.priceTrendInfo?.valid,
        )
        product.names.forEach { (locale, name) ->
            val sanitizedLocale = if (locale.length <= 5) locale else "?"
            val translation = NameTranslationEntity(
                id = 0,
                product = entity,
                languageCode = sanitizedLocale,
                name = name
            )
            entity.nameTranslations.add(translation)
        }
        product.sellOffers?.forEach { sellOffer ->
            val sellOfferEntity = SellOfferEntity(
                id = null,
                product = entity,
                sellerName = sellOffer.sellerName,
                sellerLocation = sellOffer.sellerLocation,
                productLanguage = sellOffer.productLanguage,
                special = sellOffer.special,
                condition = sellOffer.condition,
                amount = sellOffer.amount,
                price = sellOffer.price
            )
            entity.sellOffers.add(sellOfferEntity)
        }
        return entity
    }

    fun toProductSet(entity: ProductSetEntity?): ProductSet?{
        if(entity == null || entity.id == null )
            return null
        return ProductSet(setId = entity.id, cmCode = entity.cmProductCode?:"", names = entity.nameTranslations.associate { it.languageCode to it.name })

    }

    fun toProductSeries(entity: SeriesEntity?): ProductSeries? {
        if(entity == null || entity.id == null )
            return null
        return ProductSeries(seriesId = entity.id, names = entity.nameTranslations.associate { it.languageCode to it.name })

    }

    fun toDomain(entity: ProductEntity): Product {
        val namesMap = entity.nameTranslations.associate { it.languageCode to it.name }
        val sellOffers = entity.sellOffers.map { sellOffer ->
            SellOffer(
                sellerName = sellOffer.sellerName,
                sellerLocation = sellOffer.sellerLocation,
                productLanguage = sellOffer.productLanguage,
                special = sellOffer.special,
                condition = sellOffer.condition,
                amount = sellOffer.amount,
                price = sellOffer.price
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
            genre = entity.genre,
            type = entity.type,
            cmId = entity.cmId,
            imgLink = entity.imgLink,
            price = entity.price,
            priceTrendInfo = if (entity.priceTrend != null) {
                StringWithValidity(
                    entity.priceTrend,
                    entity.priceTrendValid
                )
            } else {
                null
            },
            sellOffers = sellOffers
        )
    }
}
