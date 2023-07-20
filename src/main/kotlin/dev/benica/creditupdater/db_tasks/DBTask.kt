package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.db.ConnectionProvider
import dev.benica.creditupdater.db.ExtractionProgressTracker
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.di.*
import dev.benica.creditupdater.extractor.CharacterExtractor
import dev.benica.creditupdater.extractor.CreditExtractor
import dev.benica.creditupdater.extractor.Extractor
import mu.KLogger
import mu.KotlinLogging
import java.sql.SQLException
import kotlin.system.measureTimeMillis

/**
 * @constructor Create empty dev.benica.CreditUpdater.Doer
 * @property targetSchema The schema to use.
 */
class DBTask(
    private val targetSchema: String,
) {
    // Constants
    companion object {
        private const val DEFAULT_BATCH_SIZE = 75000
    }

    internal val queryExecutor: QueryExecutor = QueryExecutor()

    // Private Properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    // Public Methods
    /**
     * Extract characters and appearances - extracts characters and appearances
     * from the database.
     *
     * @param schema The schema with the stories_to_migrate table.
     * @param initial Whether this is the initial run.
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun extractCharactersAndAppearances(
        schema: String,
        initial: Boolean,
        startingId: Int? = null,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ) {
        logger.debug { "Starting Characters..." }

        val extractor = CharacterExtractor(schema)
        val lastIdCompleted = startingId
            ?: ExtractionProgressTracker.getLastProcessedItemId(extractor.extractedItem)
            ?: Credentials.CHARACTER_STORY_START_ID

        val table = if (initial) "gcd_story" else "stories_to_migrate"

        logger.debug { "Schema: $schema | Table: $table" }

        val selectStoriesQuery =
            """SELECT g.id, g.characters, gs.publisher_id
                FROM $schema.$table g
                JOIN $schema.gcd_issue gi on gi.id = g.issue_id
                JOIN $schema.gcd_series gs on gs.id = gi.series_id
                WHERE g.id > $lastIdCompleted
                ORDER BY g.id """.trimIndent()

        logger.debug { "SelectStoriesQuery: $selectStoriesQuery" }

        @Suppress("kotlin:S6307")
        extractAndInsertItems(
            selectItemsQuery = selectStoriesQuery,
            extractor = extractor,
            batchSize = batchSize
        )
    }

    /**
     * Extract credits - extracts non-relational creator credits from stories
     * in [schema].stories_to_migrate
     *
     * @param schema The schema with the stories_to_migrate table.
     * @param initial Whether this is the initial run.
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun extractCredits(
        schema: String,
        initial: Boolean,
        startingId: Int? = null,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ) {
        logger.info { "starting credits..." }

        val extractor = CreditExtractor(schema)
        val lastUpdatedItemId = startingId
            ?: ExtractionProgressTracker.getLastProcessedItemId(extractor.extractedItem)
            ?: Credentials.CREDITS_STORY_START_ID

        val table = if (initial) "gcd_story" else "stories_to_migrate"

        /**
         * Script sql - an sql snippet to get the writer, penciller, inker,
         * colorist, letterer, and editor from the database.
         */
        val selectStoriesQuery =
            """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                        FROM $schema.$table g
                        WHERE g.id > $lastUpdatedItemId
                        ORDER BY g.id """.trimIndent()

        @Suppress("kotlin:S6307")
        extractAndInsertItems(
            selectItemsQuery = selectStoriesQuery,
            extractor = extractor,
            batchSize = batchSize
        )
    }

    /**
     * Updates items in the database using the given SQL query to retrieve the
     * items.
     *
     * @param selectItemsQuery the SQL query to retrieve the items
     * @param extractor the extractor to use to extract the items from the
     *     result set
     * @param batchSize the batch size to use
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    internal fun extractAndInsertItems(
        selectItemsQuery: String,
        extractor: Extractor,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ) {
        ConnectionProvider.getConnection(targetSchema).connection.use { conn ->
            logger.debug { "Extracting and inserting items..." }
            val progressTracker = ExtractionProgressTracker(
                extractedType = extractor.extractedItem,
                targetSchema = targetSchema,
                totalItems = queryExecutor.getItemCount(
                    tableName = extractor.extractTable,
                    conn = conn
                ),
            )
            logger.debug { "Progress tracker: $progressTracker" }

            val initialProgress = progressTracker.progressInfo
            var totalTimeMillis: Long = initialProgress.totalTimeMillis

            var offset = 0
            var done = false

            while (!done) {
                val queryWithLimitAndOffset =
                    "$selectItemsQuery LIMIT $batchSize OFFSET ${offset * batchSize}"

                logger.debug { "Query: $queryWithLimitAndOffset" }

                queryExecutor.executeQueryAndDo(
                    query = queryWithLimitAndOffset,
                    conn = conn
                ) { resultSet ->
                    if (!resultSet.next()) {
                        logger.info { "No more items to update" }
                        done = true
                    } else {
                        do {
                            totalTimeMillis += measureTimeMillis {
                                val processedItemId = extractor.extractAndInsert(resultSet, conn)
                                progressTracker.updateProgressInfo(processedItemId, totalTimeMillis)
                            }
                        } while (resultSet.next())
                    }
                }

                offset++
            }

            logger.info { "END\n\n\n" }
            progressTracker.resetProgressInfo()
        }
    }
}
