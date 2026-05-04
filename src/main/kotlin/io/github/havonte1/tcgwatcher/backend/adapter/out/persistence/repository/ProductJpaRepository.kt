package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByExternalId(externalId: Long): ProductEntity?

    fun findAllByExternalIdIn(externalIds: Collection<Long>): List<ProductEntity>

    fun findByCmId(cmId: String?): ProductEntity?

    fun existsByExternalId(externalId: Long): Boolean

    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.series LEFT JOIN FETCH p.productSet LEFT JOIN FETCH p.nameTranslations LEFT JOIN FETCH p.sellOffers WHERE p.id = :id")
    fun findWithDetailsById(@Param("id") id: Long): ProductEntity?

    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.series LEFT JOIN FETCH p.productSet LEFT JOIN FETCH p.nameTranslations LEFT JOIN FETCH p.sellOffers")
    fun findAllWithDetails(): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.series LEFT JOIN FETCH p.productSet LEFT JOIN FETCH p.nameTranslations LEFT JOIN FETCH p.sellOffers WHERE p.externalId IN :externalIds")
    fun findAllWithDetailsByExternalIdIn(@Param("externalIds") externalIds: Collection<Long>): List<ProductEntity>
}
