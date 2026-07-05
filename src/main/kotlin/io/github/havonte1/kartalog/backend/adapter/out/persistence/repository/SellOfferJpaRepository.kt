package io.github.havonte1.kartalog.backend.adapter.out.persistence.repository

import io.github.havonte1.kartalog.backend.adapter.out.persistence.entity.SellOfferEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SellOfferJpaRepository : JpaRepository<SellOfferEntity, Long> {
    fun findByProductId(productId: Long): List<SellOfferEntity>

    fun findAllByProductIdIn(productIds: List<Long>): List<SellOfferEntity>
}
