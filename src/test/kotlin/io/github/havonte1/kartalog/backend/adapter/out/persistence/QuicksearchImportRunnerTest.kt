package io.github.havonte1.kartalog.backend.adapter.out.persistence

import io.github.havonte1.kartalog.backend.adapter.out.persistence.repository.NameTranslationJpaRepository
import io.github.havonte1.kartalog.backend.adapter.out.persistence.repository.ProductJpaRepository
import io.github.havonte1.kartalog.backend.adapter.out.persistence.repository.ProductSetJpaRepository
import io.github.havonte1.kartalog.backend.adapter.out.persistence.repository.SeriesJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@TestPropertySource(properties = ["app.data.import.enabled=true"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QuicksearchImportRunnerTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)
    }

    @Autowired
    lateinit var seriesRepo: SeriesJpaRepository

    @Autowired
    lateinit var productSetRepo: ProductSetJpaRepository

    @Autowired
    lateinit var productRepo: ProductJpaRepository

    @Autowired
    lateinit var translationRepo: NameTranslationJpaRepository

//    @BeforeEach
//    fun cleanDb() {
//        translationRepo.deleteAll()
//        productRepo.deleteAll()
//        productSetRepo.deleteAll()
//        seriesRepo.deleteAll()
//    }

    @Test
    fun `import runs on startup`() {
        // The runner will load the database from classpath resources
        // After context startup, the runner has executed.
        // Verify counts
        assertEquals(20, seriesRepo.count(), "Series count should be 20")
        assertEquals(778, productSetRepo.count(), "Product set count should be 916")
        assertEquals(972, productRepo.count(), "Product count should be 972")
    }

    @Test
    fun `import is idempotent on second startup`() {
        // Context is reloaded due to @DirtiesContext after previous test.
        // Verify that counts are unchanged (no duplicates)
        assertEquals(20, seriesRepo.count(), "Series count should remain 20 after second startup")
        assertEquals(778, productSetRepo.count(), "Product set count should remain 192 after second startup")
        assertEquals(972, productRepo.count(), "Product count should remain 972 after second startup")
    }
}
