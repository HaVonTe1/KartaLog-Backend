package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp

/**
 * Custom revision entity for Envers audit tracking.
 * Stores the revision number and timestamp. Additional columns can be added here
 * and will be automatically populated in the audit tables.
 */
@Entity
@RevisionEntity
@Table(name = "revinfo", schema = "watcher")
class RevisionInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev", nullable = false, updatable = false)
    var rev: Long = 0,
    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false, updatable = false)
    var revtstmp: Long = System.currentTimeMillis(),
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
