import Credentials.Companion.PASSWORD
import Credentials.Companion.USERNAME
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.sql.*
import java.util.*
import kotlin.system.measureTimeMillis

var i = 0

fun main(args: Array<String>) = runBlocking {
    val updater = Updater()

    updater.update()
}

private const val MILLIS_PER_SECOND = 1000
private const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
private const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR


class Updater {
    private var conn: Connection? = null

    suspend fun update() {
        try {
            getConnection()

            CharacterUpdater(conn).updateCharacters()

            val updateDatabase = coroutineScope {
                async {
                    addTables()
                    shrinkDatabase()
                }
            }
            val update = coroutineScope {
                async {
                    updateDatabase.await().let {
                        println("Starting Credits...")
                        updateCredits()
                    }
                }
            }
            update.await().let {
                addIssueSeriesToCredits()
            }
        } finally {
            closeConn()
        }
    }

    private fun addTables() = runSqlScript(ADD_MODIFY_TABLES_PATH)
    private fun shrinkDatabase() = runSqlScript(SHRINK_DATABASE_PATH)
    private fun addIssueSeriesToCredits() = runSqlScript(ADD_ISSUE_SERIES_TO_CREDITS_PATH)

    private fun updateCredits() {
        var count: Int? = null
        var current = Credentials.CREDITS_STORY_START + 1

        val getCountStmt: Statement?
        var getCountResultSet: ResultSet? = null

        try {
            getCountStmt = conn?.createStatement()
            val scriptSql = """SELECT COUNT(*) AS count
                FROM gcd_story g;"""

            getCountResultSet = getCountStmt?.executeQuery(scriptSql)

            if (getCountStmt?.execute(scriptSql) == true) {
                getCountResultSet = getCountStmt.resultSet
            }

            if (getCountResultSet?.next() == true) {
                count = getCountResultSet.getInt("count")
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            closeResultSet(getCountResultSet)
        }

        val getStoriesStmt: Statement?
        var getStoriesResultSet: ResultSet? = null

        try {
            var totalTimeMillis: Long = 0

            getStoriesStmt = conn?.createStatement()
            val scriptSql = """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                FROM gcd_story g
                WHERE g.id > ${Credentials.CREDITS_STORY_START}
                ORDER BY g.id;"""

            getStoriesResultSet = getStoriesStmt?.executeQuery(scriptSql)

            if (getStoriesStmt?.execute(scriptSql) == true) {
                getStoriesResultSet = getStoriesStmt.resultSet
            }

            while (getStoriesResultSet?.next() == true) {
                println("Starting: $current${count?.let { "/$it" }}")

                totalTimeMillis += measureTimeMillis {
                    val scriptNames = getStoriesResultSet.getString("script").split(';')
                    val pencilsNames = getStoriesResultSet.getString("pencils").split(';')
                    val inksNames = getStoriesResultSet.getString("inks").split(';')
                    val colorsNames = getStoriesResultSet.getString("colors").split(';')
                    val lettersNames = getStoriesResultSet.getString("letters").split(';')
                    val editingNames = getStoriesResultSet.getString("editing").split(';')

                    val storyId = getStoriesResultSet.getInt("id")

                    makeCredits(scriptNames, storyId, 1)
                    makeCredits(pencilsNames, storyId, 2)
                    makeCredits(inksNames, storyId, 3)
                    makeCredits(colorsNames, storyId, 4)
                    makeCredits(lettersNames, storyId, 5)
                    makeCredits(editingNames, storyId, 6)
                }

                val averageTime: Long = totalTimeMillis / (current - Credentials.CREDITS_STORY_START)
                val remainingTime: Long? = count?.let {
                    averageTime * (it - current)
                }
                val pair = Companion.millisToPretty(remainingTime)
                val fair = Companion.millisToPretty(totalTimeMillis)

                println("Complete: ${current++}${count?.let { "/$it" }}")
                println("Avg: ${averageTime}ms")
                println("Elapsed: $fair ETR: $pair")
                println()
            }
            println("END")
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            closeResultSet(getStoriesResultSet)
        }
    }

    private fun makeCredits(script_names: List<String>, storyId: Int, roleId: Int) {
        for (name in script_names) {
            if (name != "") {
                makeCredit(prepareName(name), storyId, roleId)
            }
        }
    }

    private fun getStoryCredit(gcndId: Int, storyId: Int, roleId: Int): Int? {
        var storyCreditId: Int? = null
        val statement: PreparedStatement?
        var resultSet: ResultSet? = null

        try {
            val getStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM gcd_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

            statement = conn?.prepareStatement(getStoryCreditIdSql)
            statement?.setInt(1, gcndId)
            statement?.setInt(2, storyId)
            statement?.setInt(3, roleId)

            resultSet = statement?.executeQuery()

            if (statement?.execute() == true) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                storyCreditId = resultSet.getInt("id")
            }

        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(resultSet)
        }

        return storyCreditId
    }

    private fun getGcnd(extracted_name: String): Int? {
        var gcndId: Int? = null
        val getGcndStmnt: PreparedStatement?
        var gcndResultSet: ResultSet? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            getGcndStmnt = conn?.prepareStatement(getGcndSql)
            getGcndStmnt?.setString(1, extracted_name)

            gcndResultSet = getGcndStmnt?.executeQuery()

            if (getGcndStmnt?.execute() == true) {
                gcndResultSet = getGcndStmnt.resultSet
            }

            if (gcndResultSet?.next() == true) {
                gcndId = gcndResultSet.getInt("id")
            } else {
                // TODO: save extracted_name storyId, Role to file
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(gcndResultSet)
        }

        return gcndId
    }

    private fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
        getGcnd(extracted_name)?.let { gcndId ->
            getStoryCredit(gcndId, storyId, roleId) ?: makeStoryCredit(gcndId, roleId, storyId)
        }
    }

    private fun makeStoryCredit(gcndId: Int, roleId: Int, storyId: Int) {
        val insertStoryCreditStmt: PreparedStatement?

        val insertStoryCreditSql = """
                            INSERT INTO m_story_credit(created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, signature_id)
                             VALUE (CURTIME(), CURTIME(), 0, 0, 0, 0, '', '', '', ?, ?, ?, NULL)
                        """

        insertStoryCreditStmt = conn?.prepareStatement(insertStoryCreditSql)
        insertStoryCreditStmt?.setInt(1, gcndId)
        insertStoryCreditStmt?.setInt(2, roleId)
        insertStoryCreditStmt?.setInt(3, storyId)

        insertStoryCreditStmt?.executeUpdate()
    }

    private fun getConnection() {
        val connectionProps = Properties()
        connectionProps["user"] = USERNAME
        connectionProps["password"] = PASSWORD

        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/gcdb2",
                connectionProps
            )
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
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

    private fun runSqlScript(sqlScriptPath: String) {
        val stmt = conn?.createStatement()
        try {
            val fr = FileReader(File(sqlScriptPath))
            val br = BufferedReader(fr)
            val sb = StringBuffer()
            var s: String?
            while (br.readLine().also { s = it } != null) {
                sb.append(s)
            }
            br.close()
            val instructions = sb.toString().split(';')
            instructions.forEach {
                if (it != "") {
                    println("This is $it")
                    println()
                    stmt?.executeUpdate(it)
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val ADD_MODIFY_TABLES_PATH = "./src/main/kotlin/my_tables.sql"
        private const val SHRINK_DATABASE_PATH = "./src/main/kotlin/remove_records.sql"
        private const val ADD_ISSUE_SERIES_TO_CREDITS_PATH = "src/main/kotlin/add_issue_series_to_credits.sql"

        fun closeResultSet(resultSet: ResultSet?) {
            if (resultSet != null) {
                try {
                    resultSet.close()
                } catch (sqlEx: SQLException) {
                    sqlEx.printStackTrace()
                }
            }
        }

        fun prepareName(name: String): String {
            var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
            res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
            res = res.replace(Regex("\\s*\\?\\s*"), "")
            res = res.replace(Regex("^\\s*"), "")
            return res
        }

        private fun millisToPretty(remainingTime: Long?): String = remainingTime?.let {
            if (it < MILLIS_PER_MINUTE) {
                "0s"
            } else {
                var remainingTime1 = it
                val days: Long = remainingTime1 / MILLIS_PER_DAY
                remainingTime1 -= days * MILLIS_PER_DAY
                val hours: Long = remainingTime1 / MILLIS_PER_HOUR
                remainingTime1 -= hours * MILLIS_PER_HOUR
                val minutes: Long = remainingTime1 / MILLIS_PER_MINUTE
                "${if (days > 0) "${days}d " else ""}${if (hours > 0) "${hours}h " else ""}${if (minutes > 0) "${minutes}m " else ""}"
            }
        } ?: "0s"
    }
}

