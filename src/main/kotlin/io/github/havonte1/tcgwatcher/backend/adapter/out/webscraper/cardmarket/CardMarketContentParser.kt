package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class CardMarketContentParser {
    private val logger = KotlinLogging.logger {}

    private val paginationRegex = "\\b(?:von|of|de) (\\d+)\\b".toRegex()

    // Knospi (PRE 004) --> Name: Knospi   code: (PRE-004)
    private val nameAndCodePattern = "^(.*?)\\s*\\((.*?)\\)$".toRegex()

    fun parseGalaryPage(content: String, page: Int = 1): SearchResultsPageDto<CardmarketProductGallaryItemDto> {
        val document = Jsoup.parse(content)

        logger.debug { "Parsing a tags with class card and a href" }
        val tiles = document.getElementsByTag("a")
            .filter { element -> element.hasClass("card") && element.hasAttr("href") }
        logger.debug { "Found: ${tiles.size}" }

        val cardmarketProductGallaryItemDtos = ArrayList<CardmarketProductGallaryItemDto>(tiles.size)

        tiles.forEach {
            val cmLink = it.attr("href")
            val parsedLink = parseLink(cmLink)
            val imgTag = it.getElementsByTag("img")
            var imageLink = if (imgTag.isNotEmpty()) imgTag[0].attr("data-echo") else ""
            if (imageLink.isEmpty() && imgTag.isNotEmpty()) {
                val imageLinkBySrc = imgTag[0].attr("src")
                if (imageLinkBySrc.startsWith("https")) {
                    imageLink = imageLinkBySrc
                }
            }
            val titleTag = it.getElementsByTag("h2")
            val localName = if (titleTag.isNotEmpty()) titleTag[0].text() else ""
            val matchResult = nameAndCodePattern.find(localName)
            val name = matchResult?.groupValues?.getOrNull(1)
            val code = matchResult?.groupValues?.getOrNull(2)
            val intPriceTag = it.getElementsByTag("b")
            val intPrice = if (intPriceTag.isNotEmpty()) intPriceTag[0].text() else ""
            val itemDto = CardmarketProductGallaryItemDto(
                name = NameDto(name ?: localName, parsedLink.language ?: "", localName),
                code = CodeType(code ?: "", code != null),
                genre = parsedLink.genre ?: "",
                type = parsedLink.type ?: "",
                cmId = parsedLink.id ?: "",
                cmLink = cmLink,
                imgLink = imageLink,
                price = intPrice,
                priceTrend = PriceTrendType("?", false)
            )
            logger.debug { "Item: $itemDto" }

            cardmarketProductGallaryItemDtos.add(itemDto)
        }
        val totalPages = parsePagination(document)

        return SearchResultsPageDto(cardmarketProductGallaryItemDtos, page, totalPages)
    }

    private data class ParsedLink(
        val language: String?,
        val genre: String?,
        val type: String?,
        val id: String?
    )

    private fun parseLink(typePath: String?): ParsedLink {
        logger.debug { "Parsing Link: $typePath" }

        if (typePath == null || typePath.trim().isEmpty()) {
            return ParsedLink(null, null, null, null)
        }

        val parts = typePath.split('/')

        // Find index of first non-empty part after leading slash
        var startIdx = 0
        for (i in parts.indices) {
            if (parts[i].isNotEmpty()) {
                startIdx = i
                break
            }
        }

        if (parts.size - startIdx < 4) {
            logger.debug { "Path does not have enough segments: $typePath" }
            return ParsedLink(
                parts.getOrNull(startIdx),
                parts.getOrNull(startIdx + 1),
                parts.getOrNull(startIdx + 2),
                typePath
            )
        }

        val language = parts[startIdx]
        val genre = parts.getOrNull(startIdx + 1)
        val type = if (parts.size > startIdx + 3 && parts[startIdx + 2] == "Products") {
            parts.getOrNull(startIdx + 3)
        } else {
            parts.getOrNull(startIdx + 2)
        }

        // ID is everything after the language segment, including the leading slash
        val id = typePath.substringAfter(language)
        val parsedLink = ParsedLink(language, genre, type, id)
        logger.debug { "Parsed Link: $parsedLink" }
        return parsedLink
    }

    private fun parsePagination(document: Document): Int {
        logger.debug { "Looking for Pagination info" }
        val paginationDiv = document.getElementById("pagination")
        val paginationSpans = paginationDiv?.getElementsByTag("span") ?: emptyList()
        val paginationSpan = paginationSpans.firstOrNull { s -> s.hasClass("mx-1") }

        var groupValue: String? = null
        if (paginationSpan != null) {
            val text = paginationSpan.text()
            val matchResult = paginationRegex.find(text)
            groupValue = matchResult?.groupValues?.getOrNull(1)
        }

        val totalPages = groupValue?.toInt() ?: 0
        logger.debug { "Found: $totalPages" }
        return totalPages
    }

    fun parseProductDetails(document: Document, link: String): CardmarketProductDetailsDto {
        val imageTags = document.getElementsByTag("img")
        val frontImageTag =
            imageTags.first { img -> img.classNames().size == 1 } //filter out "lazy" img tags
        val imageUrl = frontImageTag.attr("src")
        val h1Tags = document.getElementsByTag("h1")
        val h1Tag = h1Tags.first()
        val displayName = h1Tag?.ownText() ?: ""

        val matchResult = nameAndCodePattern.find(displayName)
        val name = matchResult?.groupValues?.getOrNull(1)
        val code = matchResult?.groupValues?.getOrNull(2)
        val orgName = link.split("/").last()

        val parsedLink = parseLink(link)

        val infoDivs = document.getElementsByClass("info-list-container")
        val infoDiv = infoDivs.first()

        val dts = infoDiv?.getElementsByTag("dt")

        val rarityDt = dts?.first { dt -> dt.text() == "Rarität" }
        val rarityText = rarityDt?.nextElementSibling()?.getElementsByTag("svg")?.attr("title")

        val setDt = dts?.first { dt -> dt.text().startsWith("Erschienen") }
        val setHref = setDt?.nextElementSibling()?.getElementsByTag("a")
        val setLink = setHref?.first()?.attr("href")
        val setName = setHref?.first()?.attr("title")

        val abDt = dts?.first { dt -> dt.text() == "ab" }
        val localPrice = abDt?.nextElementSibling()?.text() ?: "0,00 €"

        val priceTrendDt = dts?.first { dt -> dt.text() == "Preis-Trend" }
        val localPriceTrend = priceTrendDt?.nextElementSibling()?.getElementsByTag("span")?.text() ?: "0,00 €"

        val cardmarketSellOfferDtos = ArrayList<CardmarketSellOfferDto>()

        val sellOfferRows = document.getElementsByClass("article-row")
        sellOfferRows.forEach { sellOfferRow ->
            val sellerCol = sellOfferRow.getElementsByClass("col-seller").first()
            val sellerHrefTag = sellerCol?.getElementsByTag("a")
            val sellerName = sellerHrefTag?.text()

            val sellerLocationTag = sellerCol?.getElementsByClass("icon")
            val sellerLocation = sellerLocationTag?.first()?.attr("title")
            val parts = sellerLocation?.split(": ")
            val sellerLocationSanatized = parts?.size?.let {
                if (it < 2) {
                    // Handle cases where the format is unexpected
                    logger.debug { "Warning: sellerLocationString '$sellerLocation' does not match expected format 'Prefix: CountryName'" }
                    sellerLocation
                } else {
                    parts.last().lowercase()
                }
            }

            val sellerAttributDiv = sellOfferRow.getElementsByClass("product-attributes")

            val productCondition = sellerAttributDiv.first()?.getElementsByClass("article-condition")?.first()
                ?.attr("title")

            val productAttributIcons = sellerAttributDiv.first()?.getElementsByClass("icon")
            val productLanguage = productAttributIcons?.first()
                ?.attr("title")
            var productSpeciality = ""
            productAttributIcons?.size?.let {
                if (it > 1) {
                    productSpeciality = productAttributIcons[1]
                        .attr("title")
                }
            }

            val priceContainer = sellOfferRow.getElementsByClass("price-container").first()
            val price = priceContainer?.getElementsByTag("span")?.text()

            val productAmount =
                sellOfferRow.getElementsByClass("amount-container")?.first()?.getElementsByTag("span")?.first()?.text()

            if (sellerName != null && sellerLocation != null && productLanguage != null && price != null && productAmount != null && productCondition != null) {
                val cardmarketSellOfferDto = CardmarketSellOfferDto(
                    sellerName = sellerName,
                    sellerLocation = sellerLocationSanatized ?: "",
                    productLanguage = productLanguage,
                    special = productSpeciality,
                    condition = productCondition,
                    amount = productAmount,
                    price = price
                )
                logger.debug { "Sell Offer: $cardmarketSellOfferDto" }
                cardmarketSellOfferDtos.add(cardmarketSellOfferDto)

            }
        }
        val cardmarketProductDetailsDto = CardmarketProductDetailsDto(
            name = NameDto(value = name ?: orgName, languageCode = parsedLink.language ?: "", i18n = orgName),
            code = CodeType(code ?: "", code != null),
            type = parsedLink.type ?: "",
            genre = parsedLink.genre ?: "",
            cmId = parsedLink.id ?: "",
            rarity = rarityText ?: "",
            set = SetDto(setName ?: "", setLink ?: ""),
            detailsUrl = link,
            imageUrl = imageUrl,
            price = localPrice,
            priceTrend = PriceTrendType(localPriceTrend, true),
            sellOffers = cardmarketSellOfferDtos
        )
        logger.debug { "Product Details: $cardmarketProductDetailsDto" }
        return cardmarketProductDetailsDto
    }

}
