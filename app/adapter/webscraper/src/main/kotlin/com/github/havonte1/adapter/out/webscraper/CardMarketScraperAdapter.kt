package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product
import com.github.havonte1.domain.port.out.CardMarketScraperPort
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Playwright
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Playwright‑based implementation of {@link CardMarketScraperPort}.
 * Scrapes CardMarket product listings and maps them to {@link Product} domain objects.
 */
@Component
class CardMarketScraperAdapter @JvmOverloads constructor() : CardMarketScraperPort {
    private val logger = LoggerFactory.getLogger(CardMarketScraperAdapter::class.java)

    /**
     * Executes a search on CardMarket and returns a list of products.
     * Only externalId, setName, rarity and imageUrl are populated; other fields remain null.
     */
    override fun search(searchString: String): List<Product> {
        logger.info("Scraping CardMarket for {}", searchString)
        val results = mutableListOf<Product>()
        Playwright.create().use { playwright ->
            val browser: Browser = playwright.chromium().launch(LaunchOptions().setHeadless(true))
            val page = browser.newPage()
            val url = "https://www.cardmarket.com/de/Pokemon/Products/Search?searchString=$searchString"
            page.navigate(url)
            val content = page.content()
            val doc: Document = Jsoup.parse(content)
            val cards = doc.select(".article-card")
            for (card in cards) {
                val idAttr = card.attr("data-id")
                val externalId = idAttr.toLongOrNull() ?: continue
                val setName = card.selectFirst(".product-set")?.text()
                val rarity = card.selectFirst(".product-rarity")?.text()
                val imageUrl = card.selectFirst("img")?.attr("src")
                results.add(
                    Product(
                        externalId = externalId,
                        setName = setName,
                        rarity = rarity,
                        imageUrl = imageUrl
                    )
                )
            }
            browser.close()
        }
        return results
    }
}
