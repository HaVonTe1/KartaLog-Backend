package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api

import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*

@Path("/")
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinServerCodegen"], comments = "Generator version: 7.19.0")
interface CollectablesApi {

    @GET
    @Path("/collectables/")
    @Produces("application/json")
    fun listCollectables(
        @QueryParam("page") @Min(0) @DefaultValue("0") page: Int,
        @QueryParam("size") @Min(1) @DefaultValue("20") size: Int,
        @QueryParam("query") query: String?
    ): List<ProductDTO>
}
