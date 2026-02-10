package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketProductMapper
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardmarketProductGallaryItemDto
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.NameDto
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.SearchResultsPageDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardMarketProductMapperTest {

    private val mapper = CardMarketProductMapper()

    @Test
    fun `toProducts maps SearchResultsPageDto correctly`() {
        val nameDto = NameDto(value = "Pikachu", languageCode = "en")
        val item = CardmarketProductGallaryItemDto(
            name = nameDto,
            code = "12345",
            genre = "Pokemon",
            type = "Single",
            cmId = "12345",
            cmLink = "https://cardmarket.com/product/12345",
            imgLink = "https://images.cardmarket.com/12345.jpg",
            price = "10,00 €",
            priceTrend = "up"
        )
        val pageDto = SearchResultsPageDto(results = listOf(item), page = 1, totalPages = 1)

        val products = mapper.toProducts(pageDto)
        assertEquals(1, products.size, "Exactly one product should be produced")
        val product = products.first()

        // externalId should come from the imgLink filename
        assertEquals(12345L, product.externalId, "externalId should be parsed from imgLink filename")
        assertEquals("12345", product.cmId)
        assertEquals("https://images.cardmarket.com/12345.jpg", product.imgLink)
        assertEquals("10,00 €", product.price)
        assertEquals("up", product.priceTrendInfo?.value)
        assertTrue(product.priceTrendInfo?.valid == true)
        assertEquals("Pokemon", product.genre)
        assertEquals("Single", product.type)
        assertEquals(nameDto.value, product.names["en"])
        assertEquals("12345", product.codeInfo?.value)
        assertTrue(product.codeInfo?.valid == true)
    }
}
