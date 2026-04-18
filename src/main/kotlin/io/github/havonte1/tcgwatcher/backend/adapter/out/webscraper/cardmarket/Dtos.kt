package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType

data class SearchResultsPageDto<T>(
    val results: List<T>,
    val page: Int,
    val totalPages: Int
)

// --- Cardmarket -----

data class CardmarketProductGallaryItemDto(
    val name: NameDto,
    val code: CodeType,
    val genre: Genre,
    val type: ProductType,
    val cmId: String,
    val cmLink: String,
    val imgLink: String,
    val price: String,
) {
    constructor(
        name: NameDto,
        code: String,
        genre: Genre,
        type: ProductType,
        cmId: String,
        cmLink: String,
        imgLink: String,
        price: String,
        priceTrend: String,
    ) : this(
        name = name,
        code = CodeType(code, code.isNotEmpty()),
        type = type,
        genre = genre,
        cmId = cmId,
        cmLink = cmLink,
        imgLink = imgLink,
        price = price,
    )
}

data class CodeType(
    val value: String,
    val valid: Boolean,
)

data class PriceTrendType(
    val value: String,
    val valid: Boolean,
)

data class NameDto(
    val value: String,
    val locale: Locale,
    val i18n: String = "",
)

data class SetDto(
    val name: String,
    val code: String,
)

data class CardmarketProductDetailsDto(
    val name: NameDto,
    val type: ProductType,
    val genre: Genre,
    val code: CodeType,
    val cmId: String,
    val imageUrl: String,
    val rarity: String = "",
    val set: SetDto = SetDto("", ""),
    val price: String = "0,00 €",
    val priceTrend: PriceTrendType = PriceTrendType("?", false),
    val sellOffers: List<CardmarketSellOfferDto> = emptyList(),
    val languagePricing: List<CardmarketLanguagePricingDto> = emptyList(),
    val productAttributes: List<ProductAttributeDto> = emptyList(),
    val releaseDate: String = "",
    val cardNumber: String = "",
)

data class CardmarketLanguagePricingDto(
    val locale: Locale,
    val price: String,
    val priceTrend: String,
    val priceTrendValid: Boolean,
)

data class ProductAttributeDto(
    val attributeName: String,
    val value: String,
    val attributeType: String,
)

data class CardmarketSellOfferDto(
    val sellerName: String,
    val sellerLocation: String, // e.g. "Deutschland", "Germany"
    val productLanguage: String, // e.g. "Japanisch", "japanese"
    val special: String,
    val condition: String,
    val amount: String,
    val price: String,
)
