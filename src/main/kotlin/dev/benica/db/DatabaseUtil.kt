package dev.benica.db

import dev.benica.converter.Extractor
import dev.benica.TerminalUtil.Companion.clearTerminal
import dev.benica.TerminalUtil.Companion.millisToPretty
import dev.benica.TerminalUtil.Companion.upNLines
import dev.benica.di.DatabaseComponent
import kotlinx.coroutines.coroutineScope
import mu.KLogger
import mu.KotlinLogging
import toPercent
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.sql.*
import javax.inject.Inject
import kotlin.system.measureTimeMillis

private val logger: KLogger
    get() = KotlinLogging.logger { }


abstract class ConnectionSource {
    abstract fun getConnection(database: String): Connection
}

/**
 * Database util - utility functions for the database.
 *
 * @constructor Create empty Database util
 */
class DatabaseUtil(
    private val database: String,
    databaseComponent: DatabaseComponent,
) {
    @Inject
    internal lateinit var connectionSource: ConnectionSource

    init {
        databaseComponent.inject(this)
    }

    @Volatile
    var numLines = 0

    /**
     * Get connection - gets a connection to the database.
     *
     * @return the connection
     */
    internal fun getConnection(): Connection? =
        try {
            connectionSource.getConnection(database)
        } catch (sqlEx: SQLException) {
            logger.error("Error getting connection", sqlEx)
            null
        } catch (ex: Exception) {
            logger.error("Error getting connection", ex)
            null
        }


    /**
     * Run sql script - runs a sql script with CREATE TABLE statements.
     *
     * @param sqlScriptPath the sql script path
     * @param verbose whether to print the sql script
     */
    internal fun executeSqlScript(sqlScriptPath: String, verbose: Boolean = true) =
        runSqlScript(sqlScriptPath, verbose) { stmt, instr -> stmt.execute(instr) }

    /**
     * Run sql script query - runs a sql script with SELECT statements.
     *
     * @param sqlScriptPath the sql script path
     */
    internal fun runSqlScriptQuery(sqlScriptPath: String, verbose: Boolean = true) =
        runSqlScript(sqlScriptPath, verbose) { stmt, instr -> stmt.executeQuery(instr) }


    /**
     * Run sql script update - runs a sql script INSERT, UPDATE, DELETE and CREATE TABLE statements.
     *
     * @param sqlScriptPath the sql script path
     */
    internal fun runSqlScriptUpdate(sqlScriptPath: String, verbose: Boolean = true) =
        runSqlScript(sqlScriptPath, verbose) { stmt, instr -> stmt.executeUpdate(instr) }

    /**
     * Run sql script - runs a sql script using [executor].
     *
     * @param sqlScriptPath the sql script path
     * @param executor the executor
     * @receiver the receiver
     */
    private fun runSqlScript(
        sqlScriptPath: String,
        verbose: Boolean = true,
        executor: (Statement, String) -> Unit
    ) {
        val stmt = connectionSource.getConnection(database).createStatement()
        try {
            val instructions = parseSqlScript(File(sqlScriptPath))
            instructions.forEach { instr ->
                if (instr != "") {
                    if (verbose) {
                        logger.info { "${instr.replace("\\s{2,}".toRegex(), "\n")}\n" }
                    }
                    if (!instr.startsWith('#')) {
                        try {
                            stmt?.let { executor(it, instr) }
                        } catch (sqlEx: SQLException) {
                            logger.error("Error running SQL script $instr", sqlEx)
                            throw sqlEx
                        }
                    }
                }
            }
        } catch (sqlEx: SQLException) {
            logger.error("Error running SQL script", sqlEx)
        }
    }
    // TODO: This obviously looks better, but it doesn't work because the statements are
    //  not on single lines, but instead semicolon-separated. If the sql files
    //  were formatted differently, this would work.
    //            file.useLines { lines ->
    //                lines.forEach { line ->
    //                    if (line.isNotBlank()) {
    //                        executor(stmt, line)
    //                    }
    //                }
    //            }


    /**
     * A simple function that executes an SQL string
     *
     * @param sql the SQL string to execute
     */
    internal fun executeSql(sql: String) {
        val stmt = connectionSource.getConnection(database).createStatement()
        try {
            stmt.execute(sql)
        } catch (sqlEx: SQLException) {
            logger.error("Error running SQL script $sql", sqlEx)
            throw sqlEx
        }
    }

    /**
     * Parses a SQL script file and returns a list of individual SQL
     * statements.
     *
     * This function reads the contents of the provided SQL script file and
     * splits it into individual SQL statements based on the semicolon (;)
     * delimiter. Any empty or whitespace-only statements are filtered out.
     *
     * @param file the SQL script file to parse
     * @return a list of individual SQL statements extracted from the script
     *     file
     */
    private fun parseSqlScript(file: File): List<String> {
        val instructions = mutableListOf<String>()
        try {
            FileReader(file).use { fr ->
                BufferedReader(fr).use { br ->
                    val sb = StringBuffer()
                    var s: String?
                    while (br.readLine().also { s = it } != null) {
                        val patchedStatement = s!!.replace("<schema>", database)
                        if (patchedStatement.trim().endsWith(';')) {
                            sb.append(patchedStatement.trim().removeSuffix(";"))
                            instructions.add(sb.toString().trim())
                            sb.setLength(0)
                        } else {
                            sb.append(patchedStatement)
                        }
                        sb.append(patchedStatement).append("\n")
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Error parsing SQL script", e)
            throw e
        }

        return instructions.filter { it.isNotBlank() }
    }

    /**
     * Get item count - gets the number of items in a table.
     *
     * @param tableName the table name
     * @param condition the condition
     * @return the item count
     */
    internal fun getItemCount(
        tableName: String,
        condition: String? = null
    ): Int? {
        return try {
            val scriptSql = StringBuilder()
            scriptSql.append(
                """SELECT COUNT(*) AS count
                    FROM $tableName g
                    """
            )

            if (condition != null)
                scriptSql.append(condition)
            val sql = scriptSql.toString()

            connectionSource.getConnection(database).prepareStatement(sql)
                ?.use { getCountStmt: PreparedStatement ->
                    getCountStmt.executeQuery().use { getCountResultSet: ResultSet ->
                        if (getCountResultSet.next()) {
                            getCountResultSet.getInt("count")
                        } else {
                            null
                        }
                    }
                }
        } catch (ex: SQLException) {
            ex.printStackTrace()
            null
        }
    }

    /**
     * Updates items in the database using the given SQL query to retrieve the
     * items.
     *
     * @param getItems the SQL query to retrieve the items
     * @param startingComplete the starting number of completed items
     * @param totalItems the total number of items to update, or null if
     *     unknown
     * @param extractor the extractor to use to extract the items from the
     *     result set
     * @param destDatabase the destination database to use, or null to use the
     *     source database
     */
    internal suspend fun updateItems(
        getItems: String,
        startingComplete: Long,
        totalItems: Int?,
        extractor: Extractor,
        destDatabase: String? = null,
    ) {
        /**
         * Prints progress information for the current item being updated.
         *
         * @param itemId the ID of the current item being updated
         * @param currentComplete the current number of completed items
         * @param currentDurationMillis the current duration of the update process
         *     in milliseconds
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

            logger.info { "Extract ${extractor.extractedItem} ${extractor.fromValue}: $itemId" }
            logger.info { "Complete: $currentComplete${totalItems?.let { "/$it" } ?: ""} $pctComplete" }
            logger.info { "Avg: ${averageTime}ms" }
            logger.info { "Elapsed: $elapsed ETR: $remaining" }
            numLines += 4
        }

        clearTerminal()

        var currentComplete: Long = startingComplete
        var totalTimeMillis: Long = 0

        try {
            getConnection()?.createStatement()?.use { statement ->
                statement.executeQuery(getItems).use { resultSet ->
                    while (resultSet.next()) {
                        coroutineScope {
                            totalTimeMillis += measureTimeMillis {
                                val itemId: Int = extractor.extractAndInsert(resultSet, destDatabase)

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
            } ?: logger.info { "Unable to get connection" }
        } catch (ex: SQLException) {
            logger.error("Error updating items", ex)
        }

        logger.info { "END\n\n\n" }
    }
}