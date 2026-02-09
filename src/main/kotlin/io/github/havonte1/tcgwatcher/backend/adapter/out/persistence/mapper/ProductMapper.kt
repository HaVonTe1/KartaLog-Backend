package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity
import org.springframework.stereotype.Component

/**
 * Mapper responsible for converting between the core domain model [Product] and the JPA
 * persistence entity [ProductEntity]. All mapping logic is kept in one place to keep the
 * repository implementation clean and to enable easier testing.
 */
@Component
class ProductMapper {
    /** Convert a domain [Product] into a JPA [ProductEntity]. */
    fun toEntity(product: Product): ProductEntity {
        // Create the product entity with an empty set of name translations
        val entity = ProductEntity(
            id = product.id,
            externalId = product.externalId,
            setName = product.setName,
            rarity = product.rarity,
            imageUrl = product.imageUrl,
            codeInfo = product.codeInfo?.value,
            codeInfoValid = product.codeInfo?.valid,
            genre = product.genre,
            type = product.type,
            cmId = product.cmId,
            cmLink = product.cmLink,
            imgLink = product.imgLink,
            price = product.price,
            priceTrend = product.priceTrendInfo?.value,
            priceTrendValid = product.priceTrendInfo?.valid,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            nameTranslations = mutableSetOf()
        )
        // Map each name translation from the product's names map
        product.names.forEach { (locale, name) ->
            val translation = NameTranslationEntity(
                id = 0, // let JPA generate ID
                product = entity,
                languageCode = locale,
                name = name
            )
            entity.nameTranslations.add(translation)
        }
        return entity
    }

    /** Convert a JPA [ProductEntity] into a domain [Product]. */
    fun toDomain(entity: ProductEntity): Product {
        val namesMap = entity.nameTranslations.associate { it.languageCode to it.name }
        return Product(
            id = entity.id,
            externalId = entity.externalId,
            setName = entity.setName,
            rarity = entity.rarity,
            imageUrl = entity.imageUrl,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            names = namesMap,
            codeInfo = if (entity.codeInfo != null) StringWithValidity(entity.codeInfo, entity.codeInfoValid) else null,
            genre = entity.genre,
            type = entity.type,
            cmId = entity.cmId,
            cmLink = entity.cmLink,
            imgLink = entity.imgLink,
            price = entity.price,
            priceTrendInfo = if (entity.priceTrend != null) StringWithValidity(entity.priceTrend, entity.priceTrendValid) else null
        )
    }


}
