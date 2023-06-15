package dev.benica.db

import dev.benica.TerminalUtil.Companion.millisToPretty
import dev.benica.TerminalUtil.Companion.upNLines
import dev.benica.converter.Extractor
import dev.benica.di.DatabaseComponent
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import toPercent
import java.io.File
import java.sql.*
import javax.inject.Inject
import kotlin.system.measureTimeMillis

private val logger: KLogger
    get() = KotlinLogging.logger { }

// TODO: Split out SqlScriptHandler, SqlQueryExecutor, and ProgressTracker and inject


/**
 * Database util - utility functions for the database.
 *
 * @param database the database to connect to
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
     * A helper function that takes in a string and a list of strings and
     * returns whether the string starts with any of the strings in the list.
     * It is case-insensitive.
     */
    private fun String.startsWithAny(list: List<String>): Boolean =
        list.any { this.uppercase().startsWith(it.uppercase()) }


    /**
     * Run SQL script - runs an SQL script. Statements can be separated
     * by a semicolon. This function will use the correct [Statement]
     * function depending on the type: DDL statements ([Statement.execute]),
     * DML statements ([Statement.executeUpdate]), or DQL statements
     * ([Statement.executeQuery]).
     *
     * If [runAsTransaction] is true, the script will be run as a transaction,
     * unless there is a DDL statement, in which case the transaction will be
     * committed before the DDL statement and restarted after. The transaction
     * will be rolled back if there is an error.
     *
     * @param sqlScriptPath the sql script path
     * @param runAsTransaction whether to run the script as a transaction
     * @receiver the receiver
     */
    @Throws(SQLException::class)
    internal fun runSqlScript(
        sqlScriptPath: String,
        runAsTransaction: Boolean = false
    ) {
        connectionSource.getConnection(database).use { conn ->
            conn.createStatement().use { stmt ->
                try {
                    val statements: List<String> = parseSqlScript(File(sqlScriptPath))
                    if (runAsTransaction) {
                        conn.autoCommit = false
                    }
                    statements.forEach { sqlStmt ->
                        if (sqlStmt != "") {
                            logger.info { "${sqlStmt.replace("\\s{2,}".toRegex(), "\n")}\n" }
                            executeSqlStatement(sqlStmt, stmt)
                        }
                    }
                    if (runAsTransaction) {
                        conn.commit()
                    }
                } catch (sqlEx: SQLException) {
                    if (runAsTransaction) {
                        conn.rollback()
                    }
                    logger.error("Error running SQL script", sqlEx)
                    throw sqlEx
                } finally {
                    if (runAsTransaction) {
                        conn.autoCommit = true
                    }
                }
            }
        }
    }

    /**
     * Uses the correct [Statement] function depending on
     * the type: DDL statements ([Statement.execute]), DML
     * statements ([Statement.executeUpdate]), or DQL statements
     * ([Statement.executeQuery]). If the statement is a DQL statement, the
     * connection is set to auto-commit before the statement is executed and
     * then set back to auto-commit false after the statement is executed.
     *
     * @param sqlStmt the SQL statement to execute
     * @param stmt the [Statement] to use, defaults to a new [Statement] from
     *     the connection
     */
    @Throws(SQLException::class)
    internal fun executeSqlStatement(
        sqlStmt: String,
        stmt: Statement = connectionSource.getConnection(database).createStatement()
    ) {
        try {
            when {
                sqlStmt.startsWithAny(listOf("CREATE", "ALTER", "DROP", "TRUNCATE")) -> {
                    val autoCommit = stmt.connection.autoCommit
                    stmt.connection.autoCommit = true
                    stmt.execute(sqlStmt)
                    stmt.connection.autoCommit = autoCommit
                }

                sqlStmt.startsWithAny(listOf("INSERT", "UPDATE", "DELETE")) -> stmt.executeUpdate(sqlStmt)
                sqlStmt.startsWithAny(listOf("SELECT")) -> stmt.executeQuery(sqlStmt)
                else -> Unit
            }
        } catch (sqlEx: SQLException) {
            logger.error("Error running SQL script $sqlStmt", sqlEx)
            throw sqlEx
        } catch (ex: Exception) {
            logger.error("Error running SQL script $sqlStmt", ex)
            throw ex
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
     * @param file the SQL script file to parse, semicolon-delimited
     * @return a list of individual SQL statements extracted from the script
     *     file
     */
    private fun parseSqlScript(file: File): List<String> =
        file.useLines { lines ->
            lines.filter { it.isNotBlank() }
                .map { it.replace("<schema>", database).trim() }
                .joinToString(separator = " ")
                .split(";")
                .map { it.trim() }

        }

    /**
     * Get item count - gets the number of items in a table.
     *
     * @param tableName the table name
     * @param condition the condition
     * @return the item count
     */
    @Throws(SQLException::class)
    internal fun getItemCount(
        tableName: String,
        condition: String? = null
    ): Int? {
        val sql = "SELECT COUNT(*) AS count FROM $tableName g" + if (condition != null) " WHERE $condition" else ""
        val stmt = connectionSource.getConnection(database).createStatement()
        val rs = stmt.executeQuery(sql)
        rs.next()
        return rs.getInt(1)
    }

    /**
     * Prints progress information for the current item being updated.
     *
     * @param itemId the ID of the current item being updated
     * @param currentComplete the current number of completed items
     * @param currentDurationMillis the current duration of the update process
     *     in milliseconds
     */
    private fun printProgressInfo(
        itemId: Int,
        currentComplete: Long,
        currentDurationMillis: Long,
        startingComplete: Long,
        totalItems: Int?,
        extractor: Extractor
    ) {
        val numComplete = currentComplete - startingComplete
        val pctComplete: String =
            totalItems?.let { (currentComplete.toFloat() / it).toPercent() }
                ?: "???"

        val averageTime: Long = currentDurationMillis / numComplete
        val remainingTime: Long? = totalItems?.let { averageTime * (it - numComplete) }
        val remaining = millisToPretty(remainingTime)
        val elapsed = millisToPretty(currentDurationMillis)

        upNLines(4)
        logger.info { "Extract ${extractor.extractedItem} ${extractor.fromValue}: $itemId" }
        logger.info { "Complete: $currentComplete${totalItems?.let { "/$it" } ?: ""} $pctComplete" }
        logger.info { "Avg: ${averageTime}ms" }
        logger.info { "Elapsed: $elapsed ETR: $remaining" }
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
        batchSize: Int = totalItems?.let { it / 20 } ?: 100000
    ) {
        var currentComplete: Long = startingComplete
        var totalTimeMillis: Long = 0
        var offset = 0

        try {
            getConnection()?.createStatement()?.use { statement ->
                coroutineScope {
                    while (true) {
                        val queryWithLimitOffset = "$getItems LIMIT $batchSize OFFSET ${offset * batchSize}"

                        val executeQuery = statement.executeQuery(queryWithLimitOffset)

                        if (!executeQuery.next()) {
                            logger.info { "No more items to update" }
                            break
                        }

                        executeQuery.use { resultSet ->
                            do {
                                totalTimeMillis += measureTimeMillis {
                                    currentComplete++

                                    val itemId: Int = extractor.extractAndInsert(resultSet, destDatabase)

                                    printProgressInfo(
                                        itemId = itemId,
                                        currentComplete = currentComplete,
                                        currentDurationMillis = totalTimeMillis,
                                        startingComplete = startingComplete,
                                        totalItems = totalItems,
                                        extractor = extractor
                                    )
                                }
                            } while (resultSet.next())
                        }

                        offset++
                    }
                }
            } ?: logger.info { "Unable to get connection" }
        } catch (ex: SQLException) {
            logger.error("Error updating items", ex)
        }

        logger.info { "END\n\n\n" }
    }
}