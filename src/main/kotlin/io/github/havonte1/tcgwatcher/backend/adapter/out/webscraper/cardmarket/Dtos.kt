package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

data class SearchResultsPageDto<T>(
    val results: List<T>,
    val page: Int,
    val totalPages: Int,
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
    val priceTrend: PriceTrendType,
    val series: SeriesDto? = null,
)

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
    val languageCode: String,
    val i18n: String = "",
)

data class SeriesDto(
    val seriesId: Long,
    val name: String,
    val languageCode: String,
)

data class SetDto(
    val name: String,
    val code: String,
)

data class CardmarketProductDetailsDto(
    val name: NameDto,
    val type: String,
    val genre: String,
    val code: CodeType,
    val cmId: String,
    val imageUrl: String,
    val rarity: String = "",
    val set: SetDto = SetDto("", ""),
    val series: SeriesDto? = null,
    val price: String = "0,00 €",
    val priceTrend: PriceTrendType = PriceTrendType("?", false),
    val sellOffers: List<CardmarketSellOfferDto> = emptyList(),
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
