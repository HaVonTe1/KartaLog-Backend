package io.github.havonte1.tcgwatcher.backend

import com.ninjasquad.springmockk.MockkBean
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcher
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.application.SearchResponse
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.fail

@SpringBootTest
@Testcontainers
@Tag("integration")
@AutoConfigureCache
@ActiveProfiles(value = ["test"])
class CollectablesServiceIT {
    private val locale: Locale = Locale.GERMAN
    private val genre: Genre = Genre.POKEMON
    private val testFilePikachuV1 = "src/test/resources/pikachu_gallery_size30_v1.html"
    private val testFilePikachuV2 = "src/test/resources/pikachu_gallery_size30_v2.html"


    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)
    }

    @Autowired
    lateinit var service: SearchUseCase

    @Autowired
    lateinit var searchRepo: SearchResultRepository

    @Autowired
    lateinit var productRepo: ProductRepository

    @Autowired
    lateinit var scraperPort: CardMarketScraperPort

    @Autowired
    lateinit var cacheManager: CacheManager

    @MockkBean
    lateinit var webFetcher: CardMarketWebFetcherPort

    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
    }

    @Test
    fun `repeated seaches with different result`() = runBlocking {
        coEvery { webFetcher.fetch("Pikachu", locale, genre, 1) } returnsMany
            listOf(
                Result.success( Files.readString(Paths.get( testFilePikachuV1))),
                Result.success( Files.readString(Paths.get( testFilePikachuV2)))
            )

        val firstResult: SearchResponse = service.search("Pikachu", locale, genre)
        assertEquals(30, firstResult.products.size)

        firstResult.products.find { it.externalId == 576753L }?.let {
            assertEquals("Pikachu-V4-CEL008", it.cmId)
            assertEquals("1,50 €", it.price)
        } ?: fail("No element with externalId=576753 found")

        // Clear cache to simulate time passing and allow fresh scraping
        cacheManager.getCache("listCache")?.clear()

        //repeated search to a later time results in same products BUT with different prices
        val secondResult: SearchResponse = service.search("Pikachu", locale, genre)
        assertEquals(30, secondResult.products.size)

        secondResult.products.find { it.externalId == 576753L }?.let {
            assertEquals("Pikachu-V4-CEL008", it.cmId)
            assertEquals("4,00 €", it.price)
        } ?: fail("No element with externalId=576753 found")
    }

    @Test
    fun `products are unique by externalId`() {
        val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        Assumptions.assumeTrue(File(testFilePikachu30).exists())

        val firstResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, firstResult.products.size)
        assertEquals(30, productRepo.findAll().size)

        val secondResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, secondResult.products.size)
        assertEquals(1, searchRepo.countByQuery("Pikachu"))
    }

    @Test
    fun `products are updated when prices change`() {
        val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"
        Assumptions.assumeTrue(File(testFilePikachu40).exists())

        runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, productRepo.findAll().size)

        val secondResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, secondResult.products.size)
    }

    @Test
    fun `search results are cached correctly`() {
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_30.html").exists())

        runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(1, searchRepo.countByQuery("Pikachu"))

        val cachedResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, cachedResult.products.size)
        assertEquals(1, searchRepo.countByQuery("Pikachu"))
    }

    @Test
    fun `products maintain their relationships after caching`() {
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_30.html").exists())

        val firstSearch: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        val productId = firstSearch.products.first().externalId

        val cachedSearch: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        val cachedProduct = cachedSearch.products.find { it.externalId == productId }

        assertNotNull(cachedProduct)
        assertEquals(firstSearch.products.first().cmId, cachedProduct?.cmId)
    }
}
