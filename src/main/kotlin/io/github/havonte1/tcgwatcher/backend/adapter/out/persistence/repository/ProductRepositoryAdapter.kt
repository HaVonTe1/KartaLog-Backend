package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Implementation of the outbound port [ProductRepository] using Spring Data JPA.
 */
@Component
class ProductRepositoryAdapter(
    private val jpaRepository: ProductJpaRepository,
    private val mapper: ProductMapper
) : ProductRepository {
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }

    @Transactional
    override fun save(product: Product): Product {
        val entity = mapper.toEntity(product)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun saveAll(products: List<Product>): List<Product> {
        val entities = products.map { mapper.toEntity(it) }
        val productEntities = jpaRepository.saveAll(entities)
        return productEntities.map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Product? =
        jpaRepository.findById(id).orElse(null)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findByExternalId(externalId: Long): Product? =
        jpaRepository.findByExternalId(externalId)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAll(): List<Product> =
        jpaRepository.findAll().map { mapper.toDomain(it) }

    @Transactional
    override fun delete(product: Product) {
        jpaRepository.findById(product.id).ifPresent { jpaRepository.delete(it) }
    }
}
