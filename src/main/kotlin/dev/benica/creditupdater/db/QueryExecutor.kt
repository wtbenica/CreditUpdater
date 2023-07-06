package dev.benica.creditupdater.db

import dev.benica.creditupdater.di.DatabaseComponent
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.*

/**
 * A collection of functions that execute SQL queries.
 *
 * @param schema the name of the database to use
 */
class QueryExecutor(
    private val schema: String,
) {
    // Private Properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    // Public Methods
    /**
     * Executes an SQL statement.
     *
     * Calls [Statement.execute], [Statement.executeUpdate], or
     * [Statement.executeQuery] depending on the first word of the SQL
     * statement. Comments are ignored.
     *
     * @param sqlStmt the SQL statement to execute
     * @param connection the [Connection] to use, or null to use a new
     *     connection
     * @throws SQLException if an error occurs
     * @notes If a non-null [connection] is provided, the function respects its
     *     [autoCommit] setting. If [autoCommit] is true, each statement is
     *     automatically committed. If false, the caller is responsible for
     *     handling the commit or rollback of the connection if needed.
     *
     *     If [connection] is null, the default [autoCommit] value [true] is
     *     used. Any statement is committed and the connection is closed.
     */
    @Throws(SQLException::class)
    fun executeSqlStatement(sqlStmt: String, connection: Connection) {
        try {
            @Suppress("kotlin:S6314")
            connection.createStatement().use { stmt ->
                when {
                    sqlStmt.startsWithAny(listOf("INSERT", "UPDATE", "DELETE")) -> stmt.executeUpdate(sqlStmt)
                    //sqlStmt.startsWithAny(listOf("SELECT")) -> throw SQLException("Use executeQueryAndDo for SELECT statements")
                    else -> stmt.execute(sqlStmt)
                }
            }
        } catch (sqlEx: SQLException) {
            logger.error("Error running SQL script $sqlStmt", sqlEx)
            throw sqlEx
        }
    }

    /**
     * Executes the SQL [query] and calls [handleResultSet] on the [ResultSet].
     *
     * @param query the SQL query to execute
     * @param handleResultSet the function to call on the [ResultSet]
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun executeQueryAndDo(query: String, conn: Connection, handleResultSet: (ResultSet) -> Unit) {
        conn.createStatement().use { stmt ->
            stmt.executeQuery(query).use { rs ->
                handleResultSet(rs)
            }
        }
    }

    /**
     * Runs an SQL script.
     *
     * Statements must be separated by a semicolon \(;). If the script is run
     * as a transaction, the script will be rolled back if an exception is
     * thrown.
     * - Note: This function does not handle semicolons in strings.
     * - Note: Comments must be followed by a semicolon. If not, the following
     *   statement will be commented out.
     *
     * @param sqlScript the SQL script to run
     * @param runAsTransaction whether to run the script as a transaction
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun executeSqlScript(
        sqlScript: File,
        runAsTransaction: Boolean = false,
        conn: Connection,
    ) {
        try {
            val statements: List<String> = sqlScript.parseSqlScript()
            if (runAsTransaction) {
                conn.autoCommit = false
                executeStatements(statements, conn)
                conn.commit()
            } else {
                executeStatements(statements, conn)
            }
        } catch (sqlEx: SQLException) {
            logger.error("Error running SQL script", sqlEx)
            if (runAsTransaction) {
                conn.rollback()
            }
            throw sqlEx
        } finally {
            if (runAsTransaction) {
                conn.autoCommit = true
            }
        }
    }

// Private Methods
    /**
     * Executes a list of SQL statements.
     *
     * @param statements the list of statements
     */
    internal fun executeStatements(statements: List<String>, conn: Connection) {
        logger.info { "executeStatements: ${statements.size} statements" }
        statements.filter { it.isNotBlank() }.forEach { sqlStmt ->
            logger.info { "${sqlStmt.replace("\\s{2,}".toRegex(), "\n")}\n" }
            executeSqlStatement(sqlStmt, conn)
        }
    }

    /**
     * Parses an SQL script into a list of statements.
     *
     * The script is split on semicolons and then each statement is trimmed and
     * appended to a list. The list is then returned.
     * - Note: This function does not handle semicolons in strings.
     * - Note: Comments must be followed by a semicolon. If not, the following
     *   statement will be commented out.
     *
     * @param file the file to parse
     * @return the list of statements
     */
    internal fun parseSqlScript(file: File): List<String> =
        file.useLines(Charsets.UTF_8) { lines: Sequence<String> ->
            lines.filter { it.isNotBlank() }
                .map { it.replace("<schema>", schema).trim() }
                .joinToString(separator = " ")
                .split(";")
                .filter { it.isNotBlank() }
                .map { it.trim() }
        }


    /**
     * Checks if a string starts with any of the strings in a list.
     * - Note: Comparisons are case-insensitive.
     *
     * @param list the list of strings to check
     * @return true if the string starts with any of the strings in the list,
     */
    private fun String.startsWithAny(list: List<String>): Boolean =
        list.any { this.uppercase().startsWith(it.uppercase()) }

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
        condition: String? = null,
        conn: Connection
    ): Int {
        val sql =
            "SELECT COUNT(*) AS count FROM $tableName g" + if (condition != null) " WHERE $condition" else ""
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery(sql)
            rs.next()
            return rs.getInt(1)
        }
    }

    /**
     * Executes a prepared statement batch insert/modification.
     *
     * @param sql the SQL statement
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *     should be returned. Either [Statement.NO_GENERATED_KEYS]
     *     or [Statement.RETURN_GENERATED_KEYS
     * @param batchAction the act
     * @return the int array
     */
    fun executePreparedStatementBatch(
        sql: String,
        autoGeneratedKeys: Int = Statement.NO_GENERATED_KEYS,
        conn: Connection,
        batchAction: (PreparedStatement) -> Unit
    ): IntArray =
        conn.prepareStatement(sql, autoGeneratedKeys).use { stmt ->
            batchAction(stmt)
            stmt.executeBatch()
        }

    /**
     * Executes a prepared statement.
     *
     * @param sql the SQL statement
     * @param act the act
     * @throws SQLException the SQL exception
     */
    @Throws(SQLException::class)
    fun executePreparedStatement(
        sql: String,
        conn: Connection,
        act: (PreparedStatement) -> Unit
    ) = conn.prepareStatement(sql).use { stmt ->
        act(stmt)
    }

    private fun File.parseSqlScript(): List<String> =
        useLines(Charsets.UTF_8) { lines: Sequence<String> ->
            lines.filter { it.isNotBlank() }
                .map { it.replace("<schema>", schema).trim() }
                .joinToString(separator = " ")
                .split(";")
                .filter { it.isNotBlank() }
                .map { it.trim() }
        }
}

