package dev.benica.creditupdater.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class QueryExecutorTest {
    private lateinit var queryExecutor: QueryExecutor
    private lateinit var connection: Connection
    private lateinit var statement: Statement
    private lateinit var resultSet: ResultSet

    @BeforeEach
    fun setUp() {
        connection = mock(Connection::class.java)
        statement = mock(Statement::class.java)
        resultSet = mock(ResultSet::class.java)

        queryExecutor = QueryExecutor("A string")
    }

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

    /*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class SqlQueryExecutorTest {
    private lateinit var sqlQueryExecutor: SqlQueryExecutor
    private lateinit var connectionSource: ConnectionSource
    private lateinit var connection: Connection
    private lateinit var statement: Statement
    private lateinit var preparedStatement: PreparedStatement
    private lateinit var resultSet: ResultSet

    @BeforeEach
    fun setUp() {
        connectionSource = mock(ConnectionSource::class.java)
        connection = mock(Connection::class.java)
        statement = mock(Statement::class.java)
        preparedStatement = mock(PreparedStatement::class.java)
        resultSet = mock(ResultSet::class.java)

        `when`(connectionSource.getConnection()).thenReturn(connection)
        `when`(connection.createStatement()).thenReturn(statement)
        `when`(connection.prepareStatement("INSERT INTO table (column1, column2) VALUES (?, ?)")).thenReturn(preparedStatement)
        `when`(preparedStatement.executeUpdate()).thenReturn(1)
        `when`(statement.executeQuery("SELECT COUNT(*) FROM table")).thenReturn(resultSet)
        `when`(resultSet.next()).thenReturn(true)
        `when`(resultSet.getInt(1)).thenReturn(1)

        sqlQueryExecutor = SqlQueryExecutor(connectionSource)
    }

    @Test
    @DisplayName("should insert row into database")
    fun shouldInsertRowIntoDatabase() {
        val expected = 1
        val actual = sqlQueryExecutor.executeSqlStatement("INSERT INTO table (column1, column2) VALUES (?, ?)", listOf("value1", "value2"))

        assertEquals(expected, actual)

        // Check that the row was inserted into the database
        val countStatement = connection.createStatement()
        val countResultSet = countStatement.executeQuery("SELECT COUNT(*) FROM table")
        countResultSet.next()
        val rowCount = countResultSet.getInt(1)

        assertEquals(1, rowCount)
    }
}
 */
}