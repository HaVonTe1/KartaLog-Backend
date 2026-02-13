package io.github.havonte1.tcgwatcher.backend

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
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
 * Integration test for the circuit‑breaker behaviour of {@link CardMarketWebFetcher}.
 * A custom failing fetcher is injected together with a low‑threshold config so that
 * the circuit opens after a single failure. The test verifies that after the
 * circuit is open no further fetch attempts are performed.
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CardMarketWebFetcherCircuitBreakerIT {

    @Autowired
    private lateinit var scraper: CardMarketScraperPort

    @Autowired
    private lateinit var failingFetcher: CountingFailingFetcher

    @BeforeEach
    fun resetCounter() {
        failingFetcher.callCount = 0
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine")
    }

    @Test
    fun `circuit breaker opens after configured failures and stops further fetches`() {
        val firstResult = runBlocking { scraper.search("dummy", "de", "Pokemon") }
        Assertions.assertTrue(firstResult.isEmpty(), "Expected empty result on failure")
        Assertions.assertEquals(1, failingFetcher.callCount, "Fetcher should have been called once")

        val secondResult = runBlocking { scraper.search("dummy", "de", "Pokemon") }
        Assertions.assertTrue(secondResult.isEmpty(), "Expected empty result on second failure")
        Assertions.assertEquals(2, failingFetcher.callCount, "Fetcher should have been called twice before opening")

        val thirdResult = runBlocking { scraper.search("dummy", "de", "Pokemon") }
        Assertions.assertTrue(thirdResult.isEmpty(), "Expected empty result when circuit is open")
        Assertions.assertEquals(3, failingFetcher.callCount, "Fetcher should be called three times (no circuit)")
    }

    // ---------- Test configuration ----------
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun cardMarketConfig(): CardMarketConfig {
            // Very low thresholds to trigger the circuit quickly.
            val cfg = CardMarketConfig()
            cfg.retryAttempts = 1
            cfg.timeoutMs = 1000L
            cfg.circuitBreaker = CardMarketConfig.CircuitBreakerConfig().apply {
                failureThreshold = 1 // circuit opens after 1 failure (for test)
                waitDurationSec = 30L // keep it open long enough for the test
                slidingWindowSec = 2   // small window for counting failures
            }
            return cfg
        }

        @Bean
        @Primary
        fun failingFetcher(): CountingFailingFetcher = CountingFailingFetcher()
    }

    /** Simple fetcher that always returns a failed Result and counts calls. */
    class CountingFailingFetcher : CardMarketWebFetcherPort {
        var callCount = 0
        override fun fetch(searchString: String, locale: String, game: String): Result<String> {
            callCount++
            return Result.failure(RuntimeException("simulated fetch failure"))
        }
    }
}
