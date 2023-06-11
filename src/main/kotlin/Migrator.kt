import Credentials.Companion.CHARACTER_STORIES_COMPLETE_NEW
import Credentials.Companion.CHARACTER_STORY_START_NEW
import Credentials.Companion.CREDITS_STORIES_COMPLETE_NEW
import Credentials.Companion.CREDITS_STORY_START_NEW
import Credentials.Companion.INCOMING_DATABASE
import Credentials.Companion.PRIMARY_DATABASE
import DatabaseUtil.Companion.getItemCount
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.sql.Connection

/**
 * Migrator - migrates the data from the old database to the new database.
 *
 * @constructor Create empty Migrator
 */
class Migrator : Doer(INCOMING_DATABASE) {
    /** Migrate - migrates the data from the old database to the new database. */
    suspend fun migrate() {
        coroutineScope {
            databaseConnection?.use { conn ->
                println("Migrating to $PRIMARY_DATABASE from $targetSchema")

                val updateTables = async {
                    println("starting tables...")
                    addTablesNew(conn)
                }

                val storyCount = getItemCount(
                    conn = conn,
                    tableName = "$targetSchema.stories_to_migrate",
                )

                val updateCharacters = async {
                    updateTables.await().let {
                        extractCharactersAndAppearances(
                            conn,
                            storyCount,
                            targetSchema,
                            CHARACTER_STORY_START_NEW,
                            CHARACTER_STORIES_COMPLETE_NEW
                        )
                    }
                }

                updateCharacters.await().let {
                    println("Done extracting characters.")
                }

                val updateCredits = async {
                    updateCharacters.await().let {
                        extractCredits(
                            conn,
                            storyCount,
                            targetSchema,
                            CREDITS_STORY_START_NEW,
                            CREDITS_STORIES_COMPLETE_NEW
                        )
                    }
                }

                val finishUpdating = async {
                    updateCredits.await().let {
                        addIssueSeriesToCreditsNew(conn)
                        println("Done updating credits")
                    }
                }

                finishUpdating.await().let {
                    println("Starting migration")
                    migrateRecords(conn)
                }
            } ?: logger.info { "No connection to $targetSchema" }
        }
    }

    /**
     * Migrate records - migrates the records from the old database to the new
     *
     * @param newDbConn The connection to the new database.
     */
    private fun migrateRecords(newDbConn: Connection) {
        DatabaseUtil.runSqlScriptUpdate(newDbConn, MIGRATE_PATH_NEW)
    }

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

        /**
         * Add tables new - adds tables to the new database.
         *
         * @param connection The connection to the new database.
         */
        internal fun addTablesNew(connection: Connection) =
            DatabaseUtil.runSqlScriptQuery(connection, ADD_MODIFY_TABLES_PATH_NEW)

        /**
         * Add issue series to credits new - adds the issue_id and series_id columns
         *
         * @param connection The connection to the new database.
         */
        internal fun addIssueSeriesToCreditsNew(connection: Connection) =
            DatabaseUtil.runSqlScriptUpdate(connection, ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW)
    }
}