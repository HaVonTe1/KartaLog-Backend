package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Unit test for {@link CardMarketScraperAdapter}.
 *
 * This test uses a simple in‑memory implementation of {@link CardMarketWebFetcherPort}
 * that reads the HTML fixture from disk, eliminating the need for Playwright
 * mocking and any network calls.
 */
class CardMarketScraperAdapterTest {

    @Test
    fun `search returns one product built from HTML`() {
        val resourcePath = "src/test/resources/pikachu_gallery_30.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")
        // Use a simple test implementation of CardMarketWebFetcherPort that reads the HTML file.
        class TestCardMarketWebFetcher : CardMarketWebFetcherPort {
            override fun fetch(
                searchString: String,
                locale: String,
                game: String
            ): String = Files.readString(Paths.get(resourcePath))
        }

        val testFetcher = TestCardMarketWebFetcher()
        val adapter: CardMarketScraperPort = CardMarketScraperAdapter(testFetcher)

        // ----- Execute the adapter -----

        val result: List<Product> = runBlocking {
            adapter.search(
                "Pikachu",
                locale = "de",
                game = "Pokemon"
            )
        }

        // ----- Verify -----
        assertEquals(30, result.size, "Exactly one product should be parsed")
        val first = result.first()
        // cmId should be the last segment of the parsed link
        assertEquals("Pikachu-V1-CEL005", first.cmId)
        assertEquals("https://product-images.s3.cardmarket.com/51/CEL/576750/576750.jpg", first.imgLink)
        assertEquals("Pikachu", first.names["de"])

        assertEquals("Pokemon", first.genre)
        assertEquals("Singles", first.type)
        assertEquals("1,50 €", first.price)
        assertEquals("?", first.priceTrendInfo?.value)
        assertTrue(first.priceTrendInfo?.valid == false)
        assertEquals("CEL 005", first.codeInfo?.value)
        assertTrue(first.codeInfo?.valid == true)
    }
}
