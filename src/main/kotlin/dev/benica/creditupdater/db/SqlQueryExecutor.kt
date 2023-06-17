package dev.benica.creditupdater.db

import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.SQLException
import java.sql.Statement

class SqlQueryExecutor(
    private val connectionSource: ConnectionSource,
    private val database: String,
) {

    /**
     * Executes an SQL statement.
     *
     * Calls [Statement.execute], [Statement.executeUpdate], or
     * [Statement.executeQuery] depending on the first word of the SQL
     * statement. Comments are ignored.
     *
     * @param sqlStmt the SQL statement to execute
     * @param stmt the [Statement] to use, defaults to a new [Statement] from
     *     the connection
     */
    @Throws(SQLException::class)
    fun executeSqlStatement(
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

    private val logger: KLogger
        get() = KotlinLogging.logger (this::class.java.simpleName)

    /**
     * Runs an SQL script.
     *
     * Statements must be separated by a semicolon (;). If the script is run as
     * a transaction, the script will be rolled back if an exception is thrown.
     * - Note: This function does not handle semicolons in strings.
     * - Note: Comments must be followed by a semicolon. If not, the following
     *   statement will be commented out.
     *
     * @param sqlScriptPath the sql script path
     * @param runAsTransaction whether to run the script as a transaction
     */
    @Throws(SQLException::class)
    fun executeSqlScript(
        sqlScriptPath: String,
        runAsTransaction: Boolean = false
    ) {
        connectionSource.getConnection(database).use { conn ->
            conn.createStatement().use { stmt ->
                try {
                    val statements: List<String> = parseSqlScript(File(sqlScriptPath))
                    if (runAsTransaction) {
                        conn.autoCommit = false
                        executeStatements(statements, stmt)
                        conn.commit()
                    } else {
                        executeStatements(statements, stmt)
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
        }
    }

    /**
     * Executes a list of SQL statements.
     *
     * @param statements the list of statements
     * @param stmt the [Statement] to use, defaults to a new [Statement] from
     *     the connection
     */
    private fun executeStatements(statements: List<String>, stmt: Statement) {
        statements.filter { it.isNotBlank() }.forEach { sqlStmt ->
            logger.info { "${sqlStmt.replace("\\s{2,}".toRegex(), "\n")}\n" }
            executeSqlStatement(sqlStmt, stmt)
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
        file.useLines { lines ->
            lines.filter { it.isNotBlank() }
                .map { it.replace("<schema>", database).trim() }
                .joinToString(separator = " ")
                .split(";")
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
        condition: String? = null
    ): Int {
        val sql = "SELECT COUNT(*) AS count FROM $tableName g" + if (condition != null) " WHERE $condition" else ""
        val stmt = connectionSource.getConnection(database).createStatement()
        val rs = stmt.executeQuery(sql)
        rs.next()
        return rs.getInt(1)
    }
}