package io.github.havonte1.tcgwatcher.backend.domain.model

enum class Genre(val identifier: String, val pathParam: String) {
    POKEMON("pokemon","Pokemon"),
    YUGIOH("yugioh","YuGiOh"),
    MTG("magic","Magic");

    companion object {
        private val map = entries.associateBy(Genre::identifier)

        fun fromId(id: String): Genre {
            val key = id.lowercase()
            if(map.containsKey(key))
              return map[key]!!
            else
                throw IllegalArgumentException("Unknown Genre $id")
        }
    }
}
