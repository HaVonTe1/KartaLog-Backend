package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.User
import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

@Service
class JwtTokenService(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,
    @Value("\${jwt.access-expiration-ms:900000}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-expiration-ms:604800000}")
    private val refreshTokenExpiration: Long,
) {
    private val logger = KotlinLogging.logger {}
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    fun generateAccessToken(user: User): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpiration)
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(): String {
        return java.util.UUID.randomUUID().toString()
    }

    fun validateAccessToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            logger.debug { "JWT validation failed: ${e.message}" }
            false
        }
    }

    fun getUserIdFromToken(token: String): Long {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject.toLong()
    }

    fun getEmailFromToken(token: String): String {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.get("email", String::class.java)
    }

    fun getRoleFromToken(token: String): String {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.get("role", String::class.java)
    }

    fun getAccessTokenExpiration(): Long = accessTokenExpiration
}
