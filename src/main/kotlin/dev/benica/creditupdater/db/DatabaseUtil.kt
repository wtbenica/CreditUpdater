package dev.benica.creditupdater.db

import dev.benica.creditupdater.di.DaggerDatabaseComponent
import dev.benica.creditupdater.di.DatabaseComponent
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.sql.*
import javax.inject.Inject

/**
 * Database util - utility functions for the database.
 *
 * @param targetSchema the database to connect to
 * @constructor Create empty Database util
 */
class DatabaseUtil(
    private val targetSchema: String,
    databaseComponent: DatabaseComponent = DaggerDatabaseComponent.create(),
) {
    init {
        databaseComponent.inject(this)
    }

    @Inject
    internal lateinit var connectionSource: ConnectionSource
    // Properties

    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    // Public Methods
    /**
     * Get connection - gets a connection to the database.
     *
     * @return the connection
     */
    fun getConnection(): Connection? =
        try {
            connectionSource.getConnection(targetSchema)
        } catch (sqlEx: SQLException) {
            logger.error("Error getting connection", sqlEx)
            null
        } catch (ex: Exception) {
            logger.error("Error getting connection", ex)
            null
        }

    /**
     * Runs an SQL script. Statements must be separated by a semicolon (;). If
     * the script is run as a transaction, the script will be rolled back if an
     * exception is thrown.
     * - Note: This function does not handle semicolons in strings.
     * - Note: Comments must be followed by a semicolon. If not, the following
     *   statement will be commented out.
     *
     * @param sqlScriptPath the sql script path
     * @param runAsTransaction whether to run the script as a transaction
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun runSqlScript(
        sqlScriptPath: String,
        runAsTransaction: Boolean = false
    ) {
        SqlQueryExecutor(targetSchema).executeSqlScript(sqlScriptPath, runAsTransaction)
    }

    /**
     * Executes an SQL statement.
     *
     * @param sqlStmt the SQL statement to execute
     * @param stmt the statement to use, or null to create a new statement
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun executeSqlStatement(
        sqlStmt: String,
        stmt: Statement = connectionSource.getConnection(targetSchema).createStatement()
    ) {
        SqlQueryExecutor(targetSchema).executeSqlStatement(sqlStmt, stmt)
    }
}
