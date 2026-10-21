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
        /** Stories read, extracted and inserted (in one transaction) at a time. */
        private const val DEFAULT_BATCH_SIZE = 20000

        /** The condition in the extraction queries that says where to start. */
        private val START_AFTER = Regex("""(g\.id\s*>\s*)(\d+)""")
    }

    internal val queryExecutor: QueryExecutor = QueryExecutor()

    /** The query with its "g.id > N" condition made to start after story [lastId]. */
    private fun String.startingAfter(lastId: Int): String =
        START_AFTER.replace(this) { "${it.groupValues[1]}$lastId" }

    // Private Properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    // Public Methods
    /**
     * Copies the characters of the prepared database [baseSchema] into this schema's empty
     * `m_character`, keeping their ids, so that extracting characters finds them by name and the
     * new ones are numbered after them. Does nothing if there are characters here already.
     */
    @Throws(SQLException::class)
    fun copyCharactersFrom(baseSchema: String) {
        ConnectionProvider.getConnection(targetSchema).connection.use { conn ->
            val hasCharacters = conn.createStatement().use { statement ->
                statement.executeQuery("SELECT EXISTS (SELECT 1 FROM $targetSchema.m_character)").use {
                    it.next() && it.getInt(1) == 1
                }
            }
            if (hasCharacters) {
                logger.info { "m_character in $targetSchema already has characters; not copying from $baseSchema" }
                return
            }
            conn.createStatement().use {
                val copied = it.executeUpdate(
                    // not those of a publisher the new dump no longer has
                    """INSERT INTO $targetSchema.m_character(id, name, alter_ego, publisher_id)
                       SELECT id, name, alter_ego, publisher_id FROM $baseSchema.m_character
                       WHERE publisher_id IN (SELECT id FROM $targetSchema.gcd_publisher)
                       ORDER BY id"""
                )
                logger.info { "Copied $copied characters from $baseSchema, keeping their ids" }
            }
        }
    }

    /**
     * Extract characters and appearances - extracts characters and appearances
     * from the database.
     *
     * @param schema The schema with the migrate_stories table.
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

        // a migration keeps the ids characters already have in the primary database
        val extractor = CharacterExtractor(schema, reuseExistingIds = !initial)
        val lastIdCompleted = startingId
            ?: ExtractionProgressTracker.getLastProcessedItemId(extractor.extractedItem)
            ?: Credentials.CHARACTER_STORY_START_ID

        val table = if (initial) "gcd_story" else "migrate_stories"

        logger.debug { "Schema: $schema | Table: $table" }

        val selectStoriesQuery =
            """SELECT g.id, g.characters, gs.publisher_id
                FROM $schema.$table g
                JOIN $schema.gcd_issue gi on gi.id = g.issue_id
                JOIN $schema.gcd_series gs on gs.id = gi.series_id
                WHERE g.id > $lastIdCompleted
                AND g.characters <> ''
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
     * in [schema].migrate_stories
     *
     * @param schema The schema with the migrate_stories table.
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

        val table = if (initial) "gcd_story" else "migrate_stories"

        /**
         * Script sql - an sql snippet to get the writer, penciller, inker,
         * colorist, letterer, and editor from the database.
         */
        val selectStoriesQuery =
            """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                        FROM $schema.$table g
                        WHERE g.id > $lastUpdatedItemId
                        AND (g.script <> '' OR g.pencils <> '' OR g.inks <> ''
                            OR g.colors <> '' OR g.letters <> '' OR g.editing <> '')
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

            // Stories are read a chunk at a time, each starting after the last one done (not
            // skipping a growing number of rows), and a chunk is extracted and inserted in one
            // transaction. Progress is kept once per chunk.
            var lastId = START_AFTER.find(selectItemsQuery)?.groupValues?.get(2)?.toInt() ?: 0
            var done = false

            while (!done) {
                val chunkQuery = "${selectItemsQuery.startingAfter(lastId)} LIMIT $batchSize"

                logger.debug { "Query: $chunkQuery" }

                queryExecutor.executeQueryAndDo(
                    query = chunkQuery,
                    conn = conn
                ) { resultSet ->
                    if (!resultSet.next()) {
                        logger.info { "No more items to update" }
                        done = true
                    } else {
                        val autoCommit = conn.autoCommit
                        conn.autoCommit = false
                        try {
                            var stories = 0
                            totalTimeMillis += measureTimeMillis {
                                val (last, count) = extractor.extractAndInsertBatch(resultSet, conn)
                                lastId = last
                                stories = count
                            }
                            conn.commit()
                            progressTracker.updateProgressInfo(lastId, totalTimeMillis, stories)
                        } catch (e: Exception) {
                            conn.rollback()
                            throw e
                        } finally {
                            conn.autoCommit = autoCommit
                        }
                    }
                }
            }

            logger.info { "END\n\n\n" }
            progressTracker.resetProgressInfo()
        }
    }
}
