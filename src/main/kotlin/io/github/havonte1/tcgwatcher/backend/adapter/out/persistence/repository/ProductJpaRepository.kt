package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByExternalId(externalId: Long): ProductEntity?
    fun findAllByExternalIdIn(externalIds: Collection<Long>): List<ProductEntity>
    fun findByCmIdAndGenreAndType(cmId: String?, genre: String?, type: String?): ProductEntity?

}
