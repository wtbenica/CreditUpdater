import Credentials.Companion.PASSWORD
import Credentials.Companion.USERNAME
import TerminalUtil.Companion.clearTerminal
import TerminalUtil.Companion.millisToPretty
import TerminalUtil.Companion.upNLines
import kotlinx.coroutines.coroutineScope
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.sql.*
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Database util - utility functions for the database.
 *
 * @constructor Create empty Database util
 */
class DatabaseUtil {
    companion object {
        @Volatile
        var numLines = 0

        /**
         * Get connection - gets a connection to the database.
         *
         * @param database the database
         * @return the connection
         */
        internal fun getConnection(database: String): Connection? {
            val connectionProps = Properties()
            connectionProps["user"] = USERNAME
            connectionProps["password"] = PASSWORD

            return try {
                DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/$database",
                    connectionProps
                )
            } catch (sqlEx: SQLException) {
                logger.error("Error getting connection", sqlEx)
                null
            } catch (ex: Exception) {
                logger.error("Error getting connection", ex)
                null
            }
        }

        /**
         * Close result set - closes a result set.
         *
         * @param resultSet the result set
         */
        // TODO: Destroy this! [ResultSet] is [AutoCloseable]!
        fun closeResultSet(resultSet: ResultSet?) {
            if (resultSet != null) {
                try {
                    resultSet.close()
                } catch (sqlEx: SQLException) {
                    sqlEx.printStackTrace()
                }
            }
        }

        /**
         * Run sql script query - runs a sql script using [Statement.execute]
         *
         * @param conn the connection
         * @param sqlScriptPath the sql script path
         */
        internal fun runSqlScriptQuery(conn: Connection, sqlScriptPath: String) =
            runSqlScript(conn, sqlScriptPath) { stmt, instr -> stmt.execute(instr) }

        /**
         * Run sql script update - runs a sql script using
         * [Statement.executeUpdate]
         *
         * @param conn the connection
         * @param sqlScriptPath the sql script path
         */
        internal fun runSqlScriptUpdate(conn: Connection, sqlScriptPath: String) =
            runSqlScript(conn, sqlScriptPath) { stmt, instr -> stmt.executeUpdate(instr) }

        /**
         * Run sql script - runs a sql script using [executor]
         *
         * @param conn the connection
         * @param sqlScriptPath the sql script path
         * @param executor the executor
         * @receiver the receiver
         */
        private fun runSqlScript(
            conn: Connection,
            sqlScriptPath: String,
            executor: (Statement, String) -> Unit
        ) {
            val stmt = conn.createStatement()
            try {
                val fr = FileReader(File(sqlScriptPath))
                val br = BufferedReader(fr)
                val sb = StringBuffer()
                var s: String?
                while (br.readLine().also { s = it } != null) {
                    sb.append("$s ")
                }
                br.close()
                val instructions = sb.toString().split(';')
                instructions.forEach { instr ->
                    if (instr != "") {
                        println(instr)
                        println()
                        stmt?.let { executor(it, instr) }
                    }
                }
            } catch (sqlEx: SQLException) {
                logger.error("Error running SQL script", sqlEx)
            }
        }

        /**
         * Get item count - gets the number of items in a table.
         *
         * @param conn the connection
         * @param tableName the table name
         * @param condition the condition
         * @return the item count
         */
        internal fun getItemCount(
            conn: Connection?,
            tableName: String,
            condition: String? = null
        ): Int? {
            val getCountStmt: Statement?
            var getCountResultSet: ResultSet? = null

            return try {
                getCountStmt = conn?.createStatement()
                val scriptSql = StringBuilder()
                scriptSql.append(
                    """SELECT COUNT(*) AS count
                    FROM $tableName g
                    """
                )

                if (condition != null)
                    scriptSql.append(condition)
                val sql = scriptSql.toString()

                getCountResultSet = getCountStmt?.executeQuery(sql)

                if (getCountStmt?.execute(sql) == true) {
                    getCountResultSet = getCountStmt.resultSet
                }

                if (getCountResultSet?.next() == true) {
                    getCountResultSet.getInt("count")
                } else {
                    null
                }
            } catch (ex: SQLException) {
                ex.printStackTrace()
                null
            } finally {
                closeResultSet(getCountResultSet)
            }
        }

        /**
         * Updates items in the database using the given SQL query to retrieve the items.
         *
         * @param getItems the SQL query to retrieve the items
         * @param startingComplete the starting number of completed items
         * @param totalItems the total number of items to update, or null if unknown
         * @param extractor the extractor to use to extract the items from the result set
         * @param conn the database connection to use
         * @param destDatabase the destination database to use, or null to use the source database
         */
        internal suspend fun updateItems(
            getItems: String,
            startingComplete: Long,
            totalItems: Int?,
            extractor: Extractor,
            conn: Connection,
            destDatabase: String? = null,
        ) {
            /**
             * Prints progress information for the current item being updated.
             *
             * @param itemId the ID of the current item being updated
             * @param currentComplete the current number of completed items
             * @param currentDurationMillis the current duration of the update process in milliseconds
             */
            fun printProgressInfo(
                itemId: Int,
                currentComplete: Long,
                currentDurationMillis: Long
            ) {
                val numComplete = currentComplete - startingComplete
                val pctComplete: String =
                    totalItems?.let { (currentComplete.toFloat() / it).toPercent() }
                        ?: "???"

                val averageTime: Long = currentDurationMillis / numComplete
                val remainingTime: Long? = totalItems?.let { averageTime * (it - numComplete) }
                val remaining = millisToPretty(remainingTime)
                val elapsed = millisToPretty(currentDurationMillis)

                upNLines(numLines)
                numLines = 0

                println("Extract ${extractor.extractedItem} ${extractor.fromValue}: $itemId")
                println("Complete: $currentComplete${totalItems?.let { "/$it" } ?: ""} $pctComplete")
                println("Avg: ${averageTime}ms")
                println("Elapsed: $elapsed ETR: $remaining")
                numLines += 4
            }

            clearTerminal()

            var currentComplete: Long = startingComplete
            var totalTimeMillis: Long = 0

            try {
                conn.createStatement().use { statement ->
                    statement.executeQuery(getItems).use { resultSet ->
                        while (resultSet.next()) {
                            coroutineScope {
                                totalTimeMillis += measureTimeMillis {
                                    val itemId: Int = extractor.extract(resultSet, destDatabase)

                                    currentComplete++
                                    printProgressInfo(
                                        itemId = itemId,
                                        currentComplete = currentComplete,
                                        currentDurationMillis = totalTimeMillis
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (ex: SQLException) {
                logger.error("Error updating items", ex)
            }

            println("END\n\n\n")
        }
    }
}