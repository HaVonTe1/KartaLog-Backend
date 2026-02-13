package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertTrue

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
        val postgres = PostgreSQLContainer("postgres:18.1-alpine")
    }

    @Test
    fun `GET collectables returns empty list on successful request`() {
        // Use Mockito stubbing for the @MockitoBean
        runBlocking {
            Mockito.`when`(searchUseCase.search("test", "en", "Pokemon")).thenReturn(emptyList())
        }

        val mvcResult = mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
            param("page", "0")
            param("size", "10")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        // Ensure overall request succeeded
        assertEquals(200, dispatched.response.status)
    }

    @Test
    fun `GET collectables with negative page returns server error`() {
        val mvcResult = mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
            param("page", "-1")
            param("size", "10")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        val resolved = dispatched.resolvedException ?: dispatched.asyncResult as? Exception
        val message = resolved?.cause?.message ?: resolved?.message ?: ""
        assertTrue(
            message.contains("must be greater than or equal to 0"),
            "Expected validation message but was: $message"
        )
        // Ensure overall request succeeded
        assertEquals(400, dispatched.response.status)
    }

    @Test
    fun `GET collectables with zero size returns server error`() {
        val mvcResult = mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
            param("page", "0")
            param("size", "0")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        val resolved = dispatched.resolvedException ?: dispatched.asyncResult as? Exception
        val message = resolved?.cause?.message ?: resolved?.message ?: ""
        assertTrue(
            message.contains("must be greater than or equal to 1"),
            "Expected validation message but was: $message"
        )
        // Ensure overall request succeeded
        assertEquals(400, dispatched.response.status)
    }

    @Test
    fun `GET collectables with blank query returns server error`() {
        // Current behavior throws an exception from the controller when query is blank.
        try {
            val mvcResult = mockMvc.get("/collectables/") {
                param("query", "   ")
                param("locale", "en")
                param("game", "Pokemon")
                param("page", "0")
                param("size", "10")
            }
                .andExpect { request { asyncStarted() } }
                .andReturn()

            val dispatched = mockMvc.perform(
                MockMvcRequestBuilders.asyncDispatch(mvcResult)
            ).andReturn()

            val resolved = dispatched.resolvedException ?: dispatched.asyncResult as? Exception
            val message = resolved?.cause?.message ?: resolved?.message ?: ""
            assertTrue(
                message.contains("Query must not be blank"),
                "Expected exception message to mention blank query but was: $message"
            )
            // Ensure overall request succeeded
            assertEquals(400, dispatched.response.status)
        } catch (ex: Exception) {
            val message = ex.cause?.message ?: ex.message ?: ""
            assertTrue(
                message.contains("Query must not be blank"),
                "Expected exception message to mention blank query but was: $message"
            )
        }
    }
}
