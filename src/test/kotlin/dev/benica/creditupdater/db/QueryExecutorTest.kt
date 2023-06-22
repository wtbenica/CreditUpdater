package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
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

        val file = File("test.sql")
        file.writeText(fileContents)

        val actual = queryExecutor.parseSqlScript(file)

        assertEquals(expected, actual)
    }

    @AfterEach
    fun tearDown() {
    }


    // executeSqlStatement
    @Test
    @DisplayName("Should execute CREATE TABLE")
    fun shouldExecuteCreateTable() {
        val tableName = "test_table_create"
        val sqlStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"

        queryExecutor.executeSqlStatement(sqlStmt)

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            val metadata = conn.metaData
            val table = metadata.getTables(null, null, tableName, null)

            assertTrue(table.next(), "Table $tableName should exist")
        }
    }

    @Test
    @DisplayName("Should execute ALTER TABLE")
    fun shouldExecuteAlterTable() {
        val tableName = "test_table_alter"
        val columnName = "test_column"
        val sqlStmt = "ALTER TABLE $tableName ADD COLUMN $columnName VARCHAR(255);"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Execute ALTER TABLE
            queryExecutor.executeSqlStatement(sqlStmt)

            // Check that column exists
            val metadata = conn.metaData
            val table = metadata.getColumns(null, null, tableName, columnName)
            assertTrue(table.next(), "Column $columnName should exist in table $tableName")
        }
    }

    @Test
    @DisplayName("Should execute INSERT INTO")
    fun shouldExecuteInsertInto() {
        val tableName = "test_table_insert_into"
        val sqlStmt = "INSERT INTO $tableName VALUES (1);"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Execute INSERT INTO
            queryExecutor.executeSqlStatement(sqlStmt)

            // Check that row exists
            val selectStmt = "SELECT * FROM $tableName WHERE id = 1;"
            conn.createStatement().use { stmt ->
                val resultSet = stmt.executeQuery(selectStmt)
                assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
            }
        }
    }

    @Test
    @DisplayName("Should execute UPDATE")
    fun shouldExecuteUpdate() {
        val tableName = "test_table_update"
        val sqlStmt = "UPDATE $tableName SET id = 2 WHERE id = 1;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Insert row
            val insertStmt = "INSERT INTO $tableName VALUES (1);"
            conn.createStatement().use { stmt ->
                stmt.execute(insertStmt)
            }

            // Execute UPDATE
            queryExecutor.executeSqlStatement(sqlStmt)

            // Check that row exists
            val selectStmt = "SELECT * FROM $tableName WHERE id = 2;"
            conn.createStatement().use { stmt ->
                val resultSet = stmt.executeQuery(selectStmt)
                assertTrue(resultSet.next(), "Row with id 2 should exist in table $tableName")
            }
        }
    }

    @Test
    @DisplayName("Should execute DELETE")
    fun shouldExecuteDelete() {
        val tableName = "test_table_delete"
        val sqlStmt = "DELETE FROM $tableName WHERE id = 1;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Insert row
            val insertStmt = "INSERT INTO $tableName VALUES (1);"
            conn.createStatement().use { stmt ->
                stmt.execute(insertStmt)
            }

            // Execute DELETE
            queryExecutor.executeSqlStatement(sqlStmt)

            // Check that row exists
            val selectStmt = "SELECT * FROM $tableName WHERE id = 1;"
            conn.createStatement().use { stmt ->
                val resultSet = stmt.executeQuery(selectStmt)
                assertTrue(!resultSet.next(), "Row with id 1 should not exist in table $tableName")
            }
        }
    }

    @Test
    @DisplayName("Should execute DROP TABLE")
    fun shouldExecuteDropTable() {
        val tableName = "test_table_drop"
        val sqlStmt = "DROP TABLE $tableName;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Execute DROP TABLE
            queryExecutor.executeSqlStatement(sqlStmt)

            // Check that table does not exist
            val metadata = conn.metaData
            val table = metadata.getTables(null, null, tableName, null)
            assertTrue(!table.next(), "Table $tableName should not exist")
        }
    }

    @Test
    @DisplayName("Should throw SQLException when executing SELECT statement")
    fun shouldThrowSQLExceptionWhenExecutingSelectStatement() {
        // Setup
        val sqlStmt = "SELECT * FROM test_table;"

        // Execute & Assert
        assertThrows<SQLException> {
            queryExecutor.executeSqlStatement(sqlStmt)
        }
    }

    @Test
    @DisplayName("Should not change database when executing comment")
    fun shouldNotChangeDatabaseWhenExecutingComment() {
        // Setup
        val sqlStmt = "/* comment */"


        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            val databaseBefore = conn.catalog

            // Execute
            queryExecutor.executeSqlStatement(sqlStmt)
            val databaseAfter = conn.catalog

            // Assert
            assertEquals(databaseBefore, databaseAfter, "Database should not change")
        }
    }

    // executeQueryAndDo
    @Test
    @DisplayName("Should execute query and handle result set")
    fun shouldExecuteQueryAndHandleResultSet() {
        val tableName = "test_table_query"
        val sqlStmt = "SELECT * FROM $tableName;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Insert row
            val insertStmt = "INSERT INTO $tableName VALUES (1);"
            conn.createStatement().use { stmt ->
                stmt.execute(insertStmt)
            }

            // Execute query
            queryExecutor.executeQueryAndDo(sqlStmt) { resultSet ->
                assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
                assertFalse(resultSet.next(), "There should be only one row in table $tableName")
            }
        }
    }

    @Test
    @DisplayName("Should execute query and handle result set with multiple rows")
    fun shouldExecuteQueryAndHandleResultSetWithMultipleRows() {
        val tableName = "test_table_query_multiple_rows"
        val sqlStmt = "SELECT * FROM $tableName;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY);"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Insert rows
            val insertStmt = "INSERT INTO $tableName VALUES (1), (2);"
            conn.createStatement().use { stmt ->
                stmt.execute(insertStmt)
            }

            // Execute query
            queryExecutor.executeQueryAndDo(sqlStmt) { resultSet ->
                assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.next(), "Row with id 2 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 2, "Row with id 2 should exist in table $tableName")
                assertFalse(resultSet.next(), "There should be only two rows in table $tableName")
            }
        }
    }

    @Test
    @DisplayName("Should execute query and handle result set with multiple columns")
    fun shouldExecuteQueryAndHandleResultSetWithMultipleColumns() {
        val tableName = "test_table_query_multiple_columns"
        val sqlStmt = "SELECT * FROM $tableName;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY, name VARCHAR(255));"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Insert row
            val insertStmt = "INSERT INTO $tableName VALUES (1, 'test');"
            conn.createStatement().use { stmt ->
                stmt.execute(insertStmt)
            }

            // Execute query
            queryExecutor.executeQueryAndDo(sqlStmt) { resultSet ->
                assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getString("name") == "test", "Row with id 1 should exist in table $tableName")
                assertFalse(resultSet.next(), "There should be only one row in table $tableName")
            }
        }
    }

    @Test
    @DisplayName("Should execute query and handle result set with multiple rows and columns")
    fun shouldExecuteQueryAndHandleResultSetWithMultipleRowsAndColumns() {
        val tableName = "test_table_query_multiple_rows_and_columns"
        val sqlStmt = "SELECT * FROM $tableName;"

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
        ).use { conn ->
            // Create test table
            val createTableStmt = "CREATE TABLE $tableName (id INT PRIMARY KEY, name VARCHAR(255));"
            conn.createStatement().use { stmt ->
                stmt.execute(createTableStmt)
            }

            // Insert rows
            val insertStmt = "INSERT INTO $tableName VALUES (1, 'test1'), (2, 'test2');"
            conn.createStatement().use { stmt ->
                stmt.execute(insertStmt)
            }

            // Execute query
            queryExecutor.executeQueryAndDo(sqlStmt) { resultSet ->
                assertTrue(resultSet.next(), "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 1, "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.getString("name") == "test1", "Row with id 1 should exist in table $tableName")
                assertTrue(resultSet.next(), "Row with id 2 should exist in table $tableName")
                assertTrue(resultSet.getInt("id") == 2, "Row with id 2 should exist in table $tableName")
                assertTrue(resultSet.getString("name") == "test2", "Row with id 2 should exist in table $tableName")
                assertFalse(resultSet.next(), "There should be only two rows in table $tableName")
            }
        }
    }

    // executeSqlScript
    //@Test
    //@DisplayName("Should execute sql script")


    // parseSqlScript
    @Test
    fun `parseSqlScript$CreditUpdater`() {
    }

    @Test
    fun `getItemCount$CreditUpdater`() {
    }

    @Test
    fun executePreparedStatementBatch() {
    }

    @Test
    fun executePreparedStatement() {
    }

    companion object {
        private lateinit var queryExecutor: QueryExecutor

        @BeforeAll
        @JvmStatic
        fun setUp() {
            queryExecutor = QueryExecutor("credit_updater_test")

            dropAllTables()
        }

        @AfterAll
        @JvmStatic
        fun breakDown() {
            dropAllTables()
        }

        private fun dropAllTables() {
            DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/credit_updater_test", USERNAME_INITIALIZER, PASSWORD_INITIALIZER
            ).use { conn ->
                val query =
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'credit_updater_test'"
                conn.createStatement().use { stmt ->
                    // Retrieve the names of all tables in the database
                    stmt.executeQuery(query).use { resultSet ->
                        val tableNames = mutableListOf<String>()

                        // Store the table names in a list
                        while (resultSet.next()) {
                            val tableName = resultSet.getString("table_name")
                            tableNames.add(tableName)
                        }

                        // Generate and execute DROP TABLE statements for each table
                        tableNames.forEach { tableName ->
                            val dropStatement = "DROP TABLE $tableName"
                            stmt.executeUpdate(dropStatement)
                        }
                    }
                }
            }
        }
    }
}