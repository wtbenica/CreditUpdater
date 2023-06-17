package dev.benica.db_tasks

import dev.benica.Credentials.Companion.CHARACTER_STORIES_COMPLETE_NEW
import dev.benica.Credentials.Companion.CHARACTER_STORY_START_NEW
import dev.benica.Credentials.Companion.CREDITS_STORIES_COMPLETE_NEW
import dev.benica.Credentials.Companion.CREDITS_STORY_START_NEW
import dev.benica.Credentials.Companion.INCOMING_DATABASE
import dev.benica.Credentials.Companion.PRIMARY_DATABASE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLException

/**
 * dev.benica.CreditUpdater.Migrator - migrates the data from the old
 * database to the new database.
 *
 * @constructor Create empty dev.benica.CreditUpdater.Migrator
 */
class DBMigrator(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : DBTask(INCOMING_DATABASE) {
    /** Migrate - migrates the data from the old database to the new database. */
    suspend fun migrate() {
        withContext(ioDispatcher) {
            try {
                println("Migrating to $PRIMARY_DATABASE from $targetSchema")

                println("starting tables...")
                addTablesNew()

                val storyCount = database.getItemCount(
                    tableName = "$targetSchema.stories_to_migrate",
                )

                extractCharactersAndAppearances(
                    storyCount = storyCount,
                    schema = targetSchema,
                    lastIdCompleted = CHARACTER_STORY_START_NEW,
                    numComplete = CHARACTER_STORIES_COMPLETE_NEW,
                    initial = false
                )

                println("Done extracting characters.")

                extractCredits(
                    storyCount,
                    targetSchema,
                    CREDITS_STORY_START_NEW,
                    CREDITS_STORIES_COMPLETE_NEW,
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

    /**
     * Migrate records - migrates the records from the old database to the new
     */
    private fun migrateRecords() {
        database.runSqlScript(MIGRATE_PATH_NEW)
    }

    /**
     * Add tables new - adds tables to the new database.
     */
    internal fun addTablesNew() =
        database.runSqlScript(ADD_MODIFY_TABLES_PATH_NEW)

    /**
     * Add issue series to credits new - adds the issue_id and series_id
     * columns
     */
    internal fun addIssueSeriesToCreditsNew() =
        database.runSqlScript(ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW)

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
        private const val ADD_MODIFY_TABLES_PATH_NEW = "./src/main/sql/my_tables_new.sql"

        private const val ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW =
            "src/main/sql/add_issue_series_to_credits_new.sql"

        private const val MIGRATE_PATH_NEW = "src/main/sql/migrate.sql"
    }
}