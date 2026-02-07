package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.github.havonte1.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
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
 * The test mocks Playwright static calls so no real network traffic is performed.
 * It supplies a minimal HTML snippet that contains a single product card.
 */
class CardMarketScraperAdapterTest {



    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `search returns one product built from HTML`() {
        val resourcePath = "src/test/resources/pikachu_gallery_50.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen");
        // ----- Mock Playwright static factory -----
        mockkStatic(Playwright::class)

        // Mock the Playwright instance returned by Playwright.create()
        val playwrightMock = mockk<Playwright>(relaxed = true)
        every { Playwright.create() } returns playwrightMock

        // Mock the Chromium launch chain
        val browserMock = mockk<Browser>(relaxed = true)
        val browserTypeMock = mockk<BrowserType>(relaxed = true)
        val pageMock = mockk<Page>(relaxed = true)

        every { playwrightMock.chromium() } returns browserTypeMock
        every { browserTypeMock.launch(any()) } returns browserMock
        every { browserMock.newPage() } returns pageMock

        // Navigation and content retrieval
        val responseMock = mockk<Response>(relaxed = true)
        every { pageMock.navigate(any<String>()) } returns responseMock
        val content = Files.readString(Paths.get(resourcePath))
        every { pageMock.content() } returns content

        // ----- Execute the adapter -----
        val adapter: CardMarketScraperPort = CardMarketScraperAdapter()
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
