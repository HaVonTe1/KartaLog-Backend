package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.config.GenreConfig
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.LanguagePricing
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductAttribute
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductAttributeType
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException

class CollectablesServiceTest {
    private val mockScraperPort: CardMarketScraperPort = mockk()
    private val mockSearchResultRepository: SearchResultRepository = mockk()
    private val mockProductRepository: ProductRepository = mockk()

    private lateinit var service: CollectablesService

    private val locale: Locale = Locale.GERMAN
    private val genre = Genre.POKEMON

    @BeforeEach
    fun setUp() {
        service =
            CollectablesService(
                mockScraperPort,
                mockSearchResultRepository,
                productRepository = mockProductRepository,
            )
    }

    @Test
    fun `search returns SearchResponse with products from scraper`() {
        val query = "Pikachu"
        val products =
            listOf(
                Product(
                    externalId = 1L,
                    cmId = "CM001",
                    names = mapOf(Locale.GERMAN to "Pikachu"),
                    genre = Genre.POKEMON,
                    type = ProductType.SINGLES,
                    price = "1,50 €",
                ),
            )

        every { mockSearchResultRepository.findByQueryLocaleAndGenre(query, locale.code, genre.identifier) } returns null
        coEvery {
            mockScraperPort.search(
                query,
                locale,
                genre,
            )
        } returns SearchResult(query = query, language = locale.code, genre = genre.identifier, products = products)
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result =
            runBlocking {
                service.search(query, locale, genre)
            }

        assertNotNull(result)
        assertEquals(1, result.products.size)
        assertEquals("CM001", result.products[0].cmId)
    }

    @Test
    fun `search passes filters to scraper`() {
        val query = "Pikachu"

        every { mockSearchResultRepository.findByQueryLocaleAndGenre(query, locale.code, genre.identifier) } returns null
        coEvery {
            mockScraperPort.search(query, locale, genre)
        } returns SearchResult(query = query, language = locale.code, genre = genre.identifier, products = emptyList())
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result =
            runBlocking {
                service.search(query, locale, genre)
            }

        assertNotNull(result)
    }

    @Test
    fun `search handles empty results from scraper`() {
        val query = "NonExistentCard"
        val emptyProducts: List<Product> = listOf()

        every { mockSearchResultRepository.findByQueryLocaleAndGenre(query, locale.code, genre.identifier) } returns null
        coEvery {
            mockScraperPort.search(
                query,
                locale,
                genre,
            )
        } returns SearchResult(query = query, language = locale.code, genre = genre.identifier, products = emptyProducts)
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result =
            runBlocking {
                service.search(query, locale, genre)
            }

        assertNotNull(result)
        assertEquals(0, result.products.size)
    }

    @Test
    fun `fetchProductDetails uses stored cmCode when setname is blank`() {
        val cmId = "CM001"
        val type = ProductType.SINGLES
        val storedCmCode = "base1"

        val existingProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
            )

        val scrapedProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                price = "10,00 €",
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
            )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct
        coEvery {
            mockScraperPort.fetchProductDetails(
                cmId,
                genre,
                type,
                locale,
                storedCmCode,
            )
        } returns scrapedProduct
        every { mockProductRepository.save(any()) } returns scrapedProduct

        val result =
            runBlocking {
                service.fetchProductDetails(cmId, genre, type, locale, "")
            }

        assertEquals(cmId, result?.cmId)
    }

    @Test
    fun `fetchProductDetails throws 400 when setname is blank and no stored product exists`() {
        val cmId = "UNKNOWN001"
        val genre = Genre.POKEMON
        val type = ProductType.SINGLES
        val lang = Locale.GERMAN

        every { mockProductRepository.findByCmId(cmId) } returns null

        val exception =
            assertThrows(ResponseStatusException::class.java) {
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
        val genre = Genre.POKEMON
        val type = ProductType.SINGLES
        val lang = Locale.GERMAN

        val existingProduct =
            Product(
                externalId = 2L,
                cmId = cmId,
                genre = genre,
                type = type,
                set = null,
            )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                runBlocking {
                    service.fetchProductDetails(cmId, genre, type, lang, "")
                }
            }

        assertEquals(400, exception.statusCode.value())
        assertEquals("no setname provided", exception.reason)
    }

    @Test
    fun `fetchProductDetails returns product with languagePricing populated`() {
        val cmId = "CM001"
        val type = ProductType.SINGLES
        val storedCmCode = "base1"

        val existingProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
            )

        val languagePricing =
            listOf(
                LanguagePricing(Locale.GERMAN, "10,00 €", "9,50 €", true),
                LanguagePricing(Locale.ENGLISH, "12,00 €", "11,00 €", true),
            )

        val scrapedProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                price = "10,00 €",
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
                languagePricing = languagePricing,
            )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct
        coEvery {
            mockScraperPort.fetchProductDetails(
                cmId,
                genre,
                type,
                locale,
                storedCmCode,
            )
        } returns scrapedProduct
        every { mockProductRepository.save(any()) } returns scrapedProduct

        val result =
            runBlocking {
                service.fetchProductDetails(cmId, genre, type, locale, storedCmCode)
            }

        assertNotNull(result)
        assertEquals(2, result?.languagePricing?.size)
        assertEquals("10,00 €", result?.languagePricing?.first { it.locale == Locale.GERMAN }?.price)
    }

    @Test
    fun `fetchProductDetails returns product with productAttributes populated`() {
        val cmId = "CM001"
        val type = ProductType.SINGLES
        val storedCmCode = "base1"

        val existingProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
            )

        val productAttributes =
            listOf(
                ProductAttribute("rarity", "Rare Holo", ProductAttributeType.RARITY),
                ProductAttribute("2023-03-03", "2023-03-03", ProductAttributeType.RELEASE_DATE),
                ProductAttribute("049/197", "049/197", ProductAttributeType.CARD_NUMBER),
            )

        val scrapedProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                price = "10,00 €",
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
                productAttributes = productAttributes,
                releaseDate = "2023-03-03",
                cardNumber = "049/197",
            )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct
        coEvery {
            mockScraperPort.fetchProductDetails(
                cmId,
                genre,
                type,
                locale,
                storedCmCode,
            )
        } returns scrapedProduct
        every { mockProductRepository.save(any()) } returns scrapedProduct

        val result =
            runBlocking {
                service.fetchProductDetails(cmId, genre, type, locale, storedCmCode)
            }

        assertNotNull(result)
        assertEquals(3, result?.productAttributes?.size)
        assertEquals("Rare Holo", result?.productAttributes?.first { it.attributeType == ProductAttributeType.RARITY }?.value)
        assertEquals("2023-03-03", result?.releaseDate)
        assertEquals("049/197", result?.cardNumber)
    }

    @Test
    fun `fetchProductDetails returns existing product when no changes detected`() {
        val cmId = "CM001"
        val type = ProductType.SINGLES
        val storedCmCode = "base1"

        val existingProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                price = "10,00 €",
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
                sellOffers = emptyList(),
                languagePricing = emptyList(),
                productAttributes = emptyList(),
            )

        val scrapedProduct =
            Product(
                externalId = 1L,
                cmId = cmId,
                genre = Genre.POKEMON,
                type = type,
                price = "10,00 €",
                set = ProductSet(setId = 1L, cmCode = storedCmCode),
                sellOffers = emptyList(),
                languagePricing = emptyList(),
                productAttributes = emptyList(),
            )

        every { mockProductRepository.findByCmId(cmId) } returns existingProduct
        coEvery {
            mockScraperPort.fetchProductDetails(
                cmId,
                genre,
                type,
                locale,
                storedCmCode,
            )
        } returns scrapedProduct

        val result =
            runBlocking {
                service.fetchProductDetails(cmId, genre, type, locale, storedCmCode)
            }

        assertNotNull(result)
        assertEquals(existingProduct, result)
    }
}
