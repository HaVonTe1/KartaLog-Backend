package com.github.havonte1.adapter.out.persistence.repository

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.ProductRepository
import com.github.havonte1.adapter.out.persistence.mapper.ProductMapper
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

    @Transactional
    override fun save(product: Product): Product {
        val entity = mapper.toEntity(product)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
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
