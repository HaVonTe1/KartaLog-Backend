package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api.CollectablesApi
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
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
class CollectablesAdapter(
    private val collectablesService: SearchUseCase
) : CollectablesApi {

    private val logger = KotlinLogging.logger {}

    override fun listCollectables(
        page: Int,
        size: Int,
        query: String?
    ): ResponseEntity<List<ProductDTO>> {
        logger.debug { "listCollectables called with page={$page}, size={$size}, query={$query}" }
        // Use empty string when query is null
        val results = collectablesService.search(query ?: "")
        // Simple pagination logic
        val from = (page * size).coerceAtMost(results.size)
        val to = ((page + 1) * size).coerceAtMost(results.size)
        val pageSlice = results.subList(from, to)
        val dtoList: List<ProductDTO> = pageSlice.map { CollectablesMapper.toDto(it) }
        return ResponseEntity(dtoList, HttpStatus.OK)
    }
}
