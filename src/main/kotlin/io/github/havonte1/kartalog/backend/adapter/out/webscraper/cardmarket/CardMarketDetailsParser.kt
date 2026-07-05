package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Suppress("TooManyFunctions")
class CardMarketDetailsParser {
    private val nameAndCodePattern = "^(.*?)\\s*\\((.*?)\\)$".toRegex()


    private fun selectLabels(locale: Locale, map: TranslationMap): Labels {
        return when (locale.code) {
            "de" -> map.de
            "en" -> map.en
            "fr" -> map.fr
            "it" -> map.it
            "es" -> map.es
            "pt" -> map.pt
            "nl" -> map.nl
            "pl" -> map.pl
            else -> map.en
        }
    }

    fun parse(
        content: String,
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        translationMap: TranslationMap = DEFAULT_TRANSLATION_MAP,
    ): Result<CardmarketProductDetailsDto> {
        val labels = selectLabels(locale, translationMap)

        return try {
            val document = Jsoup.parse(content)
            val imageUrl = extractImageUrl(document)
            val (name, code) = extractNameAndCode(document, cmId)

            val infoDivs = document.getElementsByClass("info-list-container")
            val infoDiv = infoDivs.first() ?: return Result.failure(UnexpectedFormat("Info list container not found"))
            val dts = infoDiv.getElementsByTag("dt")

            val rarityText = extractRarity(dts, labels)
            if (rarityText == null) {
                return Result.failure(MissingElement("Rarity label not found"))
            }

            val (setName, setCode) = extractSetInfo(dts, labels)
            if (setName == null) {
                return Result.failure(MissingElement("Release date label not found"))
            }

            val localPrice = extractPrice(dts, labels)
            if (localPrice == null) {
                return Result.failure(MissingElement("Price label not found"))
            }

            val localPriceTrend = extractPriceTrend(dts, labels)
            if (localPriceTrend == null) {
                return Result.failure(MissingElement("Price trend label not found"))
            }

            val sellOffers = parseSellOffers(document)

            val languagePricing = parseLanguagePricing(document, labels)
            val (releaseDate, cardNumber) = extractAdditionalAttributes(dts, labels)
            val series = extractSeriesInfo(dts, labels)

            val productDetailsDto =
                CardmarketProductDetailsDto(
                    name = NameDto(value = name, locale = locale, i18n = cmId),
                    code = CodeType(code, code.isNotEmpty()),
                    type = type,
                    genre = genre,
                    cmId = cmId,
                    rarity = rarityText,
                    set = SetDto(setName, setCode),
                    imageUrl = imageUrl,
                    price = localPrice,
                    priceTrend = PriceTrendType(localPriceTrend, true),
                    sellOffers = sellOffers,
                    languagePricing = languagePricing,
                    productAttributes = buildProductAttributes(rarityText, releaseDate, cardNumber, setName),
                    releaseDate = releaseDate ?: "",
                    cardNumber = cardNumber ?: "",
                    seriesId = series?.first,
                    seriesName = series?.second,
                )

            Result.success(productDetailsDto)
        } catch (e: UnexpectedFormat) {
            Result.failure(e)
        } catch (e: MissingElement) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(UnexpectedFormat("Failed to parse product details: ${e.message}"))
        }
    }

    private fun extractImageUrl(document: Document): String {
        val imageTags = document.getElementsByTag("img")
        val frontImageTag = imageTags.firstOrNull { img -> img.classNames().size == 1 }
        return frontImageTag?.attr("src") ?: ""
    }

    private fun extractNameAndCode(document: Document, cmId: String): Pair<String, String> {
        val h1Tags = document.getElementsByTag("h1")
        val h1Tag = h1Tags.firstOrNull()
        val displayName = h1Tag?.ownText() ?: ""
        val matchResult = nameAndCodePattern.find(displayName)
        val name = matchResult?.groupValues?.getOrNull(1) ?: cmId
        val code = matchResult?.groupValues?.getOrNull(2) ?: ""
        return Pair(name, code)
    }

    private fun extractRarity(dts: org.jsoup.select.Elements, labels: Labels): String? {
        val rarityDt = dts.firstOrNull { it.text() == labels.rarityLabel }
        val rarityElement = rarityDt?.nextElementSibling()?.getElementsByTag("svg")
        return rarityElement?.attr("title")?.ifEmpty { rarityElement.attr("data-bs-original-title") }
    }

    private fun extractSetInfo(dts: org.jsoup.select.Elements, labels: Labels): Pair<String?, String> {
        val setDt = dts.firstOrNull { it.text().startsWith(labels.releaseDateLabel) }
        val setHref = setDt?.nextElementSibling()?.getElementsByTag("a")
        val setHrefElement = setHref?.first()
        val setLink = setHrefElement?.attr("href")
        val setName = setHrefElement?.attr("title")?.ifEmpty { setHrefElement.attr("aria-label") }
        val setCode = setLink?.substringAfterLast("/") ?: ""
        return Pair(setName, setCode)
    }

    private fun extractPrice(dts: org.jsoup.select.Elements, labels: Labels): String? {
        val priceDt = dts.firstOrNull { it.text() == labels.priceLabel }
        return priceDt?.nextElementSibling()?.text() ?: "0,00 €"
    }

    private fun extractPriceTrend(dts: org.jsoup.select.Elements, labels: Labels): String? {
        val priceTrendDt = dts.firstOrNull { it.text() == labels.priceTrendLabel }
        return priceTrendDt?.nextElementSibling()?.getElementsByTag("span")?.text() ?: "0,00 €"
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseLanguagePricing(document: Document, labels: Labels): List<CardmarketLanguagePricingDto> {
        val pricingList = mutableListOf<CardmarketLanguagePricingDto>()

        val priceTable = document.getElementsByClass("table")
            .firstOrNull { it.text().contains("Deutsch") || it.text().contains("English") }

        if (priceTable != null) {
            val rows = priceTable.getElementsByTag("tr")
            val localeCodeMap = mapOf(
                "Deutsch" to Locale.GERMAN,
                "English" to Locale.ENGLISH,
                "Français" to Locale.FRENCH,
                "Italiano" to Locale.ITALIAN,
                "Español" to Locale.SPANISH,
                "Português" to Locale.PORTUGUESE,
                "Polski" to Locale.POLISH,
            )

            for (row in rows) {
                val cells = row.getElementsByTag("td")
                if (cells.size >= 3) {
                    val localeText = cells[0].text().trim()
                    val locale = localeCodeMap[localeText] ?: continue
                    val price = cells[1].text().trim()
                    val trend = cells.getOrNull(2)?.text()?.trim() ?: ""

                    pricingList.add(
                        CardmarketLanguagePricingDto(
                            locale = locale,
                            price = price,
                            priceTrend = trend,
                            priceTrendValid = trend.isNotEmpty() && trend != "?",
                        )
                    )
                }
            }
        }

        if (pricingList.isEmpty()) {
            val defaultLocale = Locale.ENGLISH
            pricingList.add(
                CardmarketLanguagePricingDto(
                    locale = defaultLocale,
                    price = "",
                    priceTrend = "",
                    priceTrendValid = false,
                )
            )
        }

        return pricingList
    }

    private fun extractAdditionalAttributes(dts: org.jsoup.select.Elements, labels: Labels): Pair<String?, String?> {
        var releaseDate: String? = null
        var cardNumber: String? = null

        for (dt in dts) {
            val dtText = dt.text()
            when {
                dtText.startsWith(labels.releaseDateLabel) -> {
                    val dd = dt.nextElementSibling()
                    releaseDate = dd?.text()?.trim()
                }
                dtText.contains("Nummer") || dtText.contains("Number") || dtText.contains("Card number") -> {
                    val dd = dt.nextElementSibling()
                    cardNumber = dd?.text()?.trim()
                }
            }
        }

        return Pair(releaseDate, cardNumber)
    }

    private fun extractSeriesInfo(dts: org.jsoup.select.Elements, labels: Labels): Pair<Long, String>? {
        val seriesDt = dts.firstOrNull { it.text() == labels.seriesLabel }
        val seriesLink = seriesDt?.nextElementSibling()?.getElementsByTag("a")?.first()
        val seriesHref = seriesLink?.attr("href") ?: return null

        val seriesIdMatch = "serieId=(\\d+)".toRegex().find(seriesHref)
        val seriesId = seriesIdMatch?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val seriesName = seriesLink.text().ifEmpty { seriesLink.attr("title") }

        return if (seriesId > 0 && seriesName.isNotEmpty()) Pair(seriesId, seriesName) else null
    }

    private fun buildProductAttributes(
        rarity: String?,
        releaseDate: String?,
        cardNumber: String?,
        setName: String?,
    ): List<ProductAttributeDto> {
        val attributes = mutableListOf<ProductAttributeDto>()

        rarity?.let {
            attributes.add(ProductAttributeDto("rarity", it, "RARITY"))
        }
        releaseDate?.let {
            attributes.add(ProductAttributeDto("releaseDate", it, "RELEASE_DATE"))
        }
        cardNumber?.let {
            attributes.add(ProductAttributeDto("cardNumber", it, "CARD_NUMBER"))
        }
        setName?.let {
            attributes.add(ProductAttributeDto("extension", it, "EXTENSION"))
        }

        return attributes
    }

    private fun parseSellOffers(document: Document): List<CardmarketSellOfferDto> {
        val sellOfferDtos = mutableListOf<CardmarketSellOfferDto>()
        val sellOfferRows = document.getElementsByClass("article-row")

        sellOfferRows.forEach { sellOfferRow ->
            val sellerCol = sellOfferRow.getElementsByClass("col-seller").first()
            val sellerHrefTag = sellerCol?.getElementsByTag("a")
            val sellerName = sellerHrefTag?.text()

            val sellerLocationTag = sellerCol?.getElementsByClass("icon")
            val sellerLocation = sellerLocationTag?.first()?.attr("title")?.ifEmpty {
                sellerLocationTag.first()?.attr("aria-label")
            }

            val parts = sellerLocation?.split(": ")
            val sellerLocationSanitized = parts?.size?.let {
                if (it < 2) sellerLocation else parts.last().lowercase()
            }

            val sellerAttributDiv = sellOfferRow.getElementsByClass("product-attributes")
            val conditionElement = sellerAttributDiv.first()?.getElementsByClass("article-condition")?.first()
            val productCondition = conditionElement?.attr("title")
                ?.ifEmpty { conditionElement.attr("data-bs-original-title") }

            val productAttributIcons = sellerAttributDiv.first()?.getElementsByClass("icon")
            val productLanguage = productAttributIcons?.first()?.attr("title")?.ifEmpty {
                productAttributIcons.first()?.attr("aria-label")
            }

            var productSpeciality = ""
            productAttributIcons?.size?.let {
                if (it > 1) productSpeciality = productAttributIcons[1].attr("title")
            }

            val priceContainer = sellOfferRow.getElementsByClass("price-container").first()
            val price = priceContainer?.getElementsByTag("span")?.text()

            val productAmount = sellOfferRow.getElementsByClass("amount-container").first()
                ?.getElementsByTag("span")
                ?.first()
                ?.text()

            val isValidOffer = sellerName != null && sellerLocation != null &&
                productLanguage != null && price != null && productAmount != null && productCondition != null

            if (isValidOffer) {
                val sellOfferDto = CardmarketSellOfferDto(
                    sellerName = sellerName,
                    sellerLocation = sellerLocationSanitized ?: "",
                    productLanguage = productLanguage,
                    special = productSpeciality,
                    condition = productCondition,
                    amount = productAmount,
                    price = price,
                )
                sellOfferDtos.add(sellOfferDto)
            }
        }

        return sellOfferDtos
    }
}
