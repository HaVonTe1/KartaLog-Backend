package io.github.havonte1.tcgwatcher.backend.domain.model

enum class Role {
    USER,
    ADMIN,
}

data class User(
    val id: Long? = null,
    val email: String,
    val passwordHash: String,
    val role: Role = Role.USER,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
