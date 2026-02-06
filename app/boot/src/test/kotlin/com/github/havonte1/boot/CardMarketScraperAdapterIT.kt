package com.github.havonte1.boot

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.microsoft.playwright.Playwright
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

/**
 * Integration test for {@link CardMarketScraperAdapter} that runs with a real Playwright
 * instance against the live CardMarket website. The test verifies that a search
 * returns at least one {@link Product} and that the mandatory fields are populated.
 *
 * It uses the Spring Boot test context to obtain the bean, ensuring the fully
 * wired configuration works as expected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ComponentScan(basePackages = ["com.github.havonte1"])
class CardMarketScraperAdapterIT {

    @Autowired
    private lateinit var scraper: CardMarketScraperPort

    companion object {
        @BeforeAll
        @JvmStatic
        fun checkPlaywrightAvailable() {
            // Playwright downloads the driver binaries on first use; ensure they are present.
            // If the driver cannot be launched, skip the test to avoid CI failures.
            try {
                // Attempt to create a Playwright instance and close it immediately.
                Playwright.create().use { it.chromium().launch() }
            } catch (e: Exception) {
                // Skip the test suite if Playwright cannot start (e.g., missing graphics env).
                Assumptions.assumeTrue(false, "Playwright cannot start: ${"$"} {e.message}")
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            // No explicit cleanup required; Playwright resources are closed by the adapter.
        }
    }

    @Test
    fun `search returns products with required fields`() {
        val results: List<Product> = scraper.search("Pikachu")
        // At least one result should be returned for a popular term.
        Assertions.assertTrue(results.isNotEmpty(), "Expected at least one product for search term 'Pikachu'")
        // Validate that each returned product has a positive externalId and non‑null fields.
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
