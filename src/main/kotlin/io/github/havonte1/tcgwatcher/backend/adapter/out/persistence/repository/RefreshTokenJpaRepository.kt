package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByTokenHashAndRevokedFalse(tokenHash: String): Optional<RefreshTokenEntity>

    fun findByUserId(userId: Long): List<RefreshTokenEntity>

    fun deleteByUserId(userId: Long)
}
