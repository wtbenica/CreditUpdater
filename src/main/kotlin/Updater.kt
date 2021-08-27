import Credentials.Companion.ADD_ISSUE_SERIES_TO_CREDITS_PATH
import Credentials.Companion.ADD_MODIFY_TABLES_PATH
import Credentials.Companion.CHARACTER_STORIES_NUM_COMPLETE
import Credentials.Companion.CHARACTER_STORY_ID_START
import Credentials.Companion.CREDITS_STORIES_NUM_COMPLETE
import Credentials.Companion.CREDITS_STORY_ID_START
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

class Updater : Doer(PRIMARY_DATABASE) {
    suspend fun update() {
        coroutineScope {
            try {
                println("Updating $targetSchema")

                val updateDatabase = async {
                    if (UPDATE_DATABASE) {
                        println("Starting Database Updates...")
                        addTables(conn)
                        shrinkDatabase(conn)
                    }
                }

                val storyCount = getItemCount(
                    conn = conn,
                    tableName = "$targetSchema.gcd_story"
                )

                val updateCharacters = async {
                    updateDatabase.await().let {
                        if (UPDATE_CHARACTERS) {
                            extractCharactersAndAppearances(
                                conn = conn,
                                storyCount = storyCount,
                                schema = targetSchema,
                                lastIdCompleted = CHARACTER_STORY_ID_START,
                                numComplete = CHARACTER_STORIES_NUM_COMPLETE
                            )
                        }
                    }
                }

                val updateCredits = async {
                    updateCharacters.await().let {
                        if (UPDATE_CREDITS) {
                            extractCredits(
                                conn,
                                storyCount,
                                targetSchema,
                                CREDITS_STORY_ID_START,
                                CREDITS_STORIES_NUM_COMPLETE
                            )
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

    companion object {
        internal fun addTables(connection: Connection?) =
            DatabaseUtil.runSqlScriptQuery(connection, ADD_MODIFY_TABLES_PATH)

        internal fun shrinkDatabase(connection: Connection?) =
            DatabaseUtil.runSqlScriptUpdate(connection, SHRINK_DATABASE_PATH)

        internal fun addIssueSeriesToCredits(connection: Connection?) =
            DatabaseUtil.runSqlScriptUpdate(connection, ADD_ISSUE_SERIES_TO_CREDITS_PATH)

    }
}

