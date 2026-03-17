package io.github.havonte1.tcgwatcher.backend.util

import com.fasterxml.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.util.Base64

object ETagUtil {
    private val mapper = ObjectMapper()
    private val digest = MessageDigest.getInstance("SHA-256")

    fun computeWeakETag(value: Any): String {
        val json = mapper.writeValueAsBytes(value)
        val hash = digest.digest(json)
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        return b64
    }
}
