package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.SearchResultMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

import java.time.Instant

/**
 * JPA implementation of the [SearchResultRepository] outbound port.
 */
@Component
class SearchResultRepositoryAdapter(
    private val jpaRepository: SearchResultJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val productSetJpaRepository: ProductSetJpaRepository,
    private val seriesJpaRepository: SeriesJpaRepository,
    private val searchResultMapper: SearchResultMapper,
    private val productMapper: ProductMapper,
) : SearchResultRepository {

    @Transactional(readOnly = true)
    override fun findByQuery(query: String): SearchResult? =
        jpaRepository.findByQuery(query)?.let { searchResultMapper.toDomain(it) }

    @Transactional
    override fun save(searchResult: SearchResult): SearchResult {
        val entity = searchResultMapper.toEntityWithoutProducts(searchResult)
        // Upsert related products and obtain managed entities
        val managedProducts = upsertProducts(searchResult.products)
        entity.products.clear()
        entity.products.addAll(managedProducts)
        val saved = jpaRepository.save(entity)
        return searchResultMapper.toDomain(saved)
    }

    /**
     * Inserts new product entities or updates existing ones based on external ID.
     * Uses transaction isolation to prevent race conditions by ensuring all operations
     * happen within the same database transaction with consistent snapshot isolation.
     */
    private fun upsertProducts(products: List<Product>): List<ProductEntity> {
        if (products.isEmpty()) return emptyList()

        val externalIds = products.map { it.externalId }

        // Query existing products in the same transaction to get a consistent view
        val existingProducts = productJpaRepository.findAllByExternalIdIn(externalIds)
            .associateBy { it.externalId }

        val uniqueCmCodes = products.mapNotNull { it.set?.cmCode }.distinct()
        val existingSets = uniqueCmCodes.flatMap { productSetJpaRepository.findByCmProductCode(it) }
        val existingSetMap = existingSets.associateBy { it.cmProductCode }

        val newCmCodes = uniqueCmCodes.filter { it !in existingSetMap.keys }
        val newSets = newCmCodes.map { cmCode ->
            val product = products.first { it.set?.cmCode == cmCode }
            val productSetEntity = ProductSetEntity(cmProductCode = cmCode)
            product.set?.names?.forEach { (lang, name) ->
                val nameTranslationEntity = NameTranslationEntity(
                    productSet = productSetEntity,
                    languageCode = lang,
                    name = name
                )
                productSetEntity.nameTranslations.add(nameTranslationEntity)
            }
            productSetEntity
        }

        val savedNewSets = if (newSets.isNotEmpty()) {
            productSetJpaRepository.saveAll(newSets)
        } else {
            emptyList()
        }

        val allSetMap = (existingSets + savedNewSets).associateBy { it.cmProductCode }

        val entitiesToPersist = products.map { product ->
            val existing = existingProducts[product.externalId]
            if (existing != null) {
                // Update existing product with new data
                updateEntity(existing, product)
                existing
            } else {
                val cmCode = product.set?.cmCode ?: "dummy"
                val productSetEntity = allSetMap[cmCode]
                    ?: ProductSetEntity(cmProductCode = cmCode)

                product.set?.names?.forEach { (lang, name) ->
                    val nameTranslationEntity = NameTranslationEntity(
                        id = 0,
                        productSet = productSetEntity,
                        languageCode = lang,
                        name = name
                    )
                    productSetEntity.nameTranslations.add(nameTranslationEntity)
                }

                val productEntity = productMapper.toEntity(product, productSetEntity)
                productEntity
            }
        }

        // Persist all entities in a single batch operation within the same transaction
        return productJpaRepository.saveAll(entitiesToPersist)
    }

    private fun mergeNameTranslations(entity: ProductEntity, product: Product) {
        val existingLocales = entity.nameTranslations.associate { it.languageCode to it }
        product.names.forEach { (locale, name) ->
            if (existingLocales.containsKey(locale)) {
                existingLocales[locale]!!.name = name
            } else {
                val translation = NameTranslationEntity(
                    languageCode = locale,
                    name = name
                )
                entity.nameTranslations.add(translation)
            }
        }
    }

    private fun updateEntity(
        productEntity: ProductEntity,
        product: Product
    ): ProductEntity {
        val updated = productEntity.copy(
            price = product.price,
            priceTrend = product.priceTrendInfo?.value,
            priceTrendValid = product.priceTrendInfo?.valid ?: false,
            updatedAt = product.updatedAt ?: Instant.now()
        )

        // only update if values actually changed
        if (productEntity.compareTo(updated) != 0) {
            return productJpaRepository.save(updated)
        }
        mergeNameTranslations(productEntity, product)
        return updated
    }

    @Transactional
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }

    override fun countByQuery(query: String): Int = jpaRepository.countByQuery(query)

}
