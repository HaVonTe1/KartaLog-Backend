package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import org.mockito.Mockito
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CollectablesAdapterIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var searchUseCase: SearchUseCase

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
    }


    @Test
    fun `GET collectables returns empty list on successful request`() {
        // Use Mockito stubbing for the @MockitoBean
        Mockito.`when`(searchUseCase.search("test", "en", "Pokemon")).thenReturn(emptyList())
        mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
            param("page", "0")
            param("size", "10")
        }
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { content { json("[]") } }
    }

    @Test
    fun `GET collectables with negative page returns server error`() {
        mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
            param("page", "-1")
            param("size", "10")
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `GET collectables with zero size returns server error`() {
        mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
            param("page", "0")
            param("size", "0")
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `GET collectables with blank query returns server error`() {
        // Current behavior throws an exception from the controller when query is blank.
        val ex = assertThrows(Exception::class.java) {
            mockMvc.get("/collectables/") {
                param("query", "   ")
                param("locale", "en")
                param("game", "Pokemon")
                param("page", "0")
                param("size", "10")
            }
        }
        // Ensure the root cause mentions the blank query
        val message = ex.cause?.message ?: ex.message ?: ""
        kotlin.test.assertTrue(message.contains("Query must not be blank"), "Expected exception message to mention blank query but was: $message")
    }
}
