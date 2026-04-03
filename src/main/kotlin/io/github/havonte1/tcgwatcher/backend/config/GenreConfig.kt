package io.github.havonte1.tcgwatcher.backend.config

import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType

data class GenreConfigData(
    val genre: Genre,
    val searchPathPattern: String,
    val detailsPathPattern: String,
)


object GenreConfig {
    val GENRES: Map<Genre, GenreConfigData> = mapOf(
        Genre.POKEMON to GenreConfigData(
            genre = Genre.POKEMON,
            searchPathPattern = "/%s/${Genre.POKEMON.pathParam}",
            detailsPathPattern = "/%s/${Genre.POKEMON.pathParam}/Products/%s",
        ),
        Genre.YUGIOH to GenreConfigData(
            genre = Genre.YUGIOH,
            searchPathPattern = "/%s/${Genre.YUGIOH.pathParam}",
            detailsPathPattern = "/%s/${Genre.YUGIOH.pathParam}/Products/%s",
        ),
        Genre.MTG to GenreConfigData(
            genre = Genre.MTG,
            searchPathPattern = "/%s/${Genre.MTG.pathParam}",
            detailsPathPattern = "/%s/${Genre.MTG.pathParam}/Products/%s",
        ),
    )

    fun buildDetailsUrlBase(genre: Genre, locale: Locale, type: ProductType) : String {
        GENRES[genre]?.detailsPathPattern?.let {
            val path = String.format(it, locale.code, type.cmIdentifier)
            return path
        }
        throw IllegalArgumentException("Unknown genre ${genre.identifier}")
    }
}
