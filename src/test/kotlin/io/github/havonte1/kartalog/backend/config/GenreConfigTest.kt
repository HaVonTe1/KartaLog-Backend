package io.github.havonte1.kartalog.backend.config

import io.github.havonte1.kartalog.backend.domain.model.Genre
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class GenreConfigTest {
    @Test
    fun `all genres in GENRES map exist`() {
        val genres = GenreConfig.GENRES

        assertEquals(3, genres.size)
        assertNotNull(genres[Genre.POKEMON])
        assertNotNull(genres[Genre.YUGIOH])
        assertNotNull(genres[Genre.MTG])
    }

    @Test
    fun `each genre has valid searchPathPattern`() {
        val pokemonConfig = GenreConfig.GENRES[Genre.POKEMON]
        val yuGiOhConfig = GenreConfig.GENRES[Genre.YUGIOH]
        val mtgConfig = GenreConfig.GENRES[Genre.MTG]

        assertNotNull(pokemonConfig?.searchPathPattern)
        assertNotNull(yuGiOhConfig?.searchPathPattern)
        assertNotNull(mtgConfig?.searchPathPattern)

        assertEquals("/%s/Pokemon", pokemonConfig?.searchPathPattern)
        assertEquals("/%s/YuGiOh", yuGiOhConfig?.searchPathPattern)
        assertEquals("/%s/Magic", mtgConfig?.searchPathPattern)
    }

    @Test
    fun `each genre has valid detailsPathPattern`() {
        val pokemonConfig = GenreConfig.GENRES[Genre.POKEMON]
        val yuGiOhConfig = GenreConfig.GENRES[Genre.YUGIOH]
        val mtgConfig = GenreConfig.GENRES[Genre.MTG]

        assertNotNull(pokemonConfig?.detailsPathPattern)
        assertNotNull(yuGiOhConfig?.detailsPathPattern)
        assertNotNull(mtgConfig?.detailsPathPattern)

        assertEquals("/%s/Pokemon/Products/%s", pokemonConfig?.detailsPathPattern)
        assertEquals("/%s/YuGiOh/Products/%s", yuGiOhConfig?.detailsPathPattern)
        assertEquals("/%s/Magic/Products/%s", mtgConfig?.detailsPathPattern)
    }

    @Test
    fun `genre config can be queried by Genre enum`() {
        val pokemonConfig = GenreConfig.GENRES[Genre.POKEMON]
        val yuGiOhConfig = GenreConfig.GENRES[Genre.YUGIOH]
        val mtgConfig = GenreConfig.GENRES[Genre.MTG]

        assertNotNull(pokemonConfig)
        assertNotNull(yuGiOhConfig)
        assertNotNull(mtgConfig)

        assertEquals(Genre.POKEMON, pokemonConfig?.genre)
        assertEquals(Genre.YUGIOH, yuGiOhConfig?.genre)
        assertEquals(Genre.MTG, mtgConfig?.genre)
    }
}
