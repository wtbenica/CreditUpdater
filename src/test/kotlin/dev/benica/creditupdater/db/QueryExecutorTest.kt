package dev.benica.creditupdater.db

import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.dropAllTables
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getDbConnection
import dev.benica.creditupdater.di.ConnectionSource
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.io.File
import java.sql.*

class QueryExecutorTest {
    @Test
    @DisplayName("should parse script correctly")
    fun shouldParseSqlScript() {
        val fileContents =
            """SELECT * 
                |FROM table 
                |WHERE id = 3;
                |SELECT * 
                |FROM table 
                |WHERE id = 4;
                |# This is a comment;
                |SELECT * 
                |FROM table 
                |WHERE id = 5;;;
                |# This is a comment with no semicolon
                |SELECT * FROM table WHERE id = 6;""".trimMargin()

        val expected = listOf(
            "SELECT * FROM table WHERE id = 3",
            "SELECT * FROM table WHERE id = 4",
            "# This is a comment",
            "SELECT * FROM table WHERE id = 5",
            "# This is a comment with no semicolon SELECT * FROM table WHERE id = 6"
        )

        val file = File("temp.sql")
        file.writeText(fileContents)

        val actual = file.parseSqlScript(TEST_DATABASE)

        assertEquals(expected, actual)
    }

    // executeSqlStatement
    @Test
    @DisplayName("Should execute CREATE TABLE")
    fun shouldExecuteCreateTable() {
        val tableName = "test_table_create"
        val sqlStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"

        queryExecutor.executeSqlStatement(sqlStmt, mConn)

        val metadata = mConn.metaData
        val table = metadata.getTables(null, null, tableName, null)

        assertTrue(table.next(), "Table $tableName should exist")
    }

    @Test
    @DisplayName("Should execute ALTER TABLE")
    fun shouldExecuteAlterTable() {
        val tableName = "test_table_alter"
        val columnName = "test_column"
        val sqlStmt = "ALTER TABLE $tableName ADD COLUMN $columnName VARCHAR(255);"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Execute ALTER TABLE
        queryExecutor.executeSqlStatement(sqlStmt, mConn)

        // Check that column exists
        val metadata = mConn.metaData
        val table = metadata.getColumns(null, null, tableName, columnName)
        assertTrue(table.next(), "Column $columnName should exist in table $tableName")

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute INSERT INTO")
    fun shouldExecuteInsertInto() {
        val tableName = "test_table_insert_into"
        val sqlStmt = "INSERT INTO $tableName VALUES (1);"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Execute INSERT INTO
        queryExecutor.executeSqlStatement(sqlStmt, mConn)

        // Check that row exists
        val selectStmt = "SELECT * FROM $tableName WHERE id = 1;"
        mConn.createStatement().use { stmt ->
            val resultSet = stmt.executeQuery(selectStmt)
            assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute UPDATE")
    fun shouldExecuteUpdate() {
        val tableName = "test_table_update"
        val sqlStmt = "UPDATE $tableName SET id = 2 WHERE id = 1;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Insert row
        val insertStmt = "INSERT INTO $tableName VALUES (1);"
        mConn.createStatement().use { stmt ->
            stmt.execute(insertStmt)
        }

        // Execute UPDATE
        queryExecutor.executeSqlStatement(sqlStmt, mConn)

        // Check that row exists
        val selectStmt = "SELECT * FROM $tableName WHERE id = 2;"
        mConn.createStatement().use { stmt ->
            val resultSet = stmt.executeQuery(selectStmt)
            assertTrue(resultSet.next(), "Row with id 2 should exist in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute DELETE")
    fun shouldExecuteDelete() {
        val tableName = "test_table_delete"
        val sqlStmt = "DELETE FROM $tableName WHERE id = 1;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Insert row
        val insertStmt = "INSERT INTO $tableName VALUES (1);"
        mConn.createStatement().use { stmt ->
            stmt.execute(insertStmt)
        }

        // Execute DELETE
        queryExecutor.executeSqlStatement(sqlStmt, mConn)

        // Check that row exists
        val selectStmt = "SELECT * FROM $tableName WHERE id = 1;"
        mConn.createStatement().use { stmt ->
            val resultSet = stmt.executeQuery(selectStmt)
            assertTrue(!resultSet.next(), "Row with id 1 should not exist in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute DROP TABLE")
    fun shouldExecuteDropTable() {
        val tableName = "test_table_drop"
        val sqlStmt = "DROP TABLE $tableName;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Execute DROP TABLE
        queryExecutor.executeSqlStatement(sqlStmt, mConn)

        // Check that table does not exist
        val metadata = mConn.metaData
        val table = metadata.getTables(null, null, tableName, null)
        assertTrue(!table.next(), "Table $tableName should not exist")
    }

    @Test
    @DisplayName("Should throw SQLException when executing SELECT statement")
    fun shouldThrowSQLExceptionWhenExecutingSelectStatement() {
        // Setup
        val sqlStmt = "SELECT * FROM test_table;"

        // Execute & Assert
        assertThrows<SQLException> {
            queryExecutor.executeSqlStatement(sqlStmt, mConn)
        }
    }

    @Test
    @DisplayName("Should not change database when executing comment")
    fun shouldNotChangeDatabaseWhenExecutingComment() {
        // Setup
        val sqlStmt = "/* comment */"

        val databaseBefore = mConn.catalog

        // Execute
        queryExecutor.executeSqlStatement(sqlStmt, mConn)
        val databaseAfter = mConn.catalog

        // Assert
        assertEquals(databaseBefore, databaseAfter, "Database should not change")
    }

    // executeQueryAndDo
    @Test
    @DisplayName("Should execute query and handle result set")
    fun shouldExecuteQueryAndHandleResultSet() {
        val tableName = "test_table_query"
        val sqlStmt = "SELECT * FROM $tableName;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Insert row
        val insertStmt = "INSERT INTO $tableName VALUES (1);"
        mConn.createStatement().use { stmt ->
            stmt.execute(insertStmt)
        }

        // Execute query
        queryExecutor.executeQueryAndDo(sqlStmt, mConn) { resultSet ->
            assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
            assertFalse(resultSet.next(), "There should be only one row in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute query and handle result set with multiple rows")
    fun shouldExecuteQueryAndHandleResultSetWithMultipleRows() {
        val tableName = "test_table_query_multiple_rows"
        val sqlStmt = "SELECT * FROM $tableName;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Insert rows
        val insertStmt = "INSERT INTO $tableName VALUES (1), (2);"
        mConn.createStatement().use { stmt ->
            stmt.execute(insertStmt)
        }

        // Execute query
        queryExecutor.executeQueryAndDo(sqlStmt, mConn) { resultSet ->
            assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.next(), "Row with id 2 should exist in table $tableName")
            assertTrue(resultSet.getInt("id") == 2, "Row with id 2 should exist in table $tableName")
            assertFalse(resultSet.next(), "There should be only two rows in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute query and handle result set with multiple columns")
    fun shouldExecuteQueryAndHandleResultSetWithMultipleColumns() {
        val tableName = "test_table_query_multiple_columns"
        val sqlStmt = "SELECT * FROM $tableName;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY, name VARCHAR(255));"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Insert row
        val insertStmt = "INSERT INTO $tableName VALUES (1, 'test');"
        mConn.createStatement().use { stmt ->
            stmt.execute(insertStmt)
        }

        // Execute query
        queryExecutor.executeQueryAndDo(sqlStmt, mConn) { resultSet ->
            assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.getString("name") == "test", "Row with id 1 should exist in table $tableName")
            assertFalse(resultSet.next(), "There should be only one row in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute query and handle result set with multiple rows and columns")
    fun shouldExecuteQueryAndHandleResultSetWithMultipleRowsAndColumns() {
        val tableName = "test_table_query_multiple_rows_and_columns"
        val sqlStmt = "SELECT * FROM $tableName;"

        // Create test table
        val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY, name VARCHAR(255));"
        mConn.createStatement().use { stmt ->
            stmt.execute(createTableStmt)
        }

        // Insert rows
        val insertStmt = "INSERT INTO $tableName VALUES (1, 'test1'), (2, 'test2');"
        mConn.createStatement().use { stmt ->
            stmt.execute(insertStmt)
        }

        // Execute query
        queryExecutor.executeQueryAndDo(sqlStmt, mConn) { resultSet ->
            assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.getString("name") == "test1", "Row with id 1 should exist in table $tableName")
            assertTrue(resultSet.next(), "Row with id 2 should exist in table $tableName")
            assertTrue(resultSet.getInt("id") == 2, "Row with id 2 should exist in table $tableName")
            assertTrue(resultSet.getString("name") == "test2", "Row with id 2 should exist in table $tableName")
            assertFalse(resultSet.next(), "There should be only two rows in table $tableName")
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    // executeSqlScript
    @Test
    @DisplayName("Should execute SQL script")
    fun shouldExecuteSqlScript() {
        val tableName = "test_table_execute_sql_script"
        val sqlScript = File("temp.sql").apply {
            writeText(
                """CREATE TABLE $tableName (id INT PRIMARY KEY);
                    INSERT INTO $tableName VALUES (1);""".trimIndent()
            )
        }

        queryExecutor.executeSqlScript(sqlScript, conn = mConn)

        // Check if table exists
        val query =
            "SELECT table_name FROM information_schema.tables WHERE table_schema = '$TEST_DATABASE' AND table_name = '$tableName'"
        mConn.createStatement().use { stmt ->
            stmt.executeQuery(query).use { resultSet ->
                assertTrue(resultSet.next(), "Table $tableName should exist")
                assertTrue(resultSet.getString("table_name") == tableName, "Table $tableName should exist")
                assertFalse(resultSet.next(), "There should be only one table")
            }
        }

        // Check if row exists
        val query2 = "SELECT * FROM $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.executeQuery(query2).use { resultSet ->
                assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
                assertFalse(resultSet.next(), "There should be only one row in table $tableName")
            }
        }

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should execute SQL script as transaction when runAsTransaction is true")
    fun shouldExecuteSqlScriptAsTransactionWhenRunAsTransactionIsTrue() {
        // Create mock objects
        val connectionSourceMock: ConnectionSource = mock()
        val hikariDataSourceMock: HikariDataSource = mock()
        val connectionMock: Connection = mock()
        val statementMock: Statement = mock()
        val resultSetMock = mock<ResultSet>()

        val queryExecutor = QueryExecutor(TEST_DATABASE)

        // Mock the behavior of the mocked objects
        whenever(connectionSourceMock.getConnection(TEST_DATABASE)).thenReturn(hikariDataSourceMock)
        whenever(hikariDataSourceMock.connection).thenReturn(connectionMock)
        whenever(connectionMock.createStatement()).thenReturn(statementMock)
        whenever(statementMock.executeQuery(any())).thenReturn(resultSetMock)

        // Call the method under test
        val table = "test_table5"
        val sqlScript = File("temp.sql").apply {
            writeText(
                """INSERT INTO $table VALUES (1);
                |INSERT INTO $table VALUES (2);
                |INSERT INTO $table VALUES (3);
            """.trimMargin()
            )
        }
        queryExecutor.executeSqlScript(sqlScript, true, connectionMock)

        // Verify the expected interactions
        verify(connectionMock, times(3)).createStatement()
        verify(statementMock).executeUpdate("INSERT INTO $table VALUES (1)")
        verify(statementMock).executeUpdate("INSERT INTO $table VALUES (2)")
        verify(statementMock).executeUpdate("INSERT INTO $table VALUES (3)")
        verify(connectionMock).commit()
        verify(connectionMock).autoCommit = false
        verify(connectionMock).autoCommit = true
    }

    @Test
    @DisplayName("Should execute SQL script as transaction when runAsTransaction is true and rollback on error and throw exception")
    fun shouldExecuteSqlScriptAsTransactionWhenRunAsTransactionIsTrueAndRollbackOnErrorAndThrowException() {
        // Create mock objects
        val connectionSourceMock = mock<ConnectionSource>()
        val hikariDataSourceMock = mock<HikariDataSource>()
        val connectionMock = mock<Connection>()
        val statementMock = mock<Statement>()
        val resultSetMock = mock<ResultSet>()

        // Create a QueryExecutor instance with the mocked objects
        val queryExecutor = QueryExecutor(TEST_DATABASE)

        // Mock the behavior of the mocked objects
        whenever(connectionSourceMock.getConnection(TEST_DATABASE)).thenReturn(hikariDataSourceMock)
        whenever(hikariDataSourceMock.connection).thenReturn(connectionMock)
        whenever(connectionMock.createStatement()).thenReturn(statementMock)
        whenever(statementMock.executeQuery(any())).thenReturn(resultSetMock)
        val table = "test_table6"
        whenever(statementMock.executeUpdate("INSERT INTO $table VALUES (1)")).thenThrow(SQLException::class.java)

        // Call the method under test
        val sqlScript = File("temp.sql").apply {
            writeText(
                """INSERT INTO $table VALUES (1);
                |INSERT INTO $table VALUES (2);
                |INSERT INTO $table VALUES (3);
            """.trimMargin()
            )
        }
        assertThrows<SQLException> { queryExecutor.executeSqlScript(sqlScript, true, connectionMock) }

        // Verify the expected interactions
        verify(connectionMock).createStatement()
        verify(statementMock).executeUpdate("INSERT INTO $table VALUES (1)")
        verify(statementMock, never()).executeUpdate("INSERT INTO $table VALUES (2)")
        verify(statementMock, never()).executeUpdate("INSERT INTO $table VALUES (3)")
        verify(connectionMock).rollback()
        verify(connectionMock).autoCommit = false
        verify(connectionMock).autoCommit = true
    }

    // getItemCount
    @Test
    @DisplayName("Should return the number of items in the specified table")
    fun shouldReturnTheNumberOfItemsInTheSpecifiedTable() {
        val tableName = "test_table1"
        // create table
        mConn.createStatement().use { stmt ->
            stmt.executeUpdate("CREATE TABLE $tableName (id INT);")
        }

        val sqlScript = File("temp.sql").apply {
            writeText(
                """INSERT INTO $tableName VALUES (1);
                    |INSERT INTO $tableName VALUES (2);
                    |INSERT INTO $tableName VALUES (3);""".trimMargin()
            )
        }

        queryExecutor.executeSqlScript(sqlScript, conn = mConn)

        val itemCount = queryExecutor.getItemCount(tableName, conn = mConn)

        assertEquals(3, itemCount, "There should be 3 items in table $tableName")

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    @Test
    @DisplayName("Should return zero when the specified table is empty")
    fun shouldReturnZeroWhenTheSpecifiedTableIsEmpty() {
        val tableName = "test_table2"
        // create table
        mConn.createStatement().use { stmt ->
            stmt.executeUpdate("CREATE TABLE $tableName (id INT);")
        }

        val itemCount = queryExecutor.getItemCount(tableName, conn = mConn)

        assertEquals(0, itemCount, "There should be 0 items in table $tableName")

        // cleanup
        val dropTableStmt = "DROP TABLE $tableName;"
        mConn.createStatement().use { stmt ->
            stmt.execute(dropTableStmt)
        }
    }

    // executePreparedStatementBatch
    @Test
    @DisplayName("Should execute the batch action on the prepared statement")
    fun shouldExecuteAPreparedStatementBatchInsertOrModification() {
        // Mock the necessary dependencies
        val connectionSource = mock<ConnectionSource>()
        val dataSource = mock<HikariDataSource>()
        val connection = mock<Connection>()
        val preparedStatement = mock<PreparedStatement>()

        whenever(connectionSource.getConnection(any())).thenReturn(dataSource)
        whenever(dataSource.connection).thenReturn(connection)
        whenever(connection.prepareStatement(any(), any<Int>())).thenReturn(preparedStatement)

        // Define the SQL statement and the batch action
        val sql = "INSERT INTO users (name, age) VALUES (?, ?)"
        val batchAction: (PreparedStatement) -> Unit = { stmt ->
            stmt.setString(1, "John")
            stmt.setInt(2, 30)
            stmt.addBatch()

            stmt.setString(1, "Jane")
            stmt.setInt(2, 25)
            stmt.addBatch()
        }

        // Mock the executeBatch method to return the expected result
        whenever(preparedStatement.executeBatch()).thenReturn(intArrayOf(1, 1))

        // Call the method under test
        val result = QueryExecutor(TEST_DATABASE).executePreparedStatementBatch(
            sql,
            Statement.RETURN_GENERATED_KEYS,
            connection,
            batchAction
        )

        // Verify the interactions and assertions
        verify(connection).prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        verify(preparedStatement).setString(1, "John")
        verify(preparedStatement).setInt(2, 30)
        verify(preparedStatement).setString(1, "Jane")
        verify(preparedStatement).setInt(2, 25)
        verify(preparedStatement, times(2)).addBatch()
        verify(preparedStatement).executeBatch()

        assertEquals(2, result.size)
        assertEquals(1, result[0])
        assertEquals(1, result[1])

        // Additional assertion to test the return value
        val expectedReturnValue = intArrayOf(1, 1)
        assertArrayEquals(expectedReturnValue, result)
    }

    @Test
    @DisplayName("Should execute the batch action on the prepared statement")
    fun shouldExecuteAPreparedStatementBatchWithAutoGenKeys() {
        // Mock the necessary dependencies
        val connectionSource = mock<ConnectionSource>()
        val dataSource = mock<HikariDataSource>()
        val connection = mock<Connection>()
        val preparedStatement = mock<PreparedStatement>()

        whenever(connectionSource.getConnection(any())).thenReturn(dataSource)
        whenever(dataSource.connection).thenReturn(connection)
        whenever(connection.prepareStatement(any(), any<Int>())).thenReturn(preparedStatement)

        // Create an instance of QueryExecutor
        val queryExecutor = QueryExecutor(TEST_DATABASE)

        // Define the SQL statement and the batch action
        val sql = "INSERT INTO users (name, age) VALUES (?, ?)"
        val batchAction: (PreparedStatement) -> Unit = { stmt ->
            stmt.setString(1, "John")
            stmt.setInt(2, 30)
            stmt.addBatch()

            stmt.setString(1, "Jane")
            stmt.setInt(2, 25)
            stmt.addBatch()
        }

        // Mock the executeBatch method to return the expected result
        whenever(preparedStatement.executeBatch()).thenReturn(intArrayOf(1, 1))

        // Call the method under test
        val result = queryExecutor.executePreparedStatementBatch(sql, Statement.RETURN_GENERATED_KEYS, connection, batchAction)

        // Verify the interactions and assertions
        assertEquals(2, result.size)
        assertEquals(1, result[0])
        assertEquals(1, result[1])

        // Additional assertion to test the return value
        val expectedReturnValue = intArrayOf(1, 1)
        assertArrayEquals(expectedReturnValue, result)
    }

    @Test
    @DisplayName("Should throw exception when the batch action throws exception")
    fun shouldExecutePreparedStatement() {
        // Mock the necessary dependencies
        val connectionSource = mock<ConnectionSource>()
        val hikariDataSource = mock<HikariDataSource>()
        val connection = mock<Connection>()
        val preparedStatement = mock<PreparedStatement>()

        whenever(connectionSource.getConnection(any())).thenReturn(hikariDataSource)
        whenever(hikariDataSource.connection).thenReturn(connection)
        whenever(connection.prepareStatement(any())).thenReturn(preparedStatement)

        // Create an instance of QueryExecutor
        val queryExecutor = QueryExecutor(TEST_DATABASE)

        // Define the SQL statement and the action
        val sql = "INSERT INTO users (name, age) VALUES (?, ?)"
        val action: (PreparedStatement) -> Unit = { stmt ->
            stmt.setString(1, "John")
            stmt.setInt(2, 30)
            stmt.executeUpdate()

            stmt.setString(1, "Jane")
            stmt.setInt(2, 25)
            stmt.addBatch()
        }

        // Call the method under test
        queryExecutor.executePreparedStatement(sql, connection, action)

        // Verify the interactions and assertions
        verify(connection).prepareStatement(sql)
        verify(preparedStatement).setString(1, "John")
        verify(preparedStatement).setInt(2, 30)
        verify(preparedStatement).executeUpdate()
        verify(preparedStatement).setString(1, "Jane")
        verify(preparedStatement).setInt(2, 25)
        verify(preparedStatement).addBatch()
    }

    @AfterEach
    fun tearDown() {
        dropAllTables(mConn, TEST_DATABASE)
    }

    companion object {
        private lateinit var queryExecutor: QueryExecutor
        private lateinit var mConn: Connection

        @BeforeAll
        @JvmStatic
        fun setUp() {
            mConn = getDbConnection(TEST_DATABASE)
            queryExecutor = QueryExecutor(TEST_DATABASE)

            dropAllTables(mConn, TEST_DATABASE)
        }

        @AfterAll
        @JvmStatic
        fun breakDown() {
            dropAllTables(mConn, TEST_DATABASE)
            mConn.close()
            removeSqlScriptFiles()
        }

        private fun removeSqlScriptFiles(): Boolean = File("temp.sql").delete()
    }
}
