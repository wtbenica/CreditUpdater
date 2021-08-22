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

class DatabaseUtil {
    companion object {
        var numLines = 0

        internal fun getConnection(database: String): Connection? {
            val connectionProps = Properties()
            connectionProps["user"] = USERNAME
            connectionProps["password"] = PASSWORD

            return try {
                DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/$database",
                    connectionProps
                )
            } catch (ex: SQLException) {
                ex.printStackTrace()
                null
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        fun closeResultSet(resultSet: ResultSet?) {
            if (resultSet != null) {
                try {
                    resultSet.close()
                } catch (sqlEx: SQLException) {
                    sqlEx.printStackTrace()
                }
            }
        }

        internal fun runSqlScriptQuery(conn: Connection?, sqlScriptPath: String) =
            runSqlScript(conn, sqlScriptPath) { stmt, instr -> stmt.execute(instr) }

        internal fun runSqlScriptUpdate(conn: Connection?, sqlScriptPath: String) =
            runSqlScript(conn, sqlScriptPath) { stmt, instr -> stmt.executeUpdate(instr) }

        private fun runSqlScript(conn: Connection?, sqlScriptPath: String, executor: (Statement, String) -> Unit) {
            val stmt = conn?.createStatement()
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
                        println("This is $instr")
                        println()
                        stmt?.let { executor(it, instr) }
                    }
                }
            } catch (sqlEx: SQLException) {
            }
        }

        internal fun getItemCount(conn: Connection?, tableName: String, condition: String? = null): Int? {
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

        internal suspend fun updateItems(
            items: (Statement?) -> ResultSet?,
            numCompleteInit: Long,
            itemCount: Int?,
            extractedItem: String,
            fromValue: String,
            extractor: Extractor,
            conn: Connection,
            destDatabase: String? = null,
        ) {
            clearTerminal()

            var totalComplete: Long = numCompleteInit

            val statement: Statement?
            var extractedValues: ResultSet? = null

            try {
                var totalTimeMillis: Long = 0

                statement = conn.createStatement()
                extractedValues = items(statement)

                while (extractedValues?.next() == true) {
                    coroutineScope {
                        totalTimeMillis += measureTimeMillis {
                            val itemId: Int = extractor.extract(extractedValues, destDatabase)

                            val numComplete = ++totalComplete - numCompleteInit
                            val pctComplete: String =
                                itemCount?.let { (totalComplete.toFloat() / it).toPercent() } ?: "???"

                            val averageTime: Long = totalTimeMillis / numComplete
                            val remainingTime: Long? = itemCount?.let {
                                averageTime * (it - numComplete)
                            }
                            val pair = millisToPretty(remainingTime)
                            val fair = millisToPretty(totalTimeMillis)

                            upNLines(numLines)
                            numLines = 0

                            println("Extract $extractedItem $fromValue: $itemId")
                            println("Complete: $totalComplete${itemCount?.let { "/$it" } ?: ""} $pctComplete")
                            println("Avg: ${averageTime}ms")
                            println("Elapsed: $fair ETR: $pair")
                            numLines += 4
                        }
                    }
                }
                println("END\n\n\n")
            } catch (ex: SQLException) {
                ex.printStackTrace()
            } finally {
                extractor.finish()
                closeResultSet(extractedValues)
            }
        }

        internal fun runQuery(query: String): (Statement?) -> ResultSet? = { statement: Statement? ->
            var resultSet: ResultSet?

            resultSet = statement?.executeQuery(query)

            if (statement?.execute(query) == true) {
                resultSet = statement.resultSet
            }

            resultSet
        }
    }
}