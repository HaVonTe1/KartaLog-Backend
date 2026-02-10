package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

data class SearchResultsPageDto(
    val results: List<CardmarketProductGallaryItemDto>, // TODO: use a generic type
    val page: Int,
    val totalPages: Int
)

// --- Cardmarket -----

data class CardmarketProductGallaryItemDto(
    val name: NameDto,
    val code: CodeType,
    val genre: String,
    val type: String,
    val cmId: String,
    val cmLink: String,
    val imgLink: String,
    val price: String,
    val priceTrend: PriceTrendType
) {
    constructor(
        name: NameDto,
        code: String,
        genre: String,
        type: String,
        cmId: String,
        cmLink: String,
        imgLink: String,
        price: String,
        priceTrend: String
    ) : this(
        name = name,
        code = CodeType(code, code.isNotEmpty()),
        type = type,
        genre = genre,
        cmId = cmId,
        cmLink = cmLink,
        imgLink = imgLink,
        price = price,
        priceTrend = PriceTrendType(priceTrend, priceTrend.isNotEmpty())
    )
}

data class CodeType(val value: String, val valid: Boolean)
data class PriceTrendType(val value: String, val valid: Boolean)
data class NameDto(val value: String, val languageCode: String, val i18n: String = "")
data class SetDto(val name: String, val link: String)

data class CardmarketProductDetailsDto(
    val name: NameDto,
    val type: String,
    val genre: String,
    val code: CodeType,
    val cmId: String,
    val imageUrl: String,
    val detailsUrl: String,
    val rarity: String = "",
    val set: SetDto = SetDto("", ""),
    val price: String = "0,00 €",
    val priceTrend: PriceTrendType = PriceTrendType("?", false),
    val sellOffers: List<CardmarketSellOfferDto> = emptyList()
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
