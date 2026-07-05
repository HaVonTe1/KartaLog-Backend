package io.github.havonte1.kartalog.backend.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.health.contributor.Status

class CardMarketHealthIndicatorTest {
    @Test
    fun `health returns UP when circuit breaker is CLOSED`() {
        val circuitBreakerRegistry: CircuitBreakerRegistry = mockk()
        val circuitBreaker: CircuitBreaker = mockk()

        every { circuitBreakerRegistry.circuitBreaker("cardMarketCircuitBreaker") } returns circuitBreaker
        every { circuitBreaker.state } returns CircuitBreaker.State.CLOSED

        val indicator = CardMarketHealthIndicator(circuitBreakerRegistry)
        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals("CLOSED", health.details["circuitBreaker"])
    }

    @Test
    fun `health returns DOWN when circuit breaker is OPEN`() {
        val circuitBreakerRegistry: CircuitBreakerRegistry = mockk()
        val circuitBreaker: CircuitBreaker = mockk()

        every { circuitBreakerRegistry.circuitBreaker("cardMarketCircuitBreaker") } returns circuitBreaker
        every { circuitBreaker.state } returns CircuitBreaker.State.OPEN

        val indicator = CardMarketHealthIndicator(circuitBreakerRegistry)
        val health = indicator.health()

        assertEquals(Status.DOWN, health.status)
        assertEquals("OPEN", health.details["circuitBreaker"])
    }

    @Test
    fun `health returns UP when circuit breaker is HALF_OPEN`() {
        val circuitBreakerRegistry: CircuitBreakerRegistry = mockk()
        val circuitBreaker: CircuitBreaker = mockk()

        every { circuitBreakerRegistry.circuitBreaker("cardMarketCircuitBreaker") } returns circuitBreaker
        every { circuitBreaker.state } returns CircuitBreaker.State.HALF_OPEN

        val indicator = CardMarketHealthIndicator(circuitBreakerRegistry)
        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals("HALF_OPEN", health.details["circuitBreaker"])
    }
}
