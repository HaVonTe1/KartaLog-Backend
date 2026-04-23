package io.github.havonte1.tcgwatcher.backend.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LocaleTest {
    @Test
    fun `all 8 CardMarket locales exist`() {
        val locales =
            listOf(
                Locale.GERMAN,
                Locale.ENGLISH,
                Locale.FRENCH,
                Locale.ITALIAN,
                Locale.SPANISH,
                Locale.PORTUGUESE,
                Locale.DUTCH,
                Locale.POLISH,
            )

        assertEquals(8, locales.size)
    }

    @Test
    fun `each locale has correct code property`() {
        assertEquals("de", Locale.GERMAN.code)
        assertEquals("en", Locale.ENGLISH.code)
        assertEquals("fr", Locale.FRENCH.code)
        assertEquals("it", Locale.ITALIAN.code)
        assertEquals("es", Locale.SPANISH.code)
        assertEquals("pt", Locale.PORTUGUESE.code)
        assertEquals("nl", Locale.DUTCH.code)
        assertEquals("pl", Locale.POLISH.code)
    }

    @Test
    fun `locale can be used as map keys`() {
        val localeMap: HashMap<Locale, String> = HashMap()

        localeMap[Locale.GERMAN] = "German"
        localeMap[Locale.ENGLISH] = "English"
        localeMap[Locale.FRENCH] = "French"
        localeMap[Locale.ITALIAN] = "Italian"
        localeMap[Locale.SPANISH] = "Spanish"
        localeMap[Locale.PORTUGUESE] = "Portuguese"
        localeMap[Locale.DUTCH] = "Dutch"
        localeMap[Locale.POLISH] = "Polish"

        assertEquals(8, localeMap.size)

        assertNotNull(localeMap[Locale.GERMAN])
        assertNotNull(localeMap[Locale.ENGLISH])
        assertNotNull(localeMap[Locale.FRENCH])
        assertNotNull(localeMap[Locale.ITALIAN])
        assertNotNull(localeMap[Locale.SPANISH])
        assertNotNull(localeMap[Locale.PORTUGUESE])
        assertNotNull(localeMap[Locale.DUTCH])
        assertNotNull(localeMap[Locale.POLISH])
    }
}
