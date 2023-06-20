package dev.benica.creditupdater.db

import dev.benica.creditupdater.di.ConnectionSource
import dev.benica.creditupdater.di.DaggerDatabaseComponent
import dev.benica.creditupdater.di.DatabaseComponent
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import javax.inject.Inject

/**
 * A collection of functions that execute SQL queries.
 *
 * @param database the name of the database to use
 * @param databaseComponent the [DatabaseComponent] to use, defaults to a
 *     new [DaggerDatabaseComponent]
 */
class QueryExecutor(
    private val database: String,
    databaseComponent: DatabaseComponent = DaggerDatabaseComponent.create()
) {
    init {
        databaseComponent.inject(this)
    }

    // Dependencies
    @Inject
    internal lateinit var connectionSource: ConnectionSource

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
     */
    @Throws(SQLException::class)
    fun executeSqlStatement(sqlStmt: String) {
        try {
            connectionSource.getConnection(database).connection.use { conn ->
                conn.createStatement().use { stmt ->
                    when {
                        sqlStmt.startsWithAny(listOf("CREATE", "ALTER", "DROP", "TRUNCATE", "SET", "PREPARE", "EXECUTE")) -> {
                            val autoCommit = stmt.connection.autoCommit
                            stmt.connection.autoCommit = true
                            stmt.execute(sqlStmt)
                            stmt.connection.autoCommit = autoCommit
                        }

                        sqlStmt.startsWithAny(listOf("INSERT", "UPDATE", "DELETE")) -> stmt.executeUpdate(sqlStmt)
                        sqlStmt.startsWithAny(listOf("SELECT")) -> stmt.executeQuery(sqlStmt)
                        else -> Unit
                    }
                }
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
     * Executes the SQL [query] and calls [handleResultSet] on the [ResultSet].
     *
     * @param query the SQL query to execute
     * @param handleResultSet the function to call on the [ResultSet]
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun executeQueryAndDo(query: String, handleResultSet: (ResultSet) -> Unit) {
        connectionSource.getConnection(database).connection.use { c ->
            c.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    handleResultSet(rs)
                }
            }
        }
    }

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
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun executeSqlScript(
        sqlScriptPath: String,
        runAsTransaction: Boolean = false
    ) {
        connectionSource.getConnection(database).connection.use { conn ->
            try {
                val statements: List<String> = parseSqlScript(File(sqlScriptPath))
                if (runAsTransaction) {
                    conn.autoCommit = false
                    executeStatements(statements)
                    conn.commit()
                } else {
                    executeStatements(statements)
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

    // Private Methods
    /**
     * Executes a list of SQL statements.
     *
     * @param statements the list of statements
     */
    private fun executeStatements(statements: List<String>) {
        logger.info { "executeStatements: ${statements.size} statements" }
        statements.filter { it.isNotBlank() }.forEach { sqlStmt ->
            logger.info { "${sqlStmt.replace("\\s{2,}".toRegex(), "\n")}\n" }
            executeSqlStatement(sqlStmt)
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
        condition: String? = null
    ): Int {
        val sql = "SELECT COUNT(*) AS count FROM $tableName g" + if (condition != null) " WHERE $condition" else ""
        connectionSource.getConnection(database).connection.use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
                rs.next()
                return rs.getInt(1)
            }
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
        batchAction: (PreparedStatement) -> Unit
    ): IntArray =
        connectionSource.getConnection(database).connection.use { conn ->
            conn.prepareStatement(sql, autoGeneratedKeys).use { stmt ->
                batchAction(stmt)
                stmt.executeBatch()
            }
        }

    fun executePreparedStatement(
        sql: String,
        autoGeneratedKeys: Int = Statement.NO_GENERATED_KEYS,
        act: (PreparedStatement) -> Unit
    ) = connectionSource.getConnection(database).connection.use { conn ->
        conn.prepareStatement(sql, autoGeneratedKeys).use { stmt ->
            act(stmt)
        }
    }
}