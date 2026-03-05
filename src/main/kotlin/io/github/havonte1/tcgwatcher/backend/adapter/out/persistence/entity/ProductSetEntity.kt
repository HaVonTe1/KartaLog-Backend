package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.io.Serializable

/**
 * Entity representing a Pokemon set (renamed to avoid SQL keyword "set").
 * Uses the same translation table via NameTranslationEntity.
 */
@Audited
@Entity
@Table(name = "product_set", schema = "watcher")
data class ProductSetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "source_id")
    val sourceId: String? = null,

    @Column(name = "abbreviation")
    val abbreviation: String? = null,

    @Column(name = "cm_product_id")
    val cmProductId: String? = null,

    @Column(name = "code")
    val code: String? = null,

    @Column(name = "official")
    val official: Int? = null,

    @Column(name = "tcgp_id")
    val tcgpId: String? = null,

    @Column(name = "total")
    val total: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", referencedColumnName = "id")
    val series: SeriesEntity? = null,

    @OneToMany(
        mappedBy = "productSet",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val nameTranslations: MutableSet<NameTranslationEntity> = mutableSetOf()
) : Serializable