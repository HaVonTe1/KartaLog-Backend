# Testing Patterns

**Analysis Date:** 2026-04-03

## Framework

**Runner:**
- JUnit 5 (Jupiter)
- Config: `build.gradle.kts` — `useJUnitPlatform { excludeTags = setOf("integration", "e2e") }` for default `test` task
- Separate `integrationTest` task with `includeTags("integration")`

**Assertion Library:**
- `org.junit.jupiter.api.Assertions` (assertEquals, assertTrue, assertThrows)
- `kotlin.test.fail` for explicit failures

**Mocking:**
- MockK 1.13.12 (`io.mockk:mockk`)
- Patterns: `mockk()`, `every { } returns`, `coEvery { } returns` for coroutines, `answers { firstArg() }`

**Run Commands:**
```bash
./gradlew test                          # Run unit tests (excludes integration/e2e tags)
./gradlew integrationTest               # Run integration tests only (*IT, @Tag("integration"))
./gradlew test --tests "ClassName"      # Run single test class
./gradlew test --tests "ClassName.method name"  # Run single test method
./gradlew test --tests "*IT"            # Run all integration tests
./gradlew test -i                       # Verbose output (for debugging flaky tests)
./gradlew test jacocoTestReport         # Generate coverage report
```

## Test Structure

**Location:**
- Unit tests: `src/test/kotlin/` mirroring main source structure
- Integration tests: `src/test/kotlin/` with `*IT.kt` suffix or `@Tag("integration")`
- Test fixtures: `src/test/resources/` (HTML snapshots, static resources)

**Naming:**
- Unit test classes: `ClassNameTest` (e.g., `CardMarketScraperAdapterTest`)
- Integration test classes: `ClassNameIT` (e.g., `CollectablesServiceIT`)
- Test methods: backtick descriptions (e.g., `` `search returns one product` ``)
- Test methods also use: `shouldDoSomethingWhenCondition`

**Directory Structure:**
```
src/test/kotlin/io/github/havonte1/tcgwatcher/backend/
├── application/
│   └── CollectablesServiceTest.kt          # Unit test
├── adapter/
│   ├── inbound/rest/
│   │   └── CollectablesAdapterIT.kt        # Integration test
│   ├── out/
│   │   ├── persistence/
│   │   │   └── QuicksearchImportRunnerTest.kt  # Integration test (with @Tag("integration"))
│   │   └── webscraper/cardmarket/
│   │       ├── CardMarketScraperAdapterTest.kt   # Unit test
│   │       ├── CardMarketScraperAdapterIT.kt     # Integration test
│   │       ├── CardMarketContentParserTest.kt    # Unit test
│   │       └── CardMarketProductMapperTest.kt    # Unit test
├── CollectablesServiceIT.kt                # Integration test
└── SearchResultProductBehaviorIT.kt        # Integration test
```

## Test Patterns

**Unit Test Pattern (MockK + runBlocking):**
```kotlin
class CollectablesServiceTest {
    private val mockScraperPort: CardMarketScraperPort = mockk()
    private val mockSearchResultRepository: SearchResultRepository = mockk()
    private val mockProductRepository: ProductRepository = mockk()

    private lateinit var service: CollectablesService

    @BeforeEach
    fun setUp() {
        service = CollectablesService(mockScraperPort, mockSearchResultRepository, productRepository = mockProductRepository)
    }

    @Test
    fun `search returns products from scraper`() {
        val products = listOf(Product(externalId = 1L, cmId = "CM001", ...))

        every { mockSearchResultRepository.findByQuery(query) } returns null
        coEvery { mockScraperPort.search(query, locale, game) } returns products
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(1, result.size)
        assertEquals("CM001", result[0].cmId)
    }
}
```

**Integration Test Pattern (Testcontainers + @SpringBootTest):**
```kotlin
@SpringBootTest
@Testcontainers
@Tag("integration")
class CollectablesServiceIT {
    @TestConfiguration
    class ScraperTestConfig {
        @Bean
        @Primary
        fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
            override suspend fun search(...): List<Product> { ... }
            override suspend fun fetchProductDetails(...): Product? = null
        }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
    }

    @Autowired
    lateinit var service: SearchUseCase

    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
    }

    @Test
    fun `cache miss then hit`() {
        Assumptions.assumeTrue(File(testFile).exists())
        val result = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(30, result.size)
    }
}
```

**Fixture-Based Unit Test Pattern:**
```kotlin
class CardMarketContentParserTest {
    private val parser = CardMarketContentParser()

    @Test
    fun `extractProductsFromHtml parses view-source HTML correctly`() {
        val resourcePath = "src/test/resources/pikachu_gallery_30.html"
        val file = File(resourcePath)
        Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")
        val content = Files.readString(Paths.get(resourcePath))
        val products = parser.parseGalaryPage(content, 1)

        assertTrue(products.results.isNotEmpty())
        assertEquals(30, products.results.size)
    }
}
```

**Inline Test Implementation Pattern (for port interfaces):**
```kotlin
class TestCardMarketWebFetcher : CardMarketWebFetcherPort {
    override fun fetch(searchString: String, locale: String, game: String): Result<String> =
        Result.success(Files.readString(Paths.get(resourcePath)))

    override fun fetchDetails(...): Result<String> =
        Result.failure(UnsupportedOperationException("Not implemented"))
}
```

**Error Testing Pattern:**
```kotlin
@Test
fun `fetchProductDetails throws 400 when setname is blank`() {
    every { mockProductRepository.findByCmId(cmId) } returns null

    val exception = assertThrows(ResponseStatusException::class.java) {
        runBlocking { service.fetchProductDetails(cmId, genre, type, lang, "") }
    }

    assertEquals(400, exception.statusCode.value())
    assertEquals("no setname provided", exception.reason)
}
```

## Mocking

**Framework:** MockK 1.13.12

**Patterns:**
- `mockk()` — create mock instance
- `every { mock.method(args) } returns value` — stub synchronous methods
- `coEvery { mock.suspendMethod(args) } returns value` — stub suspend functions
- `every { mock.method(any()) } answers { firstArg() }` — return first argument
- `coEvery { mock.method(...) } returns null` — return nullable

**What to Mock:**
- Port interfaces (e.g., `CardMarketScraperPort`, `ProductRepository`, `SearchResultRepository`)
- External services in unit tests
- Web fetcher ports with inline test implementations reading from fixtures

**What NOT to Mock:**
- Testcontainers-managed PostgreSQL in integration tests
- Real parser/mapper instances in unit tests (they are pure functions)

## Fixtures and Factories

**Test Data:**
- HTML fixtures stored in `src/test/resources/`:
  - `pikachu_gallery_30.html` — search results page (30 products)
  - `pikachu_gallery_40.html` — search results page (40 products, different prices)
  - `pikachu_mcd166_details_stripped.html` — product detail page (view-source)
  - `pikachu_mcd166_details_playwright_stripped.html` — product detail page (Playwright-rendered)
  - `evoli_details_stripped.html` — another product detail page

**Loading Fixtures:**
```kotlin
val resourcePath = "src/test/resources/pikachu_gallery_30.html"
Assumptions.assumeTrue(File(resourcePath).exists())
val content = Files.readString(Paths.get(resourcePath))
```

**Location:** `src/test/resources/`

## Coverage

**Requirements:** JaCoCo configured (run via `./gradlew test jacocoTestReport`)
**No minimum threshold enforced** in build configuration

**View Coverage:**
```bash
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

## Test Types

**Unit Tests:**
- Located in `src/test/kotlin/` with `*Test.kt` suffix
- Focus on domain logic, application services, parsers, and mappers
- Use MockK for mocking port interfaces
- Use `runBlocking` for testing suspend functions
- No Spring context loading
- Excluded from default `test` task if tagged with `integration`

**Integration Tests:**
- Located in `src/test/kotlin/` with `*IT.kt` suffix
- Tagged with `@Tag("integration")`
- Use `@SpringBootTest` to load full Spring context
- Use Testcontainers for PostgreSQL (`PostgreSQLContainer`)
- Use `@ServiceConnection` for automatic datasource configuration
- Use `@TestConfiguration` with `@Primary` beans to replace external dependencies
- Use `@BeforeEach` to clean database state (`deleteAll()`)
- Use `@DirtiesContext` when context isolation is needed (e.g., `QuicksearchImportRunnerTest`)

**E2E Tests:**
- Not currently used (excluded from default test task via `excludeTags = setOf("integration", "e2e")`)

## Common Patterns

**Async Testing:**
```kotlin
@Test
fun `search returns products from scraper`() {
    coEvery { mockScraperPort.search(query, locale, game) } returns products
    val result = runBlocking { service.search(query, locale, game) }
    assertEquals(1, result.size)
}
```

**Error Testing:**
```kotlin
@Test
fun `throws 400 when setname is blank`() {
    val exception = assertThrows(ResponseStatusException::class.java) {
        runBlocking { service.fetchProductDetails(cmId, genre, type, lang, "") }
    }
    assertEquals(400, exception.statusCode.value())
}
```

**Conditional Test Skipping:**
```kotlin
Assumptions.assumeTrue(file.exists(), "Ressource fehlt, Test wird übersprungen")
```

**Database Cleanup:**
```kotlin
@BeforeEach
fun cleanDb() {
    productRepo.deleteAll()
    searchRepo.deleteAll()
    cacheManager.getCacheNames().forEach { cacheManager.getCache(it)?.clear() }
}
```

**Testcontainers Setup:**
```kotlin
companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer("postgres:15-alpine")
}
```

**Test Configuration Override:**
```kotlin
@TestConfiguration
class ScraperTestConfig {
    @Bean
    @Primary
    fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
        // test implementation
    }
}
```

---

*Testing analysis: 2026-04-03*