package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ninjasquad.springmockk.MockkBean
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketDetailsParser
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketGalleryParser
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketProductMapper
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcher
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.application.SearchResponse
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.config.GenreConfig
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.AllAnyMatcher
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.fail

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
@ActiveProfiles(value = ["test"])
class CollectablesAdapterIT {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

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


    @MockkBean
    lateinit var webFetcher: CardMarketWebFetcherPort

    val objectMapper = ObjectMapper().registerKotlinModule()


    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
        cacheManager.getCacheNames().forEach { cacheManager.getCache(it)?.clear() }
    }

    @Test
    fun `repeated seaches with different result`() = runBlocking {
        coEvery { webFetcher.fetch("Pikachu", Locale.GERMAN, Genre.POKEMON, 1) } returnsMany
            listOf(
                Result.success( Files.readString(Paths.get( "src/test/resources/pikachu_gallery_size30_v1.html"))),
                Result.success( Files.readString(Paths.get( "src/test/resources/pikachu_gallery_size30_v2.html")))
            )

        val firstResult: SearchResponse = service.search("Pikachu", Locale.GERMAN, Genre.POKEMON)
        assertEquals(30, firstResult.products.size)

        firstResult.products.find { it.externalId == 576754L }?.let {
            assertEquals("Pikachu-V5-CEL009", it.cmId)
            assertEquals("1,50 €", it.price)
        } ?: fail("No element with externalId=576754 found")

        // Clear cache to simulate time passing and allow fresh scraping
        cacheManager.getCache("listCache")?.clear()

        //repeated search to a later time results in same products BUT with different prices
        val secondResult: SearchResponse = service.search("Pikachu", Locale.GERMAN, Genre.POKEMON)
        assertEquals(30, secondResult.products.size)

        secondResult.products.find { it.externalId == 576754L }?.let {
            assertEquals("Pikachu-V5-CEL009", it.cmId)
            assertEquals("4,50 €", it.price)
        } ?: fail("No element with externalId=576754 found")
    }

    @Test
    fun `GET collectables returns cached result on repeated search with same params`() {
        coEvery { webFetcher.fetch("pikachu", locale = Locale.GERMAN, Genre.POKEMON, 1) } returns
            Result.success( Files.readString(Paths.get( "src/test/resources/pikachu_gallery_size30_v1.html")))

        mockMvc
            .get("/collectables/") {
                param("query", "pikachu")
                param("locale", Locale.GERMAN.code)
                param("game", Genre.POKEMON.identifier)
            }.andExpect { request { asyncStarted() } }
            .andReturn()
            .let {
                mockMvc
                    .perform(
                        MockMvcRequestBuilders.asyncDispatch(it),
                    ).andReturn()
            }

        val mvcResult =
            mockMvc
                .get("/collectables/") {
                    param("query", "pikachu")
                    param("locale", Locale.GERMAN.code)
                    param("game", Genre.POKEMON.identifier)
                }.andExpect { request { asyncStarted() } }
                .andReturn()

        val dispatched =
            mockMvc
                .perform(
                    MockMvcRequestBuilders.asyncDispatch(mvcResult),
                ).andReturn()

        assertEquals(200, dispatched.response.status)

        coVerify(exactly = 1) { webFetcher.fetch(
            any(), any(), any(), any()
        ) }
        val products: List<ProductDTO> = objectMapper.readValue(
            dispatched.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, ProductDTO::class.java),
        )
        assertEquals(30, products.size, "Expected 30 products from cache")
    }

    @Test
    fun `GET collectables with overlapping search params produces no duplicate products`() {
        coEvery { webFetcher.fetch("glurak", locale = Locale.GERMAN, Genre.POKEMON, 1) } returns
                Result.success( Files.readString(Paths.get( "src/test/resources/glurak_de_100_page1_minimal.html")))
        coEvery { webFetcher.fetch("glurak", locale = Locale.GERMAN, Genre.POKEMON, 2) } returns
                Result.success( Files.readString(Paths.get( "src/test/resources/glurak_de_100_page2_minimal.html")))
        coEvery { webFetcher.fetch("glurak", locale = Locale.GERMAN, Genre.POKEMON, 3) } returns
                Result.success( Files.readString(Paths.get( "src/test/resources/glurak_de_100_page3_minimal.html")))
        coEvery { webFetcher.fetch("glurak", locale = Locale.GERMAN, Genre.POKEMON, 4) } returns
                Result.success( Files.readString(Paths.get( "src/test/resources/glurak_de_100_page4_minimal.html")))
        coEvery { webFetcher.fetch("glurak x", locale = Locale.GERMAN, Genre.POKEMON, 1) } returns
            Result.success( Files.readString(Paths.get( "src/test/resources/glurak_x_de_100_page1_minimal.html")))


        val mvcResultGlurak =
            mockMvc
                .get("/collectables/") {
                    param("query", "glurak")
                    param("locale", Locale.GERMAN.code)
                    param("game", Genre.POKEMON.identifier)
                }.andExpect { request { asyncStarted() } }
                .andReturn()

        val dispatchedGlurak =
            mockMvc
                .perform(
                    MockMvcRequestBuilders.asyncDispatch(mvcResultGlurak),
                ).andReturn()

        assertEquals(200, dispatchedGlurak.response.status)
        val initproducts: List<ProductDTO> = objectMapper.readValue(
            dispatchedGlurak.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, ProductDTO::class.java),
        )
        assertEquals(40, initproducts.size, "Expected 30 products from cache")

        assertEquals("4,00 €", initproducts.first { it.externalId == 18L }.price)

        val mvcResult =
            mockMvc
                .get("/collectables/") {
                    param("query", "glurak x")
                    param("locale", Locale.GERMAN.code)
                    param("game", Genre.POKEMON.identifier)
                }.andExpect { request { asyncStarted() } }
                .andReturn()

        val dispatched =
            mockMvc
                .perform(
                    MockMvcRequestBuilders.asyncDispatch(mvcResult),
                ).andReturn()

        assertEquals(200, dispatched.response.status)
        val products: List<ProductDTO> = objectMapper.readValue(
            dispatched.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, ProductDTO::class.java),
        )
        val uniqueCmIds = products.map { it.cmId }.distinct()
        assertEquals(
            products.size,
            uniqueCmIds.size,
            "Expected no duplicate products, but found ${products.size - uniqueCmIds.size} duplicates",
        )

        assertEquals("4,10 €", products.first { it.externalId == 18L }.price)

    }

}
