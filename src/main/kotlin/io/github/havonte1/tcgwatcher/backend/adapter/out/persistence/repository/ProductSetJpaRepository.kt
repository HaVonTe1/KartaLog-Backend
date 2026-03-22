package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductSetJpaRepository : JpaRepository<ProductSetEntity, Long> {
    fun findBySourceId(sourceId: String): ProductSetEntity?

    fun existsBySourceId(sourceId: String): Boolean

    fun findByCmProductCode(cmProductCode: String): MutableList<ProductSetEntity>
}
