package com.github.havonte1.adapter.out.webscraper

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
        val resourcePath = "src/test/resources/pikachu_gallery_50.html"

        val file: File = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen");
        val content = Files.readString(Paths.get(resourcePath))
        val products = parser.extractProductsFromHtml(content, 1)
        // Expect at least one product parsed
        assertTrue(products.results.isNotEmpty(), "No products were parsed")
        assertEquals(30,products.results.size, "Should found 30 elements")
        // Verify first product's externalId and imageUrl based on the sample HTML
        val first = products.results.first()
        assertEquals("/Pokemon/Products/Singles/Celebrations/Pikachu-V1-CEL005", first.cmId)
        assertEquals("https://product-images.s3.cardmarket.com/51/CEL/576750/576750.jpg", first.imgLink)
    }
}
