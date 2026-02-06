package com.github.havonte1.adapter.inbound.rest

import com.github.havonte1.adapter.inbound.rest.model.ProductDTO
import com.github.havonte1.domain.model.Product
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Instant

/**
 * Stateless mapper converting between the OpenAPI generated DTO ([ProductDTO])
 * and the core domain model ([Product]).
 *
 * Implemented as a Kotlin `object` (singleton) because it holds no mutable state.
 */
object CollectablesMapper {

}
