package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardMarketProductMapperTest {
    private val mapper = CardMarketProductMapper()

    @Test
    fun `toProducts maps SearchResultsPageDto correctly`() {
        val nameDto = NameDto(value = "Pikachu", locale = Locale.ENGLISH)
        val item =
            CardmarketProductGallaryItemDto(
                name = nameDto,
                code = "12345",
                set = SetDto("Set X", "setx"),
                genre = Genre.POKEMON,
                type = ProductType.SINGLES,
                cmId = "/pokemon/product/singles/setx/12345",
                cmLink = "https://www.cardmarket.com/en/pokemon/product/singles/setx/12345",
                imgLink = "https://images.cardmarket.com/12345.jpg",
                price = "10,00 €",
                priceTrend = "up",
            )
        val pageDto = SearchResultsPageDto(results = listOf(item), 1, 1)

        val products = mapper.toProducts(pageDto)
        assertEquals(1, products.size, "Exactly one product should be produced")
        val product = products.first()

        // externalId should come from the imgLink filename
        assertEquals(12345L, product.externalId, "externalId should be parsed from imgLink filename")
        assertEquals("12345", product.cmId)
        assertEquals("setx", product.set?.cmCode)
        assertEquals("https://images.cardmarket.com/12345.jpg", product.imgLink)
        assertEquals("10,00 €", product.price)
        assertEquals("Pokemon", product.genre.pathParam)
        assertEquals("Singles", product.type.cmIdentifier)
        assertEquals(nameDto.value, product.names[Locale.ENGLISH])
        assertEquals("12345", product.codeInfo?.value)
        assertTrue(product.codeInfo?.valid == true)
    }

    @Test
    fun `toProductDetails maps CardmarketProductDetailsDto correctly`() {
        val detailsDto =
            CardmarketProductDetailsDto(
                name =
                NameDto(
                    value = "Rama",
                    locale = Locale.GERMAN,
                    i18n = "RamaLama",
                ),
                type = ProductType.SINGLES,
                genre = Genre.POKEMON,
                code = CodeType(valid = true, value = "bla"),
                cmId = "Pikachu-V1-EVS049",
                imageUrl = "https://images.cardmarket.com/12345.jpg",
                rarity = "Promo",
                set =
                SetDto(
                    name = "KRababel",
                    code = "kradfb",
                ),
                price = "1,40 €",
                priceTrend =
                PriceTrendType(
                    value = "1,23 €",
                    valid = true,
                ),
                sellOffers =
                listOf(
                    CardmarketSellOfferDto(
                        sellerName = "Jürgen",
                        sellerLocation = "Deutschland",
                        productLanguage = "Deutsch",
                        condition = "Played",
                        amount = "1",
                        price = "1,23 €",
                        special = "",
                    ),
                ),
            )
        val product = mapper.toProductDetails(detailsDto)

        assertEquals("Pikachu-V1-EVS049", product.cmId)
        assertEquals(12345L, product.externalId)
        assertEquals("KRababel", product.set?.names[Locale.GERMAN])
        assertEquals("kradfb", product.set?.cmCode)
        assertEquals("Promo", product.rarity)
        assertEquals("Rama", product.names[Locale.GERMAN])
        assertEquals("bla", product.codeInfo?.value)
        assertTrue(product.codeInfo?.valid == true)
        assertEquals("Pokemon", product.genre.pathParam)
        assertEquals("Singles", product.type.cmIdentifier)
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
