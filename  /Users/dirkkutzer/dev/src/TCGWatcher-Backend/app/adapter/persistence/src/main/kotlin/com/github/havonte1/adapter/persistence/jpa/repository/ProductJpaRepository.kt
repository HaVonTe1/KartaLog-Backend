package com.github.havonte1.adapter.persistence.jpa.repository

import com.github.havonte1.adapter.persistence.jpa.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for ProductEntity.
 */
@Repository
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByExternalId(externalId: Long): ProductEntity?
}
