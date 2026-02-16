package io.github.havonte1.tcgwatcher.backend.config

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor

class RateLimitingInterceptor(
    private val rateLimiterRegistry: RateLimiterRegistry
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val clientIp = request.remoteAddr
        val rateLimiter = rateLimiterRegistry.rateLimiter(clientIp)
        val permit = rateLimiter.acquirePermission()
        return permit
    }
}
