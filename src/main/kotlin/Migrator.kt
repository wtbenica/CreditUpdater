import Credentials.Companion.CHARACTER_STORIES_COMPLETE_NEW
import Credentials.Companion.CHARACTER_STORY_START_NEW
import Credentials.Companion.CREDITS_STORIES_COMPLETE_NEW
import Credentials.Companion.CREDITS_STORY_START_NEW
import Credentials.Companion.NEW_DATABASE
import Credentials.Companion.PRIMARY_DATABASE
import DatabaseUtil.Companion.getItemCount
import DatabaseUtil.Companion.runQuery
import DatabaseUtil.Companion.updateItems
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.sql.Connection
import java.sql.SQLException

abstract class Doer(protected val targetSchema: String) {

    protected var conn: Connection? = run {
        println("Getting connection to ${targetSchema}...")
        DatabaseUtil.getConnection(targetSchema)
    }

    protected fun closeConn() {
        if (conn != null) {
            try {
                conn?.close()
            } catch (sqlEx: SQLException) {
                sqlEx.printStackTrace()
            }
            conn = null
        }
    }
    
    companion object {
        suspend fun extractCharactersAndAppearances(
            conn: Connection?,
            storyCount: Int?,
            schema: String,
            lastIdCompleted: Long,
            numComplete: Long
        ) {
            println("Starting Characters...")

            val scriptSql = """SELECT g.id, g.characters
                        FROM $schema.stories_to_migrate g
                        WHERE g.id > $lastIdCompleted
                        ORDER BY g.id """

            conn?.let {
                updateItems(
                    items = runQuery(scriptSql),
                    numCompleteInit = numComplete,
                    itemCount = storyCount,
                    extractedItem = "Character",
                    fromValue = "Story",
                    extractor = CharacterExtractor(schema, it),
                    conn = it
                )
            }

            fun transferNewItems() {
                // Publishers
                val sql = """SELECT *
            FROM $schema.good_publishers
            WHERE TRUE;
        """
                // getPublishers
                // getForeignKeys
                // addForeignKeys
                // addPublishers

                // Series

                // Issues

                // Stories

                // Creators

                // NameDetails

                // StoryCredits

                // MStoryCredits

                // MCharacters

                // MCharacterAppearances
            }
        }

        suspend fun extractCredits(
            sourceConn: Connection?, storyCount: Int?, schema: String, lastIdCompleted: Long, numComplete: Long
        ) {
            println("starting credits...")
            val scriptSql = """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                            FROM ${schema}.stories_to_migrate g
                            WHERE g.id > $lastIdCompleted
                            ORDER BY g.id """

            sourceConn?.let {
                updateItems(
                    items = runQuery(scriptSql),
                    numCompleteInit = numComplete,
                    itemCount = storyCount,
                    extractedItem = "Credit",
                    fromValue = "StoryId",
                    extractor = CreditExtractor(schema, it),
                    conn = it
                )
            }
        }

    }
}

class Migrator : Doer(NEW_DATABASE) {
    suspend fun migrate() {
        coroutineScope {
            try {
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
            } finally {
                closeConn()
            }
        }
    }

    private fun migrateRecords(newDbConn: Connection?) {
        DatabaseUtil.runSqlScriptUpdate(newDbConn, MIGRATE_PATH_NEW)
    }

    companion object {
        private const val ADD_MODIFY_TABLES_PATH_NEW = "./src/main/sql/my_tables_new.sql"
        private const val ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW = "src/main/sql/add_issue_series_to_credits_new.sql"
        private const val MIGRATE_PATH_NEW = "src/main/sql/migrate.sql"

        internal fun addTablesNew(connection: Connection?) =
            DatabaseUtil.runSqlScriptQuery(connection, ADD_MODIFY_TABLES_PATH_NEW)

        internal fun addIssueSeriesToCreditsNew(connection: Connection?) =
            DatabaseUtil.runSqlScriptUpdate(connection, ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW)

    }
}