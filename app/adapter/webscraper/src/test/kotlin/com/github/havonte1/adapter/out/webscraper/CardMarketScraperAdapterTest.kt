package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.github.havonte1.adapter.out.webscraper.CardMarketScraperAdapter
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
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
import org.junit.jupiter.api.Test

/**
 * Unit test for {@link CardMarketScraperAdapter}.
 *
 * The test mocks Playwright static calls so no real network traffic is performed.
 * It supplies a minimal HTML snippet that contains a single product card.
 */
class CardMarketScraperAdapterTest {

    // Sample HTML that mimics the structure expected by the adapter
    private val fakeHtml = """
        <html>
        <body>
            <div class=\"article-card\" data-id=\"12345\">
                <span class=\"product-set\">Base Set</span>
                <span class=\"product-rarity\">Rare</span>
                <img src=\"https://example.com/card.jpg\" />
            </div>
        </body>
        </html>
    """.trimIndent()

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `search returns one product built from HTML`() {
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
        every { pageMock.content() } returns fakeHtml

        // ----- Execute the adapter -----
        val adapter: CardMarketScraperPort = CardMarketScraperAdapter()
        val result: List<Product> = adapter.search("test")

        // ----- Verify -----
        assertEquals(1, result.size, "Exactly one product should be parsed")
        val product = result.first()
        assertEquals(12345L, product.externalId, "External ID should be parsed from data-id attribute")
        assertEquals("Base Set", product.setName, "Set name should be extracted")
        assertEquals("Rare", product.rarity, "Rarity should be extracted")
        assertEquals("https://example.com/card.jpg", product.imageUrl, "Image URL should be extracted")
    }
}
