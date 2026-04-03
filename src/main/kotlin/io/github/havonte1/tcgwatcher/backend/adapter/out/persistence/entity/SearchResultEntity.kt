package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * JPA entity representing a cached search result.
 * Stores the original query string and the set of products returned by the scraper.
 */
@Entity
@Table(name = "search_results", schema = "watcher", uniqueConstraints = [UniqueConstraint(columnNames = ["query"])])
class SearchResultEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = false)
    val query: String,
    @Column(nullable = false, unique = false)
    val language: String,
    @Column(nullable = false, unique = false)
    val genre: String,
    @Column(name = "cached_at")
    val cachedAt: Instant? = null,
    @ManyToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        fetch = FetchType.EAGER,
    )
    @JoinTable(
        name = "search_result_products",
        schema = "watcher",
        joinColumns = [JoinColumn(name = "search_result_id")],
        inverseJoinColumns = [JoinColumn(name = "product_id")],
    )
    val products: MutableSet<ProductEntity> = mutableSetOf(),
) : java.io.Serializable {
    // JPA requires a no‑arg constructor
    constructor() : this(0, "", "", "", Instant.now())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchResultEntity) return false
        if (id != null && other.id != null) return id == other.id
        return query == other.query
    }

    override fun hashCode(): Int = id?.hashCode() ?: query.hashCode()
}
