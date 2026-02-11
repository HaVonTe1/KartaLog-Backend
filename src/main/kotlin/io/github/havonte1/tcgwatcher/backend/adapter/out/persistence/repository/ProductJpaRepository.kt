package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for ProductEntity.
 */

// TODO: use envers or something simliar to store history and auditing of products

@Repository
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByExternalId(externalId: Long): ProductEntity?
    fun findAllByExternalIdIn(externalIds: Collection<Long>): List<ProductEntity>

}
