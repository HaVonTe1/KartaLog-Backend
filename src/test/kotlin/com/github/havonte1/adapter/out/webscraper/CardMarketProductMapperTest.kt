package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.adapter.out.webscraper.cardmarket.CardMarketProductMapper
import com.github.havonte1.adapter.out.webscraper.cardmarket.CardmarketProductGallaryItemDto
import com.github.havonte1.adapter.out.webscraper.cardmarket.NameDto
import com.github.havonte1.adapter.out.webscraper.cardmarket.SearchResultsPageDto
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


        assertEquals(12345L, product.externalId, "externalId should be parsed from cmId string")
        assertEquals("https://images.cardmarket.com/12345.jpg", product.imageUrl)
        assertEquals("12345", product.cmId)
        assertEquals("https://cardmarket.com/product/12345", product.cmLink)
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
