package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.INCOMING_DATABASE
import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.di.DaggerDispatchersComponent
import dev.benica.creditupdater.di.DispatchersComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Named

/**
 * dev.benica.CreditUpdater.Migrator - migrates the data from the old
 * database to the new database.
 *
 * @constructor Create empty dev.benica.CreditUpdater.Migrator
 */
class DBMigrator(
    private val targetSchema: String = INCOMING_DATABASE,
    dispatcherComponent: DispatchersComponent = DaggerDispatchersComponent.create(),
) {
    init {
        dispatcherComponent.inject(this)
    }
    private val dbTask: DBTask = DBTask(targetSchema)

    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher

    /** Migrate - migrates the data from the old database to the new database. */
    suspend fun migrate() {
        withContext(ioDispatcher) {
            try {
                println("Migrating to $PRIMARY_DATABASE from $targetSchema")

                println("starting tables...")
                addTablesNew()

                dbTask.extractCharactersAndAppearances(
                    schema = targetSchema,
                    initial = false
                )

                println("Done extracting characters.")

                dbTask.extractCredits(
                    targetSchema,
                    false
                )

                addIssueSeriesToCreditsNew()
                println("Done updating credits")

                println("Starting migration")
                migrateRecords()
            } catch (sqlEx: SQLException) {
                println("Error migrating records: ${sqlEx.message}")
                sqlEx.printStackTrace()
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

    /**
     * Add issue series to credits new - adds the issue_id and series_id
     * columns
     */
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