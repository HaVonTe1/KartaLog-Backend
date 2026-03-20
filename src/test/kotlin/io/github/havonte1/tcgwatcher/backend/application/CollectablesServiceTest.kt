package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException

class CollectablesServiceTest {

    private val mockScraperPort: CardMarketScraperPort = mockk()
    private val mockSearchResultRepository: SearchResultRepository = mockk()
    private val mockProductRepository: ProductRepository = mockk()

    private lateinit var service: CollectablesService

    @BeforeEach
    fun setUp() {
        service = CollectablesService(
            mockScraperPort,
            mockSearchResultRepository,
            productRepository = mockProductRepository
        )
    }

    @Test
    fun `search returns products from scraper`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        val products = listOf(
            Product(
                externalId = 1L,
                cmId = "CM001",
                names = mapOf("de" to "Pikachu"),
                genre = "Pokemon",
                type = "Singles",
                price = "1,50 €"
            )
        )

        every { mockSearchResultRepository.findByQuery(query) } returns null
        coEvery { mockScraperPort.search(query, locale, game) } returns products
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(1, result.size)
        assertEquals("CM001", result[0].cmId)
    }

    @Test
    fun `search handles empty results from scraper`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        val emptyProducts: List<Product> = listOf()

        every { mockSearchResultRepository.findByQuery(query) } returns null
        coEvery { mockScraperPort.search(query, locale, game) } returns emptyProducts
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(0, result.size)
    }

    @Test
    fun `fetchProductDetails uses stored cmCode when setname is blank`() {
        val cmId = "CM001"
        val genre = "Pokemon"
        val type = "Singles"
        val lang = "de"
        val storedCmCode = "base1"

        val existingProduct = Product(
            externalId = 1L,
            cmId = cmId,
            genre = genre,
            type = type,
            set = ProductSet(setId = 1L, cmCode = storedCmCode)
        )

        val scrapedProduct = Product(
            externalId = 1L,
            cmId = cmId,
            genre = genre,
            type = type,
            price = "10,00 €",
            set = ProductSet(setId = 1L, cmCode = storedCmCode)
        )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct
        coEvery {
            mockScraperPort.fetchProductDetails(
                cmId,
                genre,
                type,
                lang,
                storedCmCode
            )
        } returns scrapedProduct
        every { mockProductRepository.save(any()) } returns scrapedProduct

        val result = runBlocking {
            service.fetchProductDetails(cmId, genre, type, lang, "")
        }

        assertEquals(cmId, result?.cmId)
    }

    @Test
    fun `fetchProductDetails throws 400 when setname is blank and no stored product exists`() {
        val cmId = "UNKNOWN001"
        val genre = "Pokemon"
        val type = "Singles"
        val lang = "de"

        every { mockProductRepository.findByCmId(cmId) } returns null

        val exception = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                service.fetchProductDetails(cmId, genre, type, lang, "")
            }
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("no setname provided", exception.reason)
    }

    @Test
    fun `fetchProductDetails throws 400 when setname is blank and product has no cmCode`() {
        val cmId = "CM002"
        val genre = "Pokemon"
        val type = "Singles"
        val lang = "de"

        val existingProduct = Product(
            externalId = 2L,
            cmId = cmId,
            genre = genre,
            type = type,
            set = null
        )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct

        val exception = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                service.fetchProductDetails(cmId, genre, type, lang, "")
            }
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("no setname provided", exception.reason)
    }
}
