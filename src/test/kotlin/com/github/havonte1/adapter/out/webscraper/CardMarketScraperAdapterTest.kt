package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.github.havonte1.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter


import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import com.github.havonte1.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort

/**
 * Unit test for {@link CardMarketScraperAdapter}.
 *
 * The test mocks Playwright static calls so no real network traffic is performed.
 * It supplies a minimal HTML snippet that contains a single product card.
 */
class CardMarketScraperAdapterTest {

    @Test
    fun `search returns one product built from HTML`() {
        val resourcePath = "src/test/resources/pikachu_gallery_50.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen");
        // Use a simple test implementation of CardMarketWebFetcherPort that reads the HTML file.
        class TestCardMarketWebFetcher(private val resourcePath: String) : CardMarketWebFetcherPort {
            override fun fetch(searchString: String): String = Files.readString(Paths.get(resourcePath))
        }

        val testFetcher = TestCardMarketWebFetcher(resourcePath)
        val adapter: CardMarketScraperPort = CardMarketScraperAdapter(testFetcher)

        // ----- Execute the adapter -----

        val result: List<Product> = adapter.search("Pikachu")

        // ----- Verify -----
        assertEquals(30, result.size, "Exactly one product should be parsed")
        val first = result.first()
        assertEquals("/Pokemon/Products/Singles/Celebrations/Pikachu-V1-CEL005", first.cmId)
        assertEquals("https://product-images.s3.cardmarket.com/51/CEL/576750/576750.jpg", first.imageUrl)
        assertEquals("Pikachu", first.names["de"])

        assertEquals("/de/Pokemon/Products/Singles/Celebrations/Pikachu-V1-CEL005", first.cmLink)
        assertEquals("Pokemon", first.genre)
        assertEquals("Singles", first.type)
        assertEquals("0,99 €", first.price)
        assertEquals("?", first.priceTrendInfo?.value)
        assertTrue(first.priceTrendInfo?.valid == false)
        assertEquals("CEL 005", first.codeInfo?.value)
        assertTrue(first.codeInfo?.valid == true)
    }
}
