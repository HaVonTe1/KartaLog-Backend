package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SeriesEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SeriesJpaRepository : JpaRepository<SeriesEntity, Long> {
    fun findBySourceId(sourceId: String): SeriesEntity?
    fun existsBySourceId(sourceId: String): Boolean

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SeriesEntity s WHERE s.sourceId IS NOT NULL")
    fun existsBySourceIdIsNotNull(): Boolean
}
