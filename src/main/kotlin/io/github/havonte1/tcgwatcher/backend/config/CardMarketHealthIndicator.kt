package io.github.havonte1.tcgwatcher.backend.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class CardMarketHealthIndicator(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) : HealthIndicator {
    private val logger = KotlinLogging.logger {}

    override fun health(): Health {
        return try {
            val circuitBreaker = circuitBreakerRegistry.circuitBreaker("cardMarketCircuitBreaker")
            val state = circuitBreaker.state

            when (state) {
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED -> {
                    Health.up()
                        .withDetail("circuitBreaker", "CLOSED")
                        .withDetail("status", "Operational")
                        .build()
                }
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN -> {
                    Health.down()
                        .withDetail("circuitBreaker", "OPEN")
                        .withDetail("status", "CardMarket unavailable - circuit breaker open")
                        .build()
                }
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN -> {
                    Health.up()
                        .withDetail("circuitBreaker", "HALF_OPEN")
                        .withDetail("status", "Testing recovery")
                        .build()
                }
                else -> {
                    Health.unknown()
                        .withDetail("circuitBreaker", state.name)
                        .build()
                }
            }
        } catch (e: IllegalStateException) {
            logger.warn(e) { "Error checking circuit breaker health" }
            Health.unknown()
                .withDetail("error", e.message ?: "Unknown error")
                .build()
        }
    }
}
