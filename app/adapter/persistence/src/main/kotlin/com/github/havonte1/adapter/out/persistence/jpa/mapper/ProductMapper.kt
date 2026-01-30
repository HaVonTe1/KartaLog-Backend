package com.github.havonte1.adapter.out.persistence.mapper

import com.github.havonte1.adapter.out.persistence.entity.LanguageEntity
import com.github.havonte1.adapter.out.persistence.entity.LocalizedStringEntity
import com.github.havonte1.adapter.out.persistence.entity.ProductEntity
import com.github.havonte1.domain.model.Language
import com.github.havonte1.domain.model.LocalizedString
import com.github.havonte1.domain.model.Product

/**
 * Mapper responsible for converting between the core domain model [Product] and the JPA
 * persistence entity [ProductEntity]. All mapping logic is kept in one place to keep the
 * repository implementation clean and to enable easier testing.
 */
import org.springframework.stereotype.Component

@Component
class ProductMapper {
    /** Convert a domain [Product] into a JPA [ProductEntity]. */
    fun toEntity(product: Product): ProductEntity {
        // Create the product entity without localized strings first to avoid circular reference issues
        val entity = ProductEntity(
            id = product.id,
            externalId = product.externalId,
            setName = product.setName,
            rarity = product.rarity,
            imageUrl = product.imageUrl,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            localizedStrings = mutableSetOf()
        )
        // Map each localized string, creating language and localized string entities
        product.localizedStrings.forEach { ls ->
            val languageEntity = LanguageEntity(ls.language.code, ls.language.name)
            val lsEntity = LocalizedStringEntity(
                id = ls.id,
                product = entity,
                language = languageEntity,
                type = ls.type,
                value = ls.value
            )
            entity.localizedStrings.add(lsEntity)
        }
        return entity
    }

    /** Convert a JPA [ProductEntity] into a domain [Product]. */
    fun toDomain(entity: ProductEntity): Product {
        val domainLocalized = entity.localizedStrings.map { toDomain(it) }.toMutableSet()
        return Product(
            id = entity.id,
            externalId = entity.externalId,
            setName = entity.setName,
            rarity = entity.rarity,
            imageUrl = entity.imageUrl,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            localizedStrings = domainLocalized
        )
    }

    private fun toDomain(lsEntity: LocalizedStringEntity): LocalizedString {
        // Build a minimal Product representation to avoid deep recursion (localized strings will be attached later)
        val minimalProduct = Product(
            id = lsEntity.product.id,
            externalId = lsEntity.product.externalId,
            setName = lsEntity.product.setName,
            rarity = lsEntity.product.rarity,
            imageUrl = lsEntity.product.imageUrl,
            createdAt = lsEntity.product.createdAt,
            updatedAt = lsEntity.product.updatedAt,
            localizedStrings = mutableSetOf()
        )
        return LocalizedString(
            id = lsEntity.id,
            product = minimalProduct,
            language = Language(lsEntity.language.code, lsEntity.language.name),
            type = lsEntity.type,
            value = lsEntity.value
        )
    }
}
