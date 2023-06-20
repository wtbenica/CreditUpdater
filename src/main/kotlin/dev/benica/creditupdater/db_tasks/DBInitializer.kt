package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.ISSUE_SERIES_PATH
import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.di.DaggerDispatchersComponent
import dev.benica.creditupdater.di.DispatchersComponent
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
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
    dispatcherComponent: DispatchersComponent = DaggerDispatchersComponent.create(),
) {
    init {
        dispatcherComponent.inject(this)
    }

    // Dependencies
    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher

    // Private properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    private val dbTask: DBTask = DBTask(targetSchema)

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
        withContext(ioDispatcher) {
            try {
                logger.info { "Updating $targetSchema" }

                if (startAtStep == 1) {
                    logger.info { "Starting Table Updates..." }
                    dropIsSourcedAndSourcedByColumns()
                    addAndModifyTables()
                    removeUnnecessaryRecords()
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
     *
     * @throws SQLException
     */
    @Throws(SQLException::class)
    private fun dropIsSourcedAndSourcedByColumns() {
        dbTask.executeSqlStatement(
            """
                    ALTER TABLE $targetSchema.gcd_story_credit
                    DROP COLUMN IF EXISTS is_sourced,
                    DROP COLUMN IF EXISTS sourced_by;
                """.trimIndent()
        )
    }

    /**
     * Add tables - this function adds the 'm_character',
     * 'm_character_appearance', 'm_story_credit', and 'm_story_credit_type'
     * tables to the database schema. It also adds the 'issue' and 'series'
     * columns to the 'gcd_story_credit' and 'm_character_appearance' tables.
     *
     * @throws SQLException
     */
    @Throws(SQLException::class)
    private fun addAndModifyTables() = dbTask.runSqlScript(sqlScriptPath = INIT_TABLES_PATH)

    /**
     * Shrink database - this function shrinks the database by removing a bunch
     * of records: Non-US/Canadian publishers, non-English languages, and
     * pre-1900 stories.
     *
     * @throws SQLException
     */
    @Throws(SQLException::class)
    private fun removeUnnecessaryRecords() {
        dbTask.runSqlScript(sqlScriptPath = INIT_CREATE_VIEWS_FOR_DELETION)
        dbTask.runSqlScript(sqlScriptPath = INIT_REMOVE_ITEMS)
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
        /**
         * This SQL query adds issue and series columns to the gcd_story_credit
         * table. It creates the m_character and m_character_appearance tables if
         * they don't already exist.
         */
        const val INIT_TABLES_PATH = "src/main/resources/sql/init_add_tables.sql"

        /**
         * This script creates several views that filter out records from the
         * database based on certain criteria. These views are then used to delete
         * records from various tables in the database.
         */
        const val INIT_CREATE_VIEWS_FOR_DELETION = "src/main/resources/sql/init_create_views_for_deletion.sql"

        /**
         * This script creates several views that filter out records from the
         * database based on certain criteria. These views are then used to delete
         * records from various tables in the database. The script also updates
         * some records in the gcd_issue and gcd_series tables by setting their
         * variant_of_id, first_issue_id, and last_issue_id fields to NULL.
         * Finally, the script deletes records from the gcd_indicia_publisher,
         * gcd_brand_group, gcd_brand_emblem_group, gcd_brand_use, and
         * gcd_publisher tables based on certain criteria. Overall, this script is
         * used to limit the records in the database based on certain criteria.
         */
        const val INIT_REMOVE_ITEMS = "src/main/resources/sql/init_remove_items.sql"

    }
}

