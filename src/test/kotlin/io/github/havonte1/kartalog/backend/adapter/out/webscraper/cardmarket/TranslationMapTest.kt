package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TranslationMapTest {
    @Test
    fun `all 8 languages exist in TranslationMap`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertNotNull(translationMap.de)
        assertNotNull(translationMap.en)
        assertNotNull(translationMap.fr)
        assertNotNull(translationMap.it)
        assertNotNull(translationMap.es)
        assertNotNull(translationMap.pt)
        assertNotNull(translationMap.nl)
        assertNotNull(translationMap.pl)
    }

    @Test
    fun `each language has all required labels`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        val allLabels =
            listOf(
                translationMap.de,
                translationMap.en,
                translationMap.fr,
                translationMap.it,
                translationMap.es,
                translationMap.pt,
                translationMap.nl,
                translationMap.pl,
            )

        for (labels in allLabels) {
            assertNotNull(labels.paginationOf)
            assertNotNull(labels.rarityLabel)
            assertNotNull(labels.releaseDateLabel)
            assertNotNull(labels.priceLabel)
            assertNotNull(labels.priceTrendLabel)
        }
    }

    @Test
    fun `german labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("von", translationMap.de.paginationOf)
        assertEquals("Rarität", translationMap.de.rarityLabel)
        assertEquals("Erschienen", translationMap.de.releaseDateLabel)
        assertEquals("ab", translationMap.de.priceLabel)
        assertEquals("Preis-Trend", translationMap.de.priceTrendLabel)
    }

    @Test
    fun `english labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("of", translationMap.en.paginationOf)
        assertEquals("Rarity", translationMap.en.rarityLabel)
        assertEquals("Released", translationMap.en.releaseDateLabel)
        assertEquals("from", translationMap.en.priceLabel)
        assertEquals("Price Trend", translationMap.en.priceTrendLabel)
    }

    @Test
    fun `french labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("de", translationMap.fr.paginationOf)
        assertEquals("Rareté", translationMap.fr.rarityLabel)
        assertEquals("Sorti", translationMap.fr.releaseDateLabel)
        assertEquals("à partir de", translationMap.fr.priceLabel)
        assertEquals("Tendance de prix", translationMap.fr.priceTrendLabel)
    }

    @Test
    fun `italian labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("di", translationMap.it.paginationOf)
        assertEquals("Rarità", translationMap.it.rarityLabel)
        assertEquals("Rilasciato", translationMap.it.releaseDateLabel)
        assertEquals("da", translationMap.it.priceLabel)
        assertEquals("Trend prezzo", translationMap.it.priceTrendLabel)
    }

    @Test
    fun `spanish labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("de", translationMap.es.paginationOf)
        assertEquals("Raridad", translationMap.es.rarityLabel)
        assertEquals("Lanzado", translationMap.es.releaseDateLabel)
        assertEquals("desde", translationMap.es.priceLabel)
        assertEquals("Tendencia de precio", translationMap.es.priceTrendLabel)
    }

    @Test
    fun `portuguese labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("de", translationMap.pt.paginationOf)
        assertEquals("Raridade", translationMap.pt.rarityLabel)
        assertEquals("Lançado", translationMap.pt.releaseDateLabel)
        assertEquals("a partir de", translationMap.pt.priceLabel)
        assertEquals("Tendência de preço", translationMap.pt.priceTrendLabel)
    }

    @Test
    fun `dutch labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("van", translationMap.nl.paginationOf)
        assertEquals("Zeldzaamheid", translationMap.nl.rarityLabel)
        assertEquals("Uitgebracht", translationMap.nl.releaseDateLabel)
        assertEquals("vanaf", translationMap.nl.priceLabel)
        assertEquals("Prijs trend", translationMap.nl.priceTrendLabel)
    }

    @Test
    fun `polish labels contain expected values`() {
        val translationMap = DEFAULT_TRANSLATION_MAP

        assertEquals("z", translationMap.pl.paginationOf)
        assertEquals("Rzadkość", translationMap.pl.rarityLabel)
        assertEquals("Wydany", translationMap.pl.releaseDateLabel)
        assertEquals("od", translationMap.pl.priceLabel)
        assertEquals("Trend ceny", translationMap.pl.priceTrendLabel)
    }
}
