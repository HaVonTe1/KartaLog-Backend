package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable

/**
 * Entity representing a localized name translation for a product.
 * Stored as a separate table to allow many languages per product.
 */
@Entity
@Table(name = "product_name_translations", schema = "watcher")
class NameTranslationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity? = null,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "name", nullable = false)
    val name: String
) : Serializable {
    // JPA requires a no‑arg constructor
    constructor(): this(0, null, "", "")
}
