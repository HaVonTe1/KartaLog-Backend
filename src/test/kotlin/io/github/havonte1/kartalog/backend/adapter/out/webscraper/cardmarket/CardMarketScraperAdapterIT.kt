package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import com.ninjasquad.springmockk.MockkBean
import io.github.havonte1.kartalog.backend.application.SearchUseCase
import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.Product
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.havonte1.kartalog.backend.domain.model.SearchResult
import io.github.havonte1.kartalog.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.kartalog.backend.domain.port.out.ProductRepository
import io.github.havonte1.kartalog.backend.domain.port.out.SearchResultRepository
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Paths

@Suppress("LongMethod", "MaxLineLength")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@ActiveProfiles(value = ["test"])
class CardMarketScraperAdapterIT {
    @Autowired
    private lateinit var scraper: CardMarketScraperAdapter

    @MockkBean
    lateinit var webFetcher: CardMarketWebFetcherPort

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var service: SearchUseCase

    @Autowired
    lateinit var searchRepo: SearchResultRepository

    @Autowired
    lateinit var productRepo: ProductRepository

    @Autowired
    lateinit var scraperPort: CardMarketScraperPort

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)
    }

    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
        cacheManager.getCacheNames().forEach { cacheManager.getCache(it)?.clear() }
    }

    @Test
    fun `search returns products with German locale`() {
        coEvery { webFetcher.fetch("Pikachu", locale = Locale.GERMAN, Genre.POKEMON, 1) } returns
            Result.success(Files.readString(Paths.get("src/test/resources/pikachu_gallery_size30_v1.html")))
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.GERMAN, Genre.POKEMON)
            }
        assertThat(result.products).isNotEmpty
        val first = result.products.first()
        assertThat(first.cmId).isNotNull
        assertThat(first.names).containsKey(Locale.GERMAN)
    }

    @Test
    @Disabled("no english source file available")
    fun `search returns products with English locale`() {
        coEvery { webFetcher.fetch("glurak", locale = Locale.ENGLISH, Genre.POKEMON, 1) } returns
            Result.success(Files.readString(Paths.get("src/test/resources/pikachu_gallery_40.html")))

        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.ENGLISH, Genre.POKEMON)
            }
        assertThat(result.products).isNotEmpty
    }

    @Test
    @Disabled("no french source file available")
    fun `search returns products with French locale`() {
        coEvery { webFetcher.fetch("glurak", locale = Locale.FRENCH, Genre.POKEMON, 1) } returns
            Result.success(Files.readString(Paths.get("src/test/resources/pikachu_gallery_30.html")))
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.FRENCH, Genre.POKEMON)
            }
        assertThat(result.products).isNotEmpty
    }

    @Test
    fun `fetchProductDetails returns product with German locale`() {
        coEvery { webFetcher.fetchDetails("12345678", locale = Locale.GERMAN, genre = Genre.POKEMON, type = ProductType.SINGLES, setname = "BaseSet") } returns
            Result.success(Files.readString(Paths.get("src/test/resources/evoli_details_stripped.html")))
        val result: Product? =
            runBlocking {
                scraper.fetchProductDetails("12345678", Genre.POKEMON, ProductType.SINGLES, Locale.GERMAN, "BaseSet")
            }
        assertThat(result).isNotNull
        assertThat(result!!.names).containsKey(Locale.GERMAN)
    }
}
