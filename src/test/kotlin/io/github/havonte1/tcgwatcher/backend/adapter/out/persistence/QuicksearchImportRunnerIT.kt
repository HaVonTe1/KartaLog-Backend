package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.NameTranslationJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.ProductJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.ProductSetJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.SeriesJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.QuicksearchImportRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File

@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QuicksearchImportRunnerTest {

    @Container
    @ServiceConnection
    val postgres = PostgreSQLContainer("postgres:15-alpine")

    @Autowired
    lateinit var seriesRepo: SeriesJpaRepository

    @Autowired
    lateinit var productSetRepo: ProductSetJpaRepository

    @Autowired
    lateinit var productRepo: ProductJpaRepository

    @Autowired
    lateinit var translationRepo: NameTranslationJpaRepository

    @BeforeEach
    fun cleanDb() {
        translationRepo.deleteAll()
        productRepo.deleteAll()
        productSetRepo.deleteAll()
        seriesRepo.deleteAll()
    }

    @Test
    fun `import runs on startup`() {
        val dbFile = File("quicksearch.db")
        assumeTrue(dbFile.exists(), "quicksearch.db must exist in project root for test")
        // After context startup, the runner has executed.
        // Verify counts
        assertEquals(20, seriesRepo.count(), "Series count should be 20")
        assertEquals(192, productSetRepo.count(), "Product set count should be 192")
        assertEquals(1861, productRepo.count(), "Product count should be 1861")
        // Verify translations for each entity
        seriesRepo.findAll().forEach { series ->
            assertEquals(3, series.nameTranslations.size, "Series ${"${series.id}"} should have 3 translations")
        }
        productSetRepo.findAll().forEach { set ->
            assertEquals(3, set.nameTranslations.size, "ProductSet ${"${set.id}"} should have 3 translations")
        }
        productRepo.findAll().forEach { product ->
            assertEquals(3, product.nameTranslations.size, "Product ${"${product.id}"} should have 3 translations")
        }
    }

    @Test
    fun `import is idempotent on second startup`() {
        val dbFile = File("quicksearch.db")
        assumeTrue(dbFile.exists(), "quicksearch.db must exist in project root for test")
        // Context is reloaded due to @DirtiesContext after previous test.
        // Verify that counts are unchanged (no duplicates)
        assertEquals(20, seriesRepo.count(), "Series count should remain 20 after second startup")
        assertEquals(192, productSetRepo.count(), "Product set count should remain 192 after second startup")
        assertEquals(1861, productRepo.count(), "Product count should remain 1861 after second startup")
    }
}
