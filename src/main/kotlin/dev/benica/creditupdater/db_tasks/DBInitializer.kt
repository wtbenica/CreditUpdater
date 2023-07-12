package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.ISSUE_SERIES_PATH
import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.di.*
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Named

/**
 * Prepares an initial database installation from a gcd dump.
 *
 * Steps:
 * 1. Add new tables to the database schema and shrink the database.
 * 2. Extract character and appearance data from the 'gcd_story' table and
 *    update the 'm_character' and 'm_character_appearance' tables.
 * 3. Extract credit data from the 'gcd_story' table and update the
 *    'gcd_story_credit' and 'm_story_credit' tables.
 * 4. Add foreign key constraints to the 'gcd_story_credit',
 *    'm_character_appearance', and 'm_story_credit' tables.
 *
 * @param targetSchema The schema to update.
 * @param startAtStep The step to start at (1-4).
 */
class DBInitializer(
    private val targetSchema: String = PRIMARY_DATABASE,
    private val startAtStep: Int,
    private val startingId: Int? = null,
    dispatcherComponent: DispatchAndExecuteComponent = DaggerDispatchAndExecuteComponent.create(),
) {
    // Dependencies
    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    internal lateinit var connectionSource: ConnectionSource

    // Private properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    private val dbTask: DBTask = DBTask(targetSchema)

    private val conn: Connection

    init {
        dispatcherComponent.inject(this)
        conn = connectionSource.getConnection(targetSchema).connection
    }


    /**
     * Prepare database - this is the main entry point for the
     * PrimaryDatabaseInitializer class. It is responsible
     * for preparing the database for the first time.
     *
     * @throws SQLException
     * @see DBTask
     */
    @Throws(SQLException::class)
    suspend fun prepareDb() {
        val queryExecutor = QueryExecutor(targetSchema)

        withContext(ioDispatcher) {
            try {
                logger.info { "Updating $targetSchema" }

                if (startAtStep == 1) {
                    logger.info { "Starting Table Updates..." }
                    dropUnusedTables(queryExecutor, conn)

                    dropIsSourcedAndSourcedByColumns()

                    DBInitAddTables(
                        queryExecutor = QueryExecutor(targetSchema),
                        targetSchema = targetSchema,
                        conn = conn
                    ).addTablesAndConstraints()

                    createDeleteViews(queryExecutor, conn)

                    removeUnnecessaryRecords(queryExecutor, conn)
                }

                if (startAtStep <= 2) {
                    logger.info { "Starting Character Updates..." }
                    dbTask.extractCharactersAndAppearances(
                        schema = targetSchema,
                        initial = true,
                        startingId = startingId,
                    )
                }

                if (startAtStep <= 3) {
                    logger.info { "Starting Credit Updates..." }
                    dbTask.extractCredits(
                        schema = targetSchema,
                        initial = true,
                        startingId = startingId.takeIf { startAtStep == 3 },
                    )
                }

                if (startAtStep <= 4) {
                    logger.info { "Starting foreign key updates" }
                    addIssueSeriesColumnsAndConstraints()
                }

                logger.info { "Successfully updated $targetSchema" }
            } catch (sqlEx: SQLException) {
                logger.error { "Failed to update $targetSchema" }
                logger.error { sqlEx.message }
                logger.error { sqlEx.stackTrace }
                throw sqlEx
            }
        }
    }

    /**
     * Drop is sourced and sourced by columns - this function drops the
     * 'is_sourced' and 'sourced_by' columns from the 'gcd_story_credit' table.
     * These columns have been added to the GCD, but are not needed by Infinite
     * Longbox.
     *
     * @throws SQLException
     */
    @Throws(SQLException::class)
    private fun dropIsSourcedAndSourcedByColumns() {
        dbTask.executeSqlStatement(
            """ALTER TABLE $targetSchema.gcd_story_credit
                DROP COLUMN IF EXISTS is_sourced,
                DROP COLUMN IF EXISTS sourced_by;
            """.trimIndent()
        )
    }

    /**
     * Add issue series to credits - this function adds the 'issue' and
     * 'series' columns to the 'gcd_story_credit', 'm_story_credit', and
     * 'm_character_appearance' tables. It also adds the appropriate foreign
     * key constraints to the 'gcd_story_credit', 'm_character_appearance', and
     * 'm_story_credit' tables.
     *
     * @throws SQLException
     */
    @Throws(SQLException::class)
    private fun addIssueSeriesColumnsAndConstraints() =
        dbTask.runSqlScript(sqlScriptPath = ISSUE_SERIES_PATH, runAsTransaction = true)

    companion object {
        const val INIT_REMOVE_ITEMS = "src/main/resources/sql/init_remove_items.sql"
        const val INIT_DROP_UNUSED_TABLES = "src/main/resources/sql/drop_unused_tables.sql"
        const val INIT_CREATE_BAD_VIEWS = "src/main/resources/sql/db_init_create_delete_views.sql"
        internal fun createDeleteViews(queryExecutor: QueryExecutor, conn: Connection) =
            queryExecutor.executeSqlScript(sqlScript = File(INIT_CREATE_BAD_VIEWS), conn = conn)

        internal fun dropUnusedTables(queryExecutor: QueryExecutor, conn: Connection) =
            queryExecutor.executeSqlScript(sqlScript = File(INIT_DROP_UNUSED_TABLES), conn = conn)

        /**
         * Shrink database - this function shrinks the database by removing a bunch
         * of records: Non-US/Canadian publishers, non-English languages, and
         * pre-1900 stories.
         *
         * @throws SQLException
         */
        @Throws(SQLException::class)
        internal fun removeUnnecessaryRecords(queryExecutor: QueryExecutor, conn: Connection) {
            queryExecutor.executeSqlScript(sqlScript = File(INIT_REMOVE_ITEMS), conn = conn)
        }
    }
}

