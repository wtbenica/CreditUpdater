package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.Companion.DEFAULT_BATCH_SIZE
import dev.benica.creditupdater.converter.Extractor
import dev.benica.creditupdater.di.DatabaseComponent
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.sql.*
import javax.inject.Inject

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
    private val logger: KLogger
        get() = KotlinLogging.logger (this::class.java.simpleName)

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
    fun getConnection(): Connection? =
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
        SqlQueryExecutor(connectionSource, database).executeSqlScript(sqlScriptPath, runAsTransaction)
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
        stmt: Statement = connectionSource.getConnection(database).createStatement()
    ) {
        SqlQueryExecutor(connectionSource, database).executeSqlStatement(sqlStmt, stmt)
    }

    /**
     * Get item count - gets the number of items in a table.
     *
     * @param tableName the table name
     * @param condition the condition
     * @return the item count
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun getItemCount(
        tableName: String,
        condition: String? = null
    ): Int = SqlQueryExecutor(connectionSource, database).getItemCount(tableName, condition)

    /**
     * Updates items in the database using the given SQL query to retrieve the
     * items.
     *
     * @param selectItemsQuery the SQL query to retrieve the items
     * @param startingComplete the starting number of completed items
     * @param totalItems the total number of items to update, or null if
     *     unknown
     * @param extractor the extractor to use to extract the items from the
     *     result set
     * @param batchSize the batch size to use
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    suspend fun extractAndInsertItems(
        selectItemsQuery: String,
        startingComplete: Long,
        totalItems: Int?,
        extractor: Extractor,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        withContext(ioDispatcher) {
            DBUpdateMonitor(connectionSource, database).extractAndInsertItems(
                selectItemsQuery = selectItemsQuery,
                startingComplete = startingComplete,
                totalItems = totalItems,
                extractor = extractor,
                batchSize = batchSize
            )
        }
    }
}

