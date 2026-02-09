package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for collectables.
 *
 * Currently delegates to a placeholder implementation that returns an empty list.
 * A proper service layer should be injected and used once it is available.
 */
@RestController
class CollectablesAdapter : CollectablesApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun listCollectables(
        page: Int,
        size: Int,
        query: String?
    ): ResponseEntity<List<ProductDTO>> {
        logger.debug("listCollectables called with page={}, size={}, query={}", page, size, query)
        // Placeholder: return an empty list. Replace with real service call later.
        return ResponseEntity(emptyList(), HttpStatus.OK)
    }
}
