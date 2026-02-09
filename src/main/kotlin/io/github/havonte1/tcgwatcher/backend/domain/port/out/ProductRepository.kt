package io.github.havonte1.tcgwatcher.backend.domain.port.out

import io.github.havonte1.tcgwatcher.backend.domain.model.Product


/**
 * Outbound port (driven adapter) for persisting and retrieving [Product] aggregates.
 *
 * Implementations live in the adapter/persistence module and use JPA/Hibernate, but the
 * domain core only depends on this abstraction, preserving hexagonal architecture.
 */
interface ProductRepository {
    /** Persist a [Product] (insert or update) and return the managed entity. */
    fun save(product: Product): Product

    /** Find a product by its internal id. */
    fun findById(id: Long): Product?

    /** Find a product by the external CardMarket id. */
    fun findByExternalId(externalId: Long): Product?

    /** Retrieve all products. */
    fun findAll(): List<Product>

    /** Delete a product. */
    fun delete(product: Product)
}
