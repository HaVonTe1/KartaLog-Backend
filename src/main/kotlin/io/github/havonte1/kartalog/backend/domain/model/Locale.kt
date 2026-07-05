package io.github.havonte1.kartalog.backend.domain.model

enum class Locale(val code: String) {
    GERMAN("de"),
    ENGLISH("en"),
    FRENCH("fr"),
    ITALIAN("it"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    DUTCH("nl"),
    POLISH("pl");

    companion object {
        private val map = Locale.entries.associateBy(Locale::code)

        fun fromId(code: String): Locale {

            if (!map.containsKey(code)) {
                throw IllegalArgumentException("Locale $code doesn't exist")
            }
            return map[code]!!
        }
    }
}
