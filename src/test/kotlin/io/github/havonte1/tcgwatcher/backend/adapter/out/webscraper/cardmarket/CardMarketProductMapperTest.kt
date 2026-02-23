package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.SellOfferDTO
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
            cmId = "/pokemon/product/singles/setx/12345",
            cmLink = "https://www.cardmarket.com/en/pokemon/product/singles/setx/12345",
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
        assertEquals("setx", product.setName)
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

    @Test
    fun `toProductDetails maps CardmarketProductDetailsDto correctly`() {

        val detailsDto = CardmarketProductDetailsDto(
            name = NameDto(
                value = "Rama",
                languageCode = "de",
                i18n = "RamaLama"
            ),
            type = "Singles",
            genre = "Pokemon",
            code = CodeType(valid = true, value = "bla"),
            cmId = "Pikachu-V1-EVS049",
            imageUrl = "https://images.cardmarket.com/12345.jpg",
            rarity = "Promo",
            set = SetDto(
                name = "KRababel",
                link = "/de/Pokemon/Products/Sets/kradfb"
            ),
            price = "1,40 €",
            priceTrend = PriceTrendType(
                value = "1,23 €",
                valid = true
            ) ,
            sellOffers = listOf(
                CardmarketSellOfferDto(
                    sellerName = "Jürgen",
                    sellerLocation = "Deutschland",
                    productLanguage = "Deutsch",
                    condition = "Played",
                    amount = "1",
                    price = "1,23 €",
                    special = ""
                )
            )
        )
        val product = mapper.toProductDetails(detailsDto)

        assertEquals("Pikachu-V1-EVS049", product.cmId)
        assertEquals(12345L, product.externalId)
        assertEquals("KRababel", product.setName)
        assertEquals("Promo", product.rarity)
        assertEquals("Rama", product.names["de"])
        assertEquals("bla", product.codeInfo?.value)
        assertTrue(product.codeInfo?.valid == true)
        assertEquals("Pokemon", product.genre)
        assertEquals("Singles", product.type)
        assertEquals("https://images.cardmarket.com/12345.jpg", product.imgLink)
        assertEquals("1,40 €", product.price)
        assertEquals("1,23 €", product.priceTrendInfo?.value)
        assertTrue(product.priceTrendInfo?.valid == true)

        assertEquals(1, product.sellOffers?.size)
        val sellOffer = product.sellOffers!!.first()
        assertEquals("Jürgen", sellOffer.sellerName)
        assertEquals("Deutschland", sellOffer.sellerLocation)
        assertEquals("Deutsch", sellOffer.productLanguage)
        assertEquals("Played", sellOffer.condition)
        assertEquals("1", sellOffer.amount)
        assertEquals("1,23 €", sellOffer.price)
        assertEquals("", sellOffer.special)


    }
}
