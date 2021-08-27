import Credentials.Companion.CHARACTER_STORIES_COMPLETE_NEW
import Credentials.Companion.CHARACTER_STORY_START_NEW
import Credentials.Companion.CREDITS_STORIES_COMPLETE_NEW
import Credentials.Companion.CREDITS_STORY_START_NEW
import Credentials.Companion.LAST_UPDATED
import Credentials.Companion.NEW_DATABASE
import Credentials.Companion.PRIMARY_DATABASE
import DatabaseUtil.Companion.getItemCount
import DatabaseUtil.Companion.runQuery
import DatabaseUtil.Companion.updateItems
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.sql.Connection

class Migrator {
    suspend fun migrate() {
        println("Migrating to $PRIMARY_DATABASE since $LAST_UPDATED")
        println("Getting connections")
        val newDbConn: Connection? by lazy { DatabaseUtil.getConnection(NEW_DATABASE) }
        val primaryDbConn: Connection? by lazy { DatabaseUtil.getConnection(PRIMARY_DATABASE) }

        coroutineScope {
            val updateTables = async {
                println("starting tables...")
//                addTablesNew(newDbConn)
            }

            val storyCount = getItemCount(
                conn = newDbConn,
                tableName = "$NEW_DATABASE.good_story",
                condition = "WHERE g.modified > '$LAST_UPDATED'"
            )

            val updateCharacters = async {
                updateTables.await().let {
//                    extractCharactersAndAppearances(newDbConn, storyCount)
                }
            }

            updateCharacters.await().let {
                println("Done extracting characters.")
            }

            val updateCredits = async {
                updateCharacters.await().let {
                    extractCredits(newDbConn, storyCount)
                }
            }

            updateCredits.await().let {
                addIssueSeriesToCreditsNew(newDbConn)
                println("Done updating credits")
            }

            migrateRecords()
        }
    }

    private fun migrateRecords() {
        
    }

    private suspend fun extractCredits(sourceConn: Connection?, storyCount: Int?) {
        println("starting credits...")
        val scriptSql = """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                            FROM gcd_story g
                            WHERE g.id > $CREDITS_STORY_START_NEW
                            AND g.type_id IN (6, 19)
                            AND g.issue_id IN (
                                SELECT gi.id
                                FROM $NEW_DATABASE.good_issue gi
                            )
                            AND g.modified > '$LAST_UPDATED'
                            ORDER BY g.id """

        sourceConn?.let {
            updateItems(
                items = runQuery(scriptSql),
                numCompleteInit = CREDITS_STORIES_COMPLETE_NEW,
                itemCount = storyCount,
                extractedItem = "Credit",
                fromValue = "StoryId",
                extractor = CreditExtractor(NEW_DATABASE, it),
                conn = it
            )
        }
    }

    private suspend fun extractCharactersAndAppearances(
        sourceConn: Connection?,
        storyCount: Int?
    ) {
        println("starting characters...")
        val scriptSql = """SELECT g.id, g.characters
                            FROM $NEW_DATABASE.good_story g
                            WHERE g.id > $CHARACTER_STORY_START_NEW
                            AND g.modified > '$LAST_UPDATED'
                            ORDER BY g.id """

        sourceConn?.let {
            updateItems(
                items = runQuery(scriptSql),
                numCompleteInit = CHARACTER_STORIES_COMPLETE_NEW,
                itemCount = storyCount,
                extractedItem = "Character",
                fromValue = "Story",
                extractor = CharacterExtractor(NEW_DATABASE, it),
                conn = it
            )
        }

        fun transferNewItems() {
            // Publishers
            val sql = """SELECT *
                FROM $NEW_DATABASE.good_publishers
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

    companion object {
        private const val ADD_MODIFY_TABLES_PATH_NEW = "./src/main/sql/my_tables_new.sql"
        private const val ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW = "src/main/sql/add_issue_series_to_credits_new.sql"

        internal fun addTablesNew(connection: Connection?) =
            DatabaseUtil.runSqlScriptQuery(connection, ADD_MODIFY_TABLES_PATH_NEW)

        internal fun addIssueSeriesToCreditsNew(connection: Connection?) =
            DatabaseUtil.runSqlScriptUpdate(connection, ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW)
    }
}