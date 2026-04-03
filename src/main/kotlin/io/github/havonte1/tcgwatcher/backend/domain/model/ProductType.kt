package io.github.havonte1.tcgwatcher.backend.domain.model

enum class ProductType(val cmIdentifier: String) {
    SINGLES("Singles"),
    BOOSTERS("Boosters"),
    BOXES("Box-Sets"),
    SEALED("Sealed-Products"),
    SETCOLLECTIONS("Sets-Lots-Collections"),
    TINS("Tins"),
    COINS("Coins"),
    SLEEVES("Sleeves"),
    DIVIDERS("Dividers"),
    PLAYMATS("Playmats"),
    THEMEDECKS("Theme-Decks"),
    DECKBOXES("Deck-Boxes"),
    ALBUMS("Albums"),
    LOTS("Lots"),
    STORAGE("Storage"),
    DICE("Dice"),
    MEMORABILIA("Memorabilia"),
    BLISTER("Blisters"),
    BOOSTERBOX("Booster-Boxes");

    // add all remaining

    companion object {
        private val map = ProductType.entries.associateBy(ProductType::cmIdentifier)

        fun fromId(code: String): ProductType {
            if (map.containsKey(code)) {
                return map[code]!!
            }
            throw IllegalArgumentException("Unknown product type $code")
        }
    }
}
