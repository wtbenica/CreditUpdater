package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.converter.CharacterExtractor
import dev.benica.creditupdater.converter.CreditExtractor
import dev.benica.creditupdater.converter.Extractor
import dev.benica.creditupdater.db.ConnectionSource
import dev.benica.creditupdater.db.DatabaseUtil
import dev.benica.creditupdater.db.ExtractionProgressTracker
import dev.benica.creditupdater.db.SqlQueryExecutor
import dev.benica.creditupdater.di.DaggerDatabaseWorkerComponent
import dev.benica.creditupdater.di.DatabaseWorkerComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import javax.inject.Inject
import javax.inject.Named
import kotlin.system.measureTimeMillis

/**
 * @constructor Create empty dev.benica.CreditUpdater.Doer
 * @property targetSchema The schema to use.
 */
class DBTask(
    private val targetSchema: String,
    databaseComponent: DatabaseWorkerComponent = DaggerDatabaseWorkerComponent.create(),
) {
    init {
        databaseComponent.inject(this)
    }

    @Inject
    internal lateinit var connectionSource: ConnectionSource

    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher


    // Constants
    companion object {
        private const val DEFAULT_BATCH_SIZE = 75000
    }

    // Properties
    protected val database = DatabaseUtil(targetSchema)

    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    /** The connection to [targetSchema]. */
    private val databaseConnection: Connection?
        get() = database.getConnection()

    // Public Methods
    /**
     * Extract credits - extracts non-relational creator credits from stories
     * in [sourceSchema].stories_to_migrate
     *
     * @param sourceSchema The schema with the stories_to_migrate table.
     * @param initial Whether this is the initial run.
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    suspend fun extractCredits(
        sourceSchema: String,
        initial: Boolean,
        startingId: Int? = null,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ) {
        logger.info { "starting credits..." }
        withContext(ioDispatcher) {
            databaseConnection?.let {
                val extractor = CreditExtractor(sourceSchema, it)
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
                        FROM $sourceSchema.$table g
                        WHERE g.id > $lastUpdatedItemId
                        ORDER BY g.id """.trimIndent()

                extractAndInsertItems(
                    selectItemsQuery = selectStoriesQuery,
                    extractor = extractor,
                    batchSize = batchSize
                )
            }
        }
    }

    /**
     * Extract characters and appearances - extracts characters and appearances
     * from the database.
     *
     * @param schema The schema with the stories_to_migrate table.
     * @param initial Whether this is the initial run.
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    suspend fun extractCharactersAndAppearances(
        schema: String,
        initial: Boolean,
        startingId: Int? = null,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ) {
        logger.info { "Starting Characters..." }
        withContext(ioDispatcher) {
            databaseConnection?.let {
                val extractor = CharacterExtractor(schema, it)
                val lastIdCompleted = startingId
                    ?: ExtractionProgressTracker.getLastProcessedItemId(extractor.extractedItem)
                    ?: Credentials.CHARACTER_STORY_START_ID

                val table = if (initial) "gcd_story" else "stories_to_migrate"

                logger.info { "Schema: $schema | Table: $table" }
                /**
                 * Script sql - a snippet that extracts characters and creates appearances
                 * for them
                 */
                val selectStoriesQuery =
                    """SELECT g.id, g.characters, gs.publisher_id
                FROM $schema.$table g
                JOIN $schema.gcd_issue gi on gi.id = g.issue_id
                JOIN $schema.gcd_series gs on gs.id = gi.series_id
                where g.id > $lastIdCompleted
                ORDER BY g.id """.trimIndent()

                logger.debug { "SelectStoriesQuery: $selectStoriesQuery" }

                extractAndInsertItems(
                    selectItemsQuery = selectStoriesQuery,
                    extractor = extractor,
                    batchSize = batchSize
                )
            }
        }
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
    private suspend fun extractAndInsertItems(
        selectItemsQuery: String,
        extractor: Extractor,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ) {
        logger.debug { "Extracting and inserting items..."}
        val progressTracker = ExtractionProgressTracker(
            extractedType = extractor.extractedItem,
            database = targetSchema,
            totalItems = withContext(ioDispatcher) { getItemCount(extractor.extractTable) },
        )
        logger.debug { "Progress tracker: $progressTracker" }

        val initialProgress = progressTracker.progressInfo
        var totalTimeMillis: Long = initialProgress.totalTimeMillis

        var offset = 0
        var done = false

        try {
            while (!done) {
                connectionSource.getConnection(targetSchema).use { conn ->
                    conn.createStatement().use { statement ->

                        val queryWithLimitAndOffset =
                            "$selectItemsQuery LIMIT $batchSize OFFSET ${offset * batchSize}"

                        logger.debug { "Query: $queryWithLimitAndOffset" }

                        val resultSet = statement.executeQuery(queryWithLimitAndOffset)

                        if (!resultSet.next()) {
                            logger.info { "No more items to update" }
                            done = true
                        } else {
                            do {
                                totalTimeMillis += measureTimeMillis {
                                    val processedItemId = extractor.extractAndInsert(resultSet)

                                    progressTracker.updateProgressInfo(processedItemId, totalTimeMillis)
                                }
                            } while (resultSet.next())
                        }
                    }
                }

                offset++
            }
        } catch (ex: SQLException) {
            logger.error("Error updating items", ex)
            throw ex
        }

        logger.info { "END\n\n\n" }
        progressTracker.resetProgressInfo()
    }

    /**
     * Get item count - gets the number of items in a table.
     *
     * @param tableName the table name
     * @param condition the condition
     * @return the item count
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    private fun getItemCount(
        tableName: String,
        condition: String? = null,
    ): Int = SqlQueryExecutor(targetSchema).getItemCount(tableName, condition)

    @Throws(SQLException::class)
    fun runSqlScript(
        sqlScriptPath: String,
        runAsTransaction: Boolean = false
    ) = database.runSqlScript(sqlScriptPath, runAsTransaction)

    @Throws(SQLException::class)
    fun executeSqlStatement(
        sqlStmt: String,
        stmt: Statement = connectionSource.getConnection(targetSchema).createStatement()
    ) = database.executeSqlStatement(sqlStmt, stmt)
}
