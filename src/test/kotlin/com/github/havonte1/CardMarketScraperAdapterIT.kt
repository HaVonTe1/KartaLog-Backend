package com.github.havonte1

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.microsoft.playwright.Playwright
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.DisabledIf

/**
 * Integration test for {@link CardMarketScraperAdapter} that runs with a real Playwright
 * instance against the live CardMarket website. The test verifies that a search
 * returns at least one {@link Product} and that the mandatory fields are populated.
 *
 * It uses the Spring Boot test context to obtain the bean, ensuring the fully
 * wired configuration works as expected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@TestPropertySource(properties = ["spring.liquibase.enabled=false"])
@Testcontainers
class CardMarketScraperAdapterIT {

    @Autowired
    private lateinit var scraper: CardMarketScraperPort

    companion object {
        @BeforeAll
        @JvmStatic
        fun checkPlaywrightAvailable() {
            try {
                Playwright.create().use { it.chromium().launch(
                com.microsoft.playwright.BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setArgs(listOf("--disable-blink-features=AutomationControlled"))
            ) }
            } catch (e: Exception) {
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
        val postgres = PostgreSQLContainer("postgres:15-alpine")
    }

    @DisabledIf()
    @Test
    fun `search returns products with required fields`() {

        val results: List<Product> = scraper.search("Pikachu")
        Assertions.assertTrue(results.isNotEmpty(), "Expected at least one product for search term 'Pikachu'")
        results.forEach { product ->
            Assertions.assertTrue(product.externalId > 0, "Product externalId must be positive")
            Assertions.assertTrue(product.setName?.isNotBlank() == true, "Product setName should not be blank")
            Assertions.assertTrue(product.rarity?.isNotBlank() == true, "Product rarity should not be blank")
            Assertions.assertTrue(
                product.imageUrl?.startsWith("http") == true,
                "Product imageUrl should be a valid URL"
            )
        }
    }
}
