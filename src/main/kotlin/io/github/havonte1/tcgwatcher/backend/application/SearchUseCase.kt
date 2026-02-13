package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product

/**
 * Use‑case for searching collectable products.
 * Currently provides a single method that returns a list of domain [Product] objects.
 */
interface SearchUseCase {

    /** Overloaded search allowing locale and game parameters. */
    suspend fun search(searchString: String, locale: String, game: String): List<Product>
}
