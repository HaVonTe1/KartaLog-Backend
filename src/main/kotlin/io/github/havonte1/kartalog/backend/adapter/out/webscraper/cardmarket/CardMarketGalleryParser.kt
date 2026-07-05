package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.text.ifEmpty

class CardMarketGalleryParser {
    private val nameAndCodePattern = "^(.*?)\\s*\\((.*?)\\)$".toRegex()
    private val minPathParts = 4
    private val logger = KotlinLogging.logger {}

    fun parse(
        content: String,
        locale: Locale,
        page: Int,
    ): Result<SearchResultsPageDto<CardmarketProductGallaryItemDto>> {
        return try {
            val document = Jsoup.parse(content)
            val tiles = document.getElementsByTag("a").filter { it.hasClass("card") && it.hasAttr("href") }

            if (tiles.isEmpty()) {
                return Result.failure(UnexpectedFormat("No product tiles found. Expected a tags with class 'card'."))
            }

            val galleryItems = tiles.map { tile -> parseTile(tile) }
            val totalPages = parsePagination(document, locale)

            Result.success(SearchResultsPageDto(galleryItems, page, totalPages))
        } catch (e: UnexpectedFormat) {
            Result.failure(e)
        } catch (e: MissingElement) {
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { e.message }
            Result.failure(UnexpectedFormat("Failed to parse gallery page: ${e.message}"))
        }
    }

    private fun parseTile(tile: org.jsoup.nodes.Element): CardmarketProductGallaryItemDto {
        val cmLink = tile.attr("href")
        val parsedLink = parseLink(cmLink)
        val imgTag = tile.getElementsByTag("img")
        var imageLink = if (imgTag.isNotEmpty()) imgTag[0].attr("data-echo") else ""
        if (imageLink.isEmpty() && imgTag.isNotEmpty()) {
            val imageLinkBySrc = imgTag[0].attr("src")
            if (imageLinkBySrc.startsWith("https")) {
                imageLink = imageLinkBySrc
            }
        }
        val titleTag = tile.getElementsByTag("h2")

        val localName = if (titleTag.isNotEmpty()) titleTag[0].text() else ""
        val matchResult = nameAndCodePattern.find(localName)
        val name = matchResult?.groupValues?.getOrNull(1)
        val code = matchResult?.groupValues?.getOrNull(2)
        val intPriceTag = tile.getElementsByTag("b")
        val intPrice = if (intPriceTag.isNotEmpty()) intPriceTag[0].text() else ""

        val expansionSymbol = titleTag.firstOrNull()?.getElementsByClass("expansion-symbol")?.firstOrNull()
        var setName = expansionSymbol?.attr("title")?:""
        if(setName.isBlank()) {
            setName = expansionSymbol?.attr("aria-label")?:""
        }
        if(setName.isBlank()) {
            setName = expansionSymbol?.attr("data-bs-original-title") ?: ""
        }



        val setCode = parsedLink.setCode ?: ""

        return CardmarketProductGallaryItemDto(
            name = NameDto(name ?: localName, Locale.fromId(parsedLink.language ?: "de"), localName),
            code = CodeType(code ?: "", code != null),
            set = SetDto(setName, setCode),
            genre = Genre.fromId(parsedLink.genre ?: "Pokemon"), //:-(
            type = ProductType.fromId(parsedLink.type ?: "Singles"),
            cmId = parsedLink.id ?: "",
            cmLink = cmLink,
            imgLink = imageLink,
            price = intPrice,
        )
    }

    private data class ParsedLink(
        val language: String?,
        val genre: String?,
        val type: String?,
        val setCode: String?,
        val id: String?,
    )

    private fun parseLink(typePath: String?): ParsedLink {
        if (typePath.isNullOrBlank()) {
            return ParsedLink(null, null, null, null, null)
        }

        val parts = typePath.split('/')
        val startIdx = parts.indexOfFirst { it.isNotEmpty() }.takeIf { it >= 0 }
            ?: return ParsedLink(null, null, null, null, null)

        if (parts.size - startIdx < minPathParts) {
            return ParsedLink(
                parts.getOrNull(startIdx),
                parts.getOrNull(startIdx + 1),
                parts.getOrNull(startIdx + 2),
                null,
                typePath,
            )
        }

        val language = parts[startIdx]
        val genre = parts.getOrNull(startIdx + 1)
        val type = parseTypeFromParts(parts, startIdx)
        val setCode = parts.getOrNull(startIdx + 4)
        val id = typePath.substringAfter(language)

        return ParsedLink(language, genre, type, setCode, id)
    }

    private fun parseTypeFromParts(parts: List<String>, startIdx: Int): String? {
        return if (parts.size > startIdx + 3 && parts[startIdx + 2] == "Products") {
            parts.getOrNull(startIdx + 3)
        } else {
            parts.getOrNull(startIdx + 2)
        }
    }

    private fun parsePagination(document: Document, locale: Locale): Int {
        val paginationDiv = document.getElementById("pagination")
        val paginationSpans = paginationDiv?.getElementsByTag("span") ?: emptyList()
        val paginationSpan = paginationSpans.firstOrNull { it.hasClass("mx-1") }

        if (paginationSpan == null) {
            throw MissingElement("Pagination span with class 'mx-1' not found")
        }

        val text = paginationSpan.text()
        val labels = locale.getTranslationMap()
        val paginationRegex = "${labels.paginationOf} (\\d+)\\b".toRegex()
        val matchResult = paginationRegex.find(text)

        return matchResult?.groupValues?.getOrNull(1)?.toInt() ?: 0
    }
}
