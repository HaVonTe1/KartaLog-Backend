package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.SearchResultMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
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
    private val mapper: SearchResultMapper
) : SearchResultRepository {

    @Transactional(readOnly = true)
    override fun findByQuery(query: String): SearchResult? =
        jpaRepository.findByQuery(query)?.let { mapper.toDomain(it) }

    @Transactional
    override fun save(searchResult: SearchResult): SearchResult {
        val entity = mapper.toEntity(searchResult)


        val managedProducts = entity.products.map { product ->
            productJpaRepository
                .findByExternalId(product.externalId)
                ?: productJpaRepository.save(product)
        }

        entity.products.clear()
        entity.products.addAll(managedProducts)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }
}
