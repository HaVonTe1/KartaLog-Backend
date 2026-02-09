package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.domain.model.Product


/**
 * Use‑case for searching collectable products.
 * Currently provides a single method that returns a list of domain [Product] objects.
 */
interface SearchUseCase {
    /** Return a list of products that match the (future) search criteria. */
    fun search(searchString: String): List<Product>
}
