package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class CardMarketContentParserTest {

    private val parser = CardMarketContentParser()

    @Test
    fun `extractProductsFromHtml parses view‑source HTML correctly`() {
        val resourcePath = "src/test/resources/pikachu_gallery_30.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")
        val content = Files.readString(Paths.get(resourcePath))
        val products = parser.parseGalaryPage(content, 1)
        // Expect at least one product parsed
        assertTrue(products.results.isNotEmpty(), "No products were parsed")
        assertEquals(30, products.results.size, "Should found 30 elements")
        // Verify first product's externalId and imageUrl based on the sample HTML
        val first = products.results.first()
        assertEquals("/Pokemon/Products/Singles/Celebrations/Pikachu-V1-CEL005", first.cmId)
        assertEquals("https://product-images.s3.cardmarket.com/51/CEL/576750/576750.jpg", first.imgLink)
    }

    @Test
    fun extractDetailsFromHtml() {
        val resourcePath = "src/test/resources/pikachu_mcd166_details_stripped.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")
        val content = Files.readString(Paths.get(resourcePath))

        val productDetails = parser.parseProductDetails(
            content,
            cmId = "Pikachu-MCD166",
            genre = "Pokemon",
            type = "Singles",
            lang = "de",
            setname = "McDonalds-Collection-2016",
        )

        assertEquals("Pikachu-MCD166", productDetails.cmId)
        assertEquals("Pokemon", productDetails.genre)
        assertEquals("Singles", productDetails.type)
        assertEquals("Pikachu", productDetails.name.value)
        assertEquals("de", productDetails.name.languageCode)
        assertEquals("Pikachu-MCD166", productDetails.name.i18n)
        assertEquals("MCD16 6", productDetails.code.value)
        assertEquals("https://product-images.s3.cardmarket.com/51/MCD16/295142/295142.jpg", productDetails.imageUrl)
        assertEquals("Promo", productDetails.rarity)
        assertEquals("McDonald's Collection 2016", productDetails.set.name)
        assertEquals("McDonalds-Collection-2016", productDetails.set.code)
        assertEquals("1,40 €", productDetails.price)
        assertEquals(2, productDetails.sellOffers.size)
        assertEquals("Fable19", productDetails.sellOffers[0].sellerName)
        assertEquals("italien", productDetails.sellOffers[0].sellerLocation)
        assertEquals("Italienisch", productDetails.sellOffers[0].productLanguage)
        assertEquals("Played", productDetails.sellOffers[0].condition)
        assertEquals("1", productDetails.sellOffers[0].amount)
        assertEquals("1,40 €", productDetails.sellOffers[0].price)

    }

    @Test
    fun extractDetailsFromHtmlPlaywright() {
        val resourcePath = "src/test/resources/pikachu_mcd166_details_playwright_stripped.html"

        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")
        val content = Files.readString(Paths.get(resourcePath))

        val productDetails = parser.parseProductDetails(
            content,
            cmId = "Pikachu-MCD166",
            genre = "Pokemon",
            type = "Singles",
            lang = "de",
            setname = "McDonalds-Collection-2016",
        )

        assertEquals("Pikachu-MCD166", productDetails.cmId)
        assertEquals("Pokemon", productDetails.genre)
        assertEquals("Singles", productDetails.type)
        assertEquals("Pikachu", productDetails.name.value)
        assertEquals("de", productDetails.name.languageCode)
        assertEquals("Pikachu-MCD166", productDetails.name.i18n)
        assertEquals("MCD16 6", productDetails.code.value)
        assertEquals("https://product-images.s3.cardmarket.com/51/MCD16/295142/295142.jpg", productDetails.imageUrl)
        assertEquals("Promo", productDetails.rarity)
        assertEquals("McDonald's Collection 2016", productDetails.set.name)
        assertEquals("McDonalds-Collection-2016", productDetails.set.code)
        assertEquals("1,40 €", productDetails.price)
        assertEquals(2, productDetails.sellOffers.size)
        assertEquals("Fable19", productDetails.sellOffers[0].sellerName)
        assertEquals("italien", productDetails.sellOffers[0].sellerLocation)
        assertEquals("Italienisch", productDetails.sellOffers[0].productLanguage)
        assertEquals("Played", productDetails.sellOffers[0].condition)
        assertEquals("1", productDetails.sellOffers[0].amount)
        assertEquals("1,40 €", productDetails.sellOffers[0].price)

    }
}
