package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.INCOMING_DATABASE
import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.di.DaggerDispatchersComponent
import dev.benica.creditupdater.di.DispatchersComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Named

/**
 * Migrator - migrates the data from the old database to the new database.
 *
 * @constructor Create empty dev.benica.CreditUpdater.Migrator
 */
class DBMigrator(
    private val sourceSchema: String = INCOMING_DATABASE,
    private val destSchema: String = PRIMARY_DATABASE,
    private val startAtStep: Int = 1,
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

    private val dbTask: DBTask = DBTask(sourceSchema)

    /** Migrate - migrates the data from the old database to the new database. */
    suspend fun migrate() {
        withContext(ioDispatcher) {
            try {
                logger.info { "Migrating to $destSchema from $sourceSchema" }

                if (startAtStep == 1) {
                    logger.info { "starting tables..." }
                    addTablesNew()
                }

                if (startAtStep <= 2) {
                    logger.info { "starting characters..." }
                    dbTask.extractCharactersAndAppearances(
                        schema = sourceSchema,
                        initial = true,
                        startingId = startingId
                    )
                }

                if (startAtStep <= 3) {
                    logger.info { "starting credits..." }
                    dbTask.extractCredits(
                        schema = sourceSchema,
                        initial = true,
                        startingId = startingId.takeIf { startAtStep == 3 }
                    )
                }

                if (startAtStep <= 4) {
                    logger.info { "starting foreign keys updates" }
                    addIssueSeriesToCreditsNew()
                    logger.info { "Done prepping $sourceSchema for migration" }
                }

                if (startAtStep <= 5) {
                    logger.info { "starting migration..." }
                    migrateRecords()
                    logger.info { "Done migrating records" }
                }
            } catch (sqlEx: SQLException) {
                logger.error { "Error migrating to $destSchema from $sourceSchema" }
                logger.error { sqlEx.message }
                logger.error { sqlEx.stackTrace }
                throw sqlEx
            }
        }
    }

    /** Migrate records - migrates the records from the old database to the new */
    private fun migrateRecords() {
        dbTask.runSqlScript(MIGRATE_PATH_NEW)
    }

    /** Add tables new - adds tables to the new database. */
    internal fun addTablesNew() =
        dbTask.runSqlScript(ADD_MODIFY_TABLES_PATH_NEW)

    /** adds the issue_id and series_id columns */
    internal fun addIssueSeriesToCreditsNew() =
        dbTask.runSqlScript(ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW)

    companion object {
        /**
         * This SQL script adds issue_id and series_id columns to the
         * gcd_story_credit and m_character_appearance tables if they don't already
         * exist. It then creates the m_story_credit, m_character_appearance, and
         * m_character tables. The script also creates several views of "good"
         * items based on certain criteria, and creates tables for items that need
         * to be migrated to the new database. Finally, the script adds constraints
         * and indexes to the m_character and m_character_appearance tables.
         */
        private const val ADD_MODIFY_TABLES_PATH_NEW = "src/main/resources/sql/my_tables_new.sql"

        private const val ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW =
            "src/main/resources/sql/add_issue_series_to_credits_new.sql"

        private const val MIGRATE_PATH_NEW = "src/main/resources/sql/migrate.sql"
    }
}