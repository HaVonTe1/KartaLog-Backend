package com.github.havonte1.application

import com.github.havonte1.domain.model.Product
import org.springframework.stereotype.Service

/**
 * Spring service implementing the [SearchUseCase].
 * For now it simply returns an empty list – real business logic will be added later.
 */
@Service
class CollectablesService : SearchUseCase {

    /** Returns an empty list of [Product]. */
    override fun search(): List<Product> = emptyList()
}
