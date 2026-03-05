package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.io.Serializable

/**
 * Entity representing a Pokemon series.
 * Uses the same i18n pattern as ProductEntity via NameTranslationEntity.
 */
@Audited
@Entity
@Table(name = "series", schema = "watcher")
data class SeriesEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "source_id")
    val sourceId: String? = null,

    @OneToMany(
        mappedBy = "series",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val nameTranslations: MutableSet<NameTranslationEntity> = mutableSetOf()
) : Serializable