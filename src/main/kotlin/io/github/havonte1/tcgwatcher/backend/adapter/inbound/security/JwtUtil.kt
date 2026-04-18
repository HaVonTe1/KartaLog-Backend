package io.github.havonte1.tcgwatcher.backend.adapter.inbound.security

import java.security.MessageDigest

object JwtUtil {
    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
