package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.havonte1.kartalog.backend.domain.model.SearchResult
import io.github.havonte1.kartalog.backend.domain.port.out.CardMarketScraperPort
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class CardMarketScraperAdapterTest {
    private val locale: Locale = Locale.GERMAN
    private val genre: Genre = Genre.POKEMON

    @Test
    fun `search returns products built from HTML`() {
        val resourcePath = "src/test/resources/pikachu_gallery_30.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")

        class TestCardMarketWebFetcher : CardMarketWebFetcherPort {
            override suspend fun fetch(
                searchString: String,
                locale: Locale,
                genre: Genre,
                page: Int,
            ): Result<String> = Result.success(Files.readString(Paths.get(resourcePath)))

            override suspend fun fetchDetails(
                cmId: String,
                genre: Genre,
                type: ProductType,
                locale: Locale,
                setname: String,
            ): Result<String> = Result.failure(UnsupportedOperationException("Not implemented"))
        }

        val testFetcher = TestCardMarketWebFetcher()
        val adapter: CardMarketScraperPort = CardMarketScraperAdapter(testFetcher)

        val result: SearchResult =
            runBlocking {
                adapter.search("Pikachu", locale, genre)
            }

        assertEquals(30, result.products.size, "Exactly 30 products should be parsed")
        val first = result.products.first()
        assertEquals("Pikachu-V1-CEL005", first.cmId)
        assertEquals("https://product-images.s3.cardmarket.com/51/CEL/576750/576750.jpg", first.imgLink)
        assertEquals("Pikachu", first.names[Locale.GERMAN])

        assertEquals("Pokemon", first.genre.pathParam)
        assertEquals("Singles", first.type.cmIdentifier)
        assertEquals("1,50 €", first.price)
        assertEquals("CEL 005", first.codeInfo?.value)
        assertTrue(first.codeInfo?.valid == true)
    }
}
