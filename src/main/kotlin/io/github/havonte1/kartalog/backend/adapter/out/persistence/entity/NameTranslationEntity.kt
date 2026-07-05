package io.github.havonte1.kartalog.backend.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.io.Serializable

@Audited
@Entity
@Table(name = "product_name_translations", schema = "watcher")
class NameTranslationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: ProductEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    var series: SeriesEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    var productSet: ProductSetEntity? = null,
    @Column(name = "language_code", nullable = false)
    val languageCode: String,
    @Column(name = "name", nullable = false)
    var name: String,
) : Serializable {
    constructor() : this(0, null, null, null, "", "")

    @PostLoad
    fun validateParent() {
        val parentCount = listOfNotNull(product, series, productSet).size
        check(parentCount == 1) { "NameTranslationEntity must have exactly one parent, but found $parentCount" }
    }
}
