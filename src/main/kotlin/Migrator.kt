import Credentials.Companion.CHARACTER_STORIES_COMPLETE_NEW
import Credentials.Companion.CHARACTER_STORY_START_NEW
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

        val sourceConn: Connection? = DatabaseUtil.getConnection(NEW_DATABASE)
        val destConn: Connection? = DatabaseUtil.getConnection(PRIMARY_DATABASE)

        coroutineScope {
            val updateTables = async {
                println("starting tables...")
//                Updater.addTablesNew(sourceConn)
//                Updater.addIssueSeriesToCreditsNew(sourceConn)
            }

            val storyCount = getItemCount(
                conn = sourceConn,
                tableName = "$NEW_DATABASE.good_story",
                condition = "WHERE g.modified > '$LAST_UPDATED'"
            )

            val updateCharacters = async {
                updateTables.await().let {
                    println("starting characters...")
                    val scriptSql = """SELECT g.id, g.characters
                        FROM $NEW_DATABASE.good_story g
                        WHERE g.id > $CHARACTER_STORY_START_NEW
                        AND g.modified > '$LAST_UPDATED'
                        ORDER BY g.id 
                        LIMIT 20"""

                    sourceConn?.let { it1 ->
                        updateItems(
                            items = runQuery(scriptSql),
                            numCompleteInit = CHARACTER_STORIES_COMPLETE_NEW,
                            itemCount = storyCount,
                            extractedItem = "Character",
                            fromValue = "Story",
                            extractor = CharacterExtractor(NEW_DATABASE, it1),
                            conn = it1
                        )
                    }
                }
            }

            updateCharacters.await().let {
                println("Done.")
            }

//            val updateCredits = async {
//                updateCharacters.await().let {
//                    println("starting creators...")
//                    val scriptSql = """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
//                        FROM gcd_story g
//                        WHERE g.id > $CREDITS_STORY_START_NEW
//                        AND g.issue_id IN (
//                            SELECT gi.id
//                            FROM $NEW_DATABASE.good_issue gi
//                            WHERE gi.modified > '$LAST_UPDATED'
//                        )
//                        ORDER BY g.id """
//                    
//                    updateItems(
//                        getItemIds = runQuery(scriptSql),
//                        database = NEW_DATABASE,
//                        numCompleteInit = CREDITS_STORIES_COMPLETE_NEW,
//                        itemCount = storyCount,
//                        extractedItem = "Credit",
//                        fromValue = "StoryId",
//                        extractItemFrom = Updater::extractCreditsFromStory,
//                        conn = sourceConn
//                    )
//                }
//            }
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