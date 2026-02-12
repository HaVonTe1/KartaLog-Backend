package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class CollectablesAdapterTest {

    private val mockService = mockk<SearchUseCase>()
    private val adapter = CollectablesAdapter(mockService)

    @Test
    fun `listCollectables throws when page is negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { adapter.listCollectables(query = "test", page = -1, size = 10, locale = "en", game = "Pokemon") }
        }
    }

    @Test
    fun `listCollectables throws when size is non‑positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {  adapter.listCollectables(query = "test", page = 0, size = 0, locale = "en", game = "Pokemon")}
        }
    }

    @Test
    fun `listCollectables throws when query is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { adapter.listCollectables(query = "   ", page = 0, size = 10, locale = "en", game = "Pokemon")}
        }
    }

    @Test
    fun `listCollectables returns empty list when service yields none`() {
        coEvery { mockService.search("test", "en", "Pokemon") } returns emptyList()
        val response: ResponseEntity<List<ProductDTO>> =
            runBlocking { adapter.listCollectables(query = "test", page = 0, size = 10, locale = "en", game = "Pokemon")}
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, response.body?.size)
    }
}
