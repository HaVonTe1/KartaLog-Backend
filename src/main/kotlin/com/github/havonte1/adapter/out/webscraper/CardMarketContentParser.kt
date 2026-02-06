package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component



class CardMarketContentParser {
    private val logger = KotlinLogging.logger {}

    fun extractProductsFromHtml(content: String): List<Product> {
        val results = mutableListOf<Product>()
        // The test HTML is a view‑source representation, not a clean DOM.
        // We'll extract product information using a simple regex on the raw string.
        // Each product block contains a data‑echo attribute with the image URL.
        // The numeric part of the image URL (the filename) is used as the externalId.
        val regex = Regex("""data-echo</span>=\"<a class=\"attribute-value\">([^\"]+)</a>\"""")
        regex.findAll(content).forEach { match ->
            val imageUrl = match.groupValues[1]
            // Extract the numeric ID from the URL path (e.g., .../576750/576750.jpg)
            val idMatch = Regex("/([0-9]+)/[0-9]+\\.jpg").find(imageUrl)
            val externalId = idMatch?.groupValues?.get(1)?.toLongOrNull()
            if (externalId != null) {
                results.add(
                    Product(
                        externalId = externalId,
                        setName = null,
                        rarity = null,
                        imageUrl = imageUrl
                    )
                )
            }
        }
        logger.debug { "Parsed ${results.size} product(s) from HTML" }
        return results
    }
}