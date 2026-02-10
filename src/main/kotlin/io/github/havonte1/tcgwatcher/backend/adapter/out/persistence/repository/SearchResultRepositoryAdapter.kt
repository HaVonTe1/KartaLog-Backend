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

        val managedProducts = searchResult.products.map { product ->
            val productEntity = productJpaRepository
                .findByExternalId(product.externalId)
            if (productEntity != null) {
                updateEntity(productEntity, product)
            } else {
                productJpaRepository.save(productMapper.toEntity(product))
            }
        }

        entity.products.clear()
        entity.products.addAll(managedProducts)
        val saved = jpaRepository.save(entity)
        return searchResultMapper.toDomain(saved)
    }

    private fun updateEntity(
        productEntity: ProductEntity,
        product: Product
    ): ProductEntity {
        val updatedEntity = productEntity.copy(
            price = product.price,
            priceTrend = product.priceTrendInfo?.value,
            priceTrendValid = product.priceTrendInfo?.valid ?: false,
            updatedAt = product.updatedAt
        )
        return productJpaRepository.save(updatedEntity)
    }

    @Transactional
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }
}
