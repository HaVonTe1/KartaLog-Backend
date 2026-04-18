package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.RefreshTokenEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.UserEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.RefreshTokenJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.UserJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.security.JwtUtil
import io.github.havonte1.tcgwatcher.backend.domain.model.Role
import io.github.havonte1.tcgwatcher.backend.domain.model.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthenticationService(
    private val userRepository: UserJpaRepository,
    private val refreshTokenRepository: RefreshTokenJpaRepository,
    private val jwtTokenService: JwtTokenService,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 7L
    }
    @Transactional
    fun register(email: String, password: String): User {
        require(!userRepository.existsByEmail(email)) { "Email already registered" }
        val encoded = passwordEncoder.encode(password)
        requireNotNull(encoded) { "Failed to encode password" }
        val userEntity = UserEntity(
            email = email,
            passwordHash = encoded,
            role = Role.USER,
        )
        val saved = userRepository.save(userEntity)
        return saved.toDomain()
    }

    @Transactional
    fun authenticate(email: String, password: String): User {
        val userEntity = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Invalid email or password") }
        require(passwordEncoder.matches(password, userEntity.passwordHash)) {
            "Invalid email or password"
        }
        return userEntity.toDomain()
    }

    @Transactional
    fun generateTokens(user: User): AuthResponse {
        val accessToken = jwtTokenService.generateAccessToken(user)
        val refreshTokenStr = jwtTokenService.generateRefreshToken()
        val tokenHash = JwtUtil.hashToken(refreshTokenStr)
        val expiresAt = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS)

        val refreshToken = RefreshTokenEntity(
            userId = user.id!!,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            revoked = false,
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenStr,
            expiresIn = jwtTokenService.getAccessTokenExpiration(),
            user = UserResponse(
                id = user.id!!,
                email = user.email,
                role = user.role.name,
            ),
        )
    }

    @Transactional
    fun refreshToken(refreshTokenStr: String): AuthResponse {
        val tokenHash = JwtUtil.hashToken(refreshTokenStr)
        val refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
            .orElseThrow { IllegalArgumentException("Invalid refresh token") }

        require(!refreshToken.revoked) { "Token has been revoked" }
        require(refreshToken.expiresAt.isAfter(Instant.now())) { "Refresh token expired" }

        val userEntity = refreshToken.user
            ?: throw IllegalStateException("User not found for refresh token")

        refreshTokenRepository.delete(refreshToken)

        val user = userEntity.toDomain()
        return generateTokens(user)
    }

    @Transactional
    fun logout(userId: Long, refreshTokenStr: String) {
        val tokenHash = JwtUtil.hashToken(refreshTokenStr)
        val refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
            .orElse(null)
        refreshToken?.let {
            val revoked = it.copy(revoked = true)
            refreshTokenRepository.save(revoked)
        }
        refreshTokenRepository.deleteByUserId(userId)
    }

    fun findById(userId: Long): User? {
        return userRepository.findById(userId).orElse(null)?.toDomain()
    }

    private fun UserEntity.toDomain() = User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val role: String,
)
