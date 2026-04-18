package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import io.github.havonte1.tcgwatcher.backend.domain.model.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.io.Serializable

@Audited
@Entity
@Table(name = "users", schema = "watcher")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "email", nullable = false, unique = true)
    val email: String,
    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: Role = Role.USER,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    @PrePersist
    fun onPrePersist() {
        val now = System.currentTimeMillis()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = System.currentTimeMillis()
    }

    constructor() : this(0, "", "", Role.USER)
}
