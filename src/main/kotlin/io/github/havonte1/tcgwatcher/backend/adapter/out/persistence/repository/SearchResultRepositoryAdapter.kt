package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.SearchResultMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * JPA implementation of the [SearchResultRepository] outbound port.
 */
@Component
class SearchResultRepositoryAdapter(
    private val jpaRepository: SearchResultJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
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
     */
    private fun upsertProducts(products: List<Product>): List<ProductEntity> {
        val externalIds = products.map { it.externalId }
        val existingProducts = productJpaRepository
            .findAllByExternalIdIn(externalIds)
            .associateBy { it.externalId }

        val entitiesToPersist = products.map { product ->
            val existing = existingProducts[product.externalId]
            if (existing != null) {
                updateEntity(existing, product)
                existing
            } else {
                productMapper.toEntity(product)
            }
        }
        return productJpaRepository.saveAll(entitiesToPersist)
    }

    private fun updateEntity(
        productEntity: ProductEntity,
        product: Product
    ): ProductEntity {
        val updated = productEntity.copy(
            price = product.price,
            priceTrend = product.priceTrendInfo?.value,
            priceTrendValid = product.priceTrendInfo?.valid ?: false,
            updatedAt = product.updatedAt
        )

        if (productEntity.compareTo(updated) != 0) {
            return productJpaRepository.save(updated)
        }
        return updated
    }

    @Transactional
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }
}
