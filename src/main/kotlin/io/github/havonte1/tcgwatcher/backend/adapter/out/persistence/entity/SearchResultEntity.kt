package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * JPA entity representing a cached search result.
 * Stores the original query string and the set of products returned by the scraper.
 */
@Entity
@Table(name = "search_results", schema = "watcher", uniqueConstraints = [UniqueConstraint(columnNames = ["query"])])
data class SearchResultEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val query: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @ManyToMany
    @JoinTable(
        name = "search_result_products",
        schema = "watcher",
        joinColumns = [JoinColumn(name = "search_result_id")],
        inverseJoinColumns = [JoinColumn(name = "product_id")]
    )
    val products: MutableSet<ProductEntity> = mutableSetOf()
) : java.io.Serializable {
    // JPA requires a no‑arg constructor
    constructor() : this(0, "", Instant.now(), mutableSetOf())
}
