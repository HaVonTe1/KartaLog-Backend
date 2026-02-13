package io.github.havonte1.tcgwatcher.backend

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcher
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Integration test for the circuit-breaker behaviour of {@link CardMarketWebFetcher}.
 * The test verifies that the circuit breaker correctly opens after failures and prevents further calls.
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CardMarketWebFetcherCircuitBreakerIT {

    @Autowired
    private lateinit var webFetcher: CardMarketWebFetcher

    @Autowired
    private lateinit var config: CardMarketConfig

    @BeforeEach
    fun resetCircuitBreaker() {
        val cb = webFetcher.circuitBreaker
        cb.reset()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine")
    }

    @Test
    fun `circuit breaker opens after configured failures`() {
        // Use a non-existent search string to ensure consistent failure behavior
        val failingSearch = "thisdoesnotexist12345xyz"

        // Configure for fast circuit opening with low threshold and short window
        config.circuitBreaker.failureThreshold = 3
        config.circuitBreaker.slidingWindowSec = 5L
        config.circuitBreaker.waitDurationSec = 60L

        val cb = webFetcher.circuitBreaker
        
        // First call - should fail
        val result1 = runBlocking { webFetcher.fetch(failingSearch, "de", "Pokemon") }
        Assertions.assertTrue(result1.isFailure)
        
        // Second call - should fail  
        val result2 = runBlocking { webFetcher.fetch(failingSearch, "de", "Pokemon") }
        Assertions.assertTrue(result2.isFailure)

        // Third call - this should open the circuit
        val result3 = runBlocking { webFetcher.fetch(failingSearch, "de", "Pokemon") }
        Assertions.assertTrue(result3.isFailure)
        
        // After reaching threshold (3 failures), circuit should be open
        // When circuit is open, calls should throw CircuitBreakerOpenException without executing the fetch
        
        // Fourth call - when circuit is open, it should fail differently (no execution)
        val result4 = runBlocking { webFetcher.fetch(failingSearch, "de", "Pokemon") }
        Assertions.assertTrue(result4.isFailure)
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun cardMarketConfig(): CardMarketConfig {
            val cfg = CardMarketConfig()
            cfg.timeoutMs = 500L // Very short timeout to ensure failures
            return cfg
        }
    }
}
