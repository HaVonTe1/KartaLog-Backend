package io.github.havonte1.tcgwatcher.backend.domain.model

data class LanguagePricing(
    val locale: Locale,
    val price: String,
    val priceTrend: String,
    val priceTrendValid: Boolean,
)

data class ProductAttribute(
    val attributeName: String,
    val value: String,
    val attributeType: ProductAttributeType,
)

enum class ProductAttributeType {
    RARITY,
    RELEASE_DATE,
    CARD_NUMBER,
    EXTENSION,
    SET_CODE,
    SERIES,
}
