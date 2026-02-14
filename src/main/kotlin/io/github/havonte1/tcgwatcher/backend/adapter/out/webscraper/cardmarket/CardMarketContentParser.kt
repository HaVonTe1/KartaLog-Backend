package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class CardMarketContentParser {
    private val logger = KotlinLogging.logger {}

    private val paginationRegex = "\\b(?:von|of|de) (\\d+)\\b".toRegex()

    // Knospi (PRE 004) --> Name: Knospi   code: (PRE-004)
    private val nameAndCodePattern = "^(.*?)\\s*\\((.*?)\\)$".toRegex()

    fun extractProductsFromHtml(content: String, page: Int = 1): SearchResultsPageDto<CardmarketProductGallaryItemDto> {
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
            var imageLink = if (imgTag.size > 0) imgTag[0].attr("data-echo") else ""
            if (imageLink.isEmpty() && imgTag.size > 0) {
                val imageLinkBySrc = imgTag[0].attr("src")
                if (imageLinkBySrc.startsWith("https")) {
                    imageLink = imageLinkBySrc
                }
            }
            val titleTag = it.getElementsByTag("h2")
            val localName = if (titleTag.size > 0) titleTag[0].text() else ""
            val matchResult = nameAndCodePattern.find(localName)
            val name = matchResult?.groupValues?.getOrNull(1)
            val code = matchResult?.groupValues?.getOrNull(2)
            val intPriceTag = it.getElementsByTag("b")
            val intPrice = if (intPriceTag.size > 0) intPriceTag[0].text() else ""
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
        val id = typePath.substringAfter("$language")
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
}
