package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Locale

data class TranslationMap(
    val de: Labels,
    val en: Labels,
    val fr: Labels,
    val it: Labels,
    val es: Labels,
    val pt: Labels,
    val nl: Labels,
    val pl: Labels,
)

fun Locale.getTranslationMap(): Labels {
    return when(this) {
        Locale.GERMAN -> DEFAULT_TRANSLATION_MAP.de
        Locale.DUTCH -> DEFAULT_TRANSLATION_MAP.nl
        Locale.ITALIAN -> DEFAULT_TRANSLATION_MAP.it
        Locale.ENGLISH -> DEFAULT_TRANSLATION_MAP.en
        Locale.FRENCH -> DEFAULT_TRANSLATION_MAP.fr
        Locale.SPANISH -> DEFAULT_TRANSLATION_MAP.it
        Locale.POLISH -> DEFAULT_TRANSLATION_MAP.pt
        Locale.PORTUGUESE -> DEFAULT_TRANSLATION_MAP.pt
    }
}

data class Labels(
    val paginationOf: String,
    val rarityLabel: String,
    val releaseDateLabel: String,
    val priceLabel: String,
    val priceTrendLabel: String,
)

val DEFAULT_TRANSLATION_MAP = TranslationMap(
    de = Labels(
        paginationOf = "von",
        rarityLabel = "Rarität",
        releaseDateLabel = "Erschienen",
        priceLabel = "ab",
        priceTrendLabel = "Preis-Trend",
    ),
    en = Labels(
        paginationOf = "of",
        rarityLabel = "Rarity",
        releaseDateLabel = "Released",
        priceLabel = "from",
        priceTrendLabel = "Price Trend",
    ),
    fr = Labels(
        paginationOf = "de",
        rarityLabel = "Rareté",
        releaseDateLabel = "Sorti",
        priceLabel = "à partir de",
        priceTrendLabel = "Tendance de prix",
    ),
    it = Labels(
        paginationOf = "di",
        rarityLabel = "Rarità",
        releaseDateLabel = "Rilasciato",
        priceLabel = "da",
        priceTrendLabel = "Trend prezzo",
    ),
    es = Labels(
        paginationOf = "de",
        rarityLabel = "Raridad",
        releaseDateLabel = "Lanzado",
        priceLabel = "desde",
        priceTrendLabel = "Tendencia de precio",
    ),
    pt = Labels(
        paginationOf = "de",
        rarityLabel = "Raridade",
        releaseDateLabel = "Lançado",
        priceLabel = "a partir de",
        priceTrendLabel = "Tendência de preço",
    ),
    nl = Labels(
        paginationOf = "van",
        rarityLabel = "Zeldzaamheid",
        releaseDateLabel = "Uitgebracht",
        priceLabel = "vanaf",
        priceTrendLabel = "Prijs trend",
    ),
    pl = Labels(
        paginationOf = "z",
        rarityLabel = "Rzadkość",
        releaseDateLabel = "Wydany",
        priceLabel = "od",
        priceTrendLabel = "Trend ceny",
    ),
)
