package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


class CardMarketContentParser {
    private val logger = KotlinLogging.logger {}

    private val paginationRegex = "\\b(?:von|of|de) (\\d+)\\b".toRegex()

    //Knospi (PRE 004) --> Name: Knospi   code: (PRE-004)
    private val nameAndCodePattern = "^(.*?)\\s*\\((.*?)\\)$".toRegex()


    fun extractProductsFromHtml(content: String, page : Int = 1): SearchResultsPageDto {
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
            var imageLink = imgTag.attr("data-echo")
            if(imageLink.isEmpty())
            {
                val imageLinkBySrc = imgTag.attr("src")
                if(imageLinkBySrc.startsWith("https"))
                    imageLink = imageLinkBySrc
            }
            val titleTag = it.getElementsByTag("h2")
            val localName = titleTag.text()
            val matchResult = nameAndCodePattern.find(localName)
            val name = matchResult?.groupValues?.getOrNull(1)
            val code = matchResult?.groupValues?.getOrNull(2)
            val intPriceTag = it.getElementsByTag("b")
            val intPrice = intPriceTag.text()
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


    private val languageAndGenreAndTypePattern = "^\\s*/?([^/]+)/([^/]+)/[^/]+/([^/]+)".toRegex()

    private fun parseLink(typePath: String?): ParsedLink {
        logger.debug { "Parsing Link: $typePath" }
        val matchResult = typePath?.let { languageAndGenreAndTypePattern.find(it) }
        val language = matchResult?.groupValues?.getOrNull(1)
        val genre = matchResult?.groupValues?.getOrNull(2)
        val type = matchResult?.groupValues?.getOrNull(3)

        val cleanPath = typePath?.trim()?.trim('/')
        val id = if(language !=null)  cleanPath?.substringAfter(language) else typePath

        val parsedLink = ParsedLink(language, genre, type, id)
        logger.debug { "Parsed Link: $parsedLink" }
        return parsedLink
    }


    private fun parsePagination(document: Document): Int {
        logger.debug { "Looking for Pagination info" }
        val paginationDiv = document.getElementById("pagination")
        val paginationSpans = paginationDiv?.getElementsByTag("span")
        val paginationSpan = paginationSpans?.first { s -> s.hasClass("mx-1") }

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
