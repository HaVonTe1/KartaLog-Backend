package com.github.havonte1.adapter.inbound.rest.api

import com.github.havonte1.adapter.inbound.rest.model.ProductDTO
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import java.io.InputStream
import jakarta.validation.constraints.*
import jakarta.validation.Valid

@Path("/")
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinServerCodegen"], comments = "Generator version: 7.19.0")
interface CollectablesApi {

    @GET
    @Path("/collectables/")
    @Produces("application/json")
    fun listCollectables(
        @QueryParam("page") @Min(0) @DefaultValue("0") page: kotlin.Int,
        @QueryParam("size") @Min(1) @DefaultValue("20") size: kotlin.Int,
        @QueryParam("query") query: kotlin.String?
    ): kotlin.collections.List<ProductDTO>
}
