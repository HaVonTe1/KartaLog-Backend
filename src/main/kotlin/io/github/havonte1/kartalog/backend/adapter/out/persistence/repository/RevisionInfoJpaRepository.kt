package io.github.havonte1.kartalog.backend.adapter.out.persistence.repository

import io.github.havonte1.kartalog.backend.adapter.out.persistence.entity.RevisionInfoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** Repository for accessing Envers revision information. */
@Repository
interface RevisionInfoJpaRepository : JpaRepository<RevisionInfoEntity, Long>
