import Credentials.Companion.ADD_ISSUE_SERIES_TO_CREDITS_PATH
import Credentials.Companion.ADD_MODIFY_TABLES_PATH
import Credentials.Companion.CHARACTER_STORIES_COMPLETE
import Credentials.Companion.CHARACTER_STORY_START
import Credentials.Companion.CREDITS_STORIES_COMPLETE
import Credentials.Companion.CREDITS_STORY_START
import Credentials.Companion.PRIMARY_DATABASE
import Credentials.Companion.SHRINK_DATABASE_PATH
import Credentials.Companion.UPDATE_CHARACTERS
import Credentials.Companion.UPDATE_CREDITS
import Credentials.Companion.UPDATE_DATABASE
import DatabaseUtil.Companion.getItemCount
import DatabaseUtil.Companion.runQuery
import DatabaseUtil.Companion.updateItems
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.sql.Connection
import java.sql.SQLException

class Updater {
    private var conn: Connection? = DatabaseUtil.getConnection(PRIMARY_DATABASE)

    suspend fun update() {
        coroutineScope {
            try {
                val updateDatabase = async {
                    if (UPDATE_DATABASE) {
                        println("Starting Database Updates...")
                        addTables(conn)
                        shrinkDatabase(conn)
                    }
                }

                val storyCount = getItemCount(conn, "gcdb2.gcd_story")

                val updateCharacters = async {
                    updateDatabase.await().let {
                        if (UPDATE_CHARACTERS) {
                            println("Starting Characters...")

                            val scriptSql = """SELECT g.id, g.characters
                                FROM $PRIMARY_DATABASE.gcd_story g
                                WHERE g.id > $CHARACTER_STORY_START
                                ORDER BY g.id """

                            conn?.let { it1 ->
                                updateItems(
                                    items = runQuery(scriptSql),
                                    numCompleteInit = CHARACTER_STORIES_COMPLETE,
                                    itemCount = storyCount,
                                    extractedItem = "Character",
                                    fromValue = "Story",
                                    extractor = CharacterExtractor(PRIMARY_DATABASE, it1),
                                    conn = it1
                                )
                            }
                        }
                    }
                }

                val updateCredits = async {
                    updateCharacters.await().let {
                        if (UPDATE_CREDITS) {
                            println("Starting Credits...")

                            val scriptSql = """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                                FROM $PRIMARY_DATABASE.gcd_story g
                                WHERE g.id > $CREDITS_STORY_START
                                ORDER BY g.id"""

                            conn?.let { it1 ->
                                updateItems(
                                    items = runQuery(scriptSql),
                                    numCompleteInit = CREDITS_STORIES_COMPLETE,
                                    itemCount = storyCount,
                                    extractedItem = "Credit",
                                    fromValue = "StoryId",
                                    extractor = CreditExtractor(PRIMARY_DATABASE, it1),
                                    conn = it1
                                )
                            }
                        }
                    }
                }

                updateCredits.await().let {
                    println("Starting FKey updates")
                    addIssueSeriesToCredits(conn)
                }
            } finally {
                closeConn()
            }
        }
    }

    private fun closeConn() {
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
        internal fun addTables(connection: Connection?) =
            DatabaseUtil.runSqlScriptQuery(connection, ADD_MODIFY_TABLES_PATH)

        internal fun shrinkDatabase(connection: Connection?) =
            DatabaseUtil.runSqlScriptUpdate(connection, SHRINK_DATABASE_PATH)

        internal fun addIssueSeriesToCredits(connection: Connection?) =
            DatabaseUtil.runSqlScriptUpdate(connection, ADD_ISSUE_SERIES_TO_CREDITS_PATH)

    }
}

