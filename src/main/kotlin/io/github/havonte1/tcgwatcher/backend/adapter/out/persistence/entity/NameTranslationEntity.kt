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
import org.hibernate.envers.Audited
import java.io.Serializable

/**
 * Entity representing a localized name translation.
 * This entity is reused for products, series, and sets by having nullable foreign
 * keys to each parent entity. Exactly one of the foreign‑key fields should be
 * non‑null for a given row.
 */
@Audited
@Entity
@Table(name = "product_name_translations", schema = "watcher")
class NameTranslationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // Parent product (original use case)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: ProductEntity? = null,

    // Optional parent series – reuses the same table for i18n
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    var series: SeriesEntity? = null,

    // Optional parent product set – reuses the same table for i18n
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    var productSet: ProductSetEntity? = null,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "name", nullable = false)
    var name: String
) : Serializable {
    // JPA requires a no‑arg constructor
    constructor() : this(0, null, null, null, "", "")
}
