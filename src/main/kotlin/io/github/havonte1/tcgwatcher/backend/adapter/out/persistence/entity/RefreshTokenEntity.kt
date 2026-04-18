package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.io.Serializable
import java.time.Instant

@Audited
@Entity
@Table(name = "refresh_tokens", schema = "watcher")
data class RefreshTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "revoked", nullable = false)
    val revoked: Boolean = false,
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: UserEntity? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    constructor() : this(0, 0, "", Instant.now())
}
