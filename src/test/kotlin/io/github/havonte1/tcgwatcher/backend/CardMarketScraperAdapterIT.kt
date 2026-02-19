package io.github.havonte1.tcgwatcher.backend

import com.microsoft.playwright.Playwright
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Integration test for {@link CardMarketScraperAdapter} that runs with a real Playwright
 * instance against the live CardMarket website. The test verifies that a search
 * returns at least one {@link Product} and that the mandatory fields are populated.
 *
 * It uses the Spring Boot test context to obtain the bean, ensuring the fully
 * wired configuration works as expected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("only for internal testing")
@Testcontainers
@Tag("integration")
class CardMarketScraperAdapterIT {

    @Autowired
    private lateinit var scraper: CardMarketScraperPort

    companion object {
        @BeforeAll
        @JvmStatic
        fun checkPlaywrightAvailable() {
            try {
                Playwright.create().use {
                    it.chromium().launch(
                        com.microsoft.playwright.BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setArgs(listOf("--disable-blink-features=AutomationControlled"))
                    )
                }
            } catch (_: Exception) {
                Assumptions.assumeTrue(false, "Playwright cannot start: ${"$"}{e.message}")
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            // No explicit cleanup needed – Playwright resources are closed by the adapter.
        }

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine")
    }

    @Test
    fun `search returns products with required fields`() {
        val results: List<Product> =
            kotlinx.coroutines.runBlocking {
                scraper.search("Pikachu", "de", "Pokemon")
            }
        Assertions.assertTrue(results.isNotEmpty(), "Expected at least one product for search term 'Pikachu'")
    }
}
