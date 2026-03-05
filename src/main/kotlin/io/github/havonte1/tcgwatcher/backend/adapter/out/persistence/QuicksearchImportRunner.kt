package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SeriesEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.NameTranslationJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.ProductJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.ProductSetJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.SeriesJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DriverManager

@Component
@Order(Int.MAX_VALUE)
@ConditionalOnProperty(name = ["app.data.import.enabled"], havingValue = "true", matchIfMissing = false)
class QuicksearchImportRunner(
    private val seriesJpaRepository: SeriesJpaRepository,
    private val productSetJpaRepository: ProductSetJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val nameTranslationJpaRepository: NameTranslationJpaRepository
) : ApplicationRunner {

    private val logger = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments) {
        logger.info { "Starting quicksearch data import from SQLite" }

        if (seriesJpaRepository.existsBySourceIdIsNotNull()) {
            logger.info { "Quicksearch data already imported, skipping" }
            return
        }

        val sqliteConnection = getSqliteConnection()
        try {
            importSeries(sqliteConnection)
            val seriesMap = buildSeriesMap()

            importSets(sqliteConnection, seriesMap)
            val setsMap = buildSetsMap()

            importCards(sqliteConnection, setsMap)

            logger.info { "Quicksearch data import completed successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to import quicksearch data" }
            throw e
        } finally {
            sqliteConnection.close()
        }
    }

    private fun getSqliteConnection(): Connection {
        //TODO: make this parameter come from the application.yml
        val dbPath = System.getProperty("user.dir") + "/quicksearch.db"
        logger.info { "Opening SQLite database at: $dbPath" }
        return DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }

    private fun importSeries(conn: Connection) {
        logger.info { "Importing series from SQLite" }
        val stmt = conn.prepareStatement("""
            SELECT id, name_de, name_en, name_fr
            FROM qs_pokemon_series
            WHERE name_de IS NOT NULL AND name_en IS NOT NULL AND name_fr IS NOT NULL
        """.trimIndent())

        val rs = stmt.executeQuery()
        var count = 0
        while (rs.next()) {
            val sourceId = rs.getString("id")
            val nameDe = rs.getString("name_de")
            val nameEn = rs.getString("name_en")
            val nameFr = rs.getString("name_fr")

            val series = SeriesEntity(sourceId = sourceId)
            val saved = seriesJpaRepository.save(series)

            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "de",
                name = nameDe,
                series = saved
            ))
            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "en",
                name = nameEn,
                series = saved
            ))
            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "fr",
                name = nameFr,
                series = saved
            ))

            count++
        }
        logger.info { "Imported $count series with translations" }
        rs.close()
        stmt.close()
    }

    private fun buildSeriesMap(): Map<String, Long> {
        return seriesJpaRepository.findAll()
            .filter { it.sourceId != null }
            .associate { it.sourceId!! to it.id!! }
    }

    private fun importSets(conn: Connection, seriesMap: Map<String, Long>) {
        logger.info { "Importing sets from SQLite" }
        val stmt = conn.prepareStatement("""
            SELECT id, abbreviation, cm_product_id, code, name_de, name_en, name_fr, official, tcgp_id, total, series_id
            FROM qs_pokemon_sets
            WHERE name_de IS NOT NULL AND name_en IS NOT NULL AND name_fr IS NOT NULL AND code IS NOT NULL
        """.trimIndent())

        val rs = stmt.executeQuery()
        var count = 0
        while (rs.next()) {
            val sourceId = rs.getString("id")
            val abbreviation = rs.getString("abbreviation")
            val cmProductId = rs.getString("cm_product_id")
            val code = rs.getString("code")
            val nameDe = rs.getString("name_de")
            val nameEn = rs.getString("name_en")
            val nameFr = rs.getString("name_fr")
            val official = rs.getInt("official")
            val tcgpId = rs.getString("tcgp_id")
            val total = rs.getInt("total")
            val seriesSourceId = rs.getString("series_id")

            val seriesId = seriesMap[seriesSourceId]
            if (seriesId == null) {
                logger.warn { "Skipping set $sourceId: series not found for series_id=$seriesSourceId" }
                continue
            }

            val seriesEntity = seriesJpaRepository.findById(seriesId).orElse(null)

            val productSet = ProductSetEntity(
                sourceId = sourceId,
                abbreviation = abbreviation,
                cmProductId = cmProductId,
                code = code,
                official = official,
                tcgpId = tcgpId,
                total = total,
                series = seriesEntity
            )
            val saved = productSetJpaRepository.save(productSet)

            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "de",
                name = nameDe,
                productSet = saved
            ))
            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "en",
                name = nameEn,
                productSet = saved
            ))
            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "fr",
                name = nameFr,
                productSet = saved
            ))

            count++
        }
        logger.info { "Imported $count sets with translations" }
        rs.close()
        stmt.close()
    }

    private fun buildSetsMap(): Map<String, Long> {
        return productSetJpaRepository.findAll()
            .filter { it.sourceId != null }
            .associate { it.sourceId!! to it.id!! }
    }

    private fun importCards(conn: Connection, setsMap: Map<String, Long>) {
        logger.info { "Importing cards from SQLite" }
        val stmt = conn.prepareStatement("""
            SELECT id, cm_page_id, cm_product_id, code, name_de, name_en, name_fr, set_id, tcgp_id
            FROM qs_pokemon_cards
            WHERE name_de IS NOT NULL AND name_en IS NOT NULL AND name_fr IS NOT NULL AND code IS NOT NULL
        """.trimIndent())

        val rs = stmt.executeQuery()
        var count = 0
        while (rs.next()) {
            val sourceId = rs.getString("id")
            val cmPageId = rs.getString("cm_page_id")
            val cmProductId = rs.getString("cm_product_id")
            val code = rs.getString("code")
            val nameDe = rs.getString("name_de")
            val nameEn = rs.getString("name_en")
            val nameFr = rs.getString("name_fr")
            val setSourceId = rs.getString("set_id")
            val tcgpId = rs.getString("tcgp_id")

            val setId = setsMap[setSourceId]
            if (setId == null) {
                logger.warn { "Skipping card $sourceId: set not found for set_id=$setSourceId" }
                continue
            }

            val externalId = cmProductId?.toLongOrNull()
            if(externalId == null) {
                logger.warn { "Skipping card $sourceId: external_id not found for product_id=$externalId" }
                continue
            }

            val productSetEntity = productSetJpaRepository.findById(setId).orElse(null)

            val product = ProductEntity(
                externalId = externalId,
                sourceId = sourceId,
                cmId = cmPageId,
                setId = setId,
                codeInfo = code,
                genre = "Pokemon",
                type = "Singles",
                productSet = productSetEntity
            )

            if(productJpaRepository.existsByExternalId(externalId)) {
                logger.warn { "Product already exists for external_id=$externalId" }
                continue
            }
            val saved = productJpaRepository.save(product)

            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "de",
                name = nameDe,
                product = saved
            ))
            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "en",
                name = nameEn,
                product = saved
            ))
            nameTranslationJpaRepository.save(NameTranslationEntity(
                languageCode = "fr",
                name = nameFr,
                product = saved
            ))

            count++
        }
        logger.info { "Imported $count cards with translations" }
        rs.close()
        stmt.close()
    }
}
