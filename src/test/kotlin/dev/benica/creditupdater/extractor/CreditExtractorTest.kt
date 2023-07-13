package dev.benica.creditupdater.extractor

import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.di.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class CreditExtractorTest {
    private val database: String = TEST_DATABASE
    private lateinit var connectionSource: ConnectionSource
    private lateinit var dataSource: HikariDataSource
    private lateinit var connection: Connection
    private lateinit var creditExtractor: CreditExtractor

    @BeforeEach
    fun setUp() {
        connectionSource = mock<ConnectionSource>()
        dataSource = mock<HikariDataSource>()
        connection = mock<Connection>()

        whenever(connectionSource.getConnection(database)).thenReturn(dataSource)
        whenever(dataSource.connection).thenReturn(connection)

        creditExtractor = CreditExtractor(database)

        mConn.createStatement().use {
            it.execute("TRUNCATE TABLE m_story_credit")
        }
    }

    @Test
    @DisplayName("should extract credits and insert them into the database")
    fun shouldExtractCreditsAndInsertThemIntoTheDatabase() {
        // Truncate gcd_story_credit
        mConn.createStatement().use {
            it.execute("TRUNCATE TABLE gcd_story_credit")
        }

        val resultSet = mock<ResultSet>()

        whenever(resultSet.getInt("id")).thenReturn(1)
        whenever(resultSet.getString("script")).thenReturn("Grant Morrison")
        whenever(resultSet.getString("pencils")).thenReturn("Frank Quitely; Val Semeiks;")
        whenever(resultSet.getString("inks")).thenReturn("Frank Quitely; Dan Green;")
        whenever(resultSet.getString("colors")).thenReturn("Chris Sotomayor; Alex Sinclair;")
        whenever(resultSet.getString("letters")).thenReturn("Richard Starkings;")
        whenever(resultSet.getString("editing")).thenReturn("Bob Schreck; Michael Wright;")

        val result = creditExtractor.extractAndInsert(resultSet)

        assertEquals(1, result)

        // verify against database
        mConn.createStatement().use {
            val res =
                it.executeQuery("SELECT * FROM m_story_credit WHERE story_id = 1 ORDER BY creator_id, credit_type_id")
            res.next()
            assertEquals(1, res.getInt("creator_id"))
            assertEquals(1, res.getInt("credit_type_id"))
            res.next()
            assertEquals(2, res.getInt("creator_id"))
            assertEquals(2, res.getInt("credit_type_id"))
            res.next()
            assertEquals(2, res.getInt("creator_id"))
            assertEquals(3, res.getInt("credit_type_id"))
            res.next()
            assertEquals(3, res.getInt("creator_id"))
            assertEquals(2, res.getInt("credit_type_id"))
            res.next()
            assertEquals(4, res.getInt("creator_id"))
            assertEquals(3, res.getInt("credit_type_id"))
            res.next()
            assertEquals(5, res.getInt("creator_id"))
            assertEquals(4, res.getInt("credit_type_id"))
            res.next()
            assertEquals(6, res.getInt("creator_id"))
            assertEquals(5, res.getInt("credit_type_id"))
            res.next()
            assertEquals(7, res.getInt("creator_id"))
            assertEquals(6, res.getInt("credit_type_id"))
            res.next()
            assertEquals(8, res.getInt("creator_id"))
            assertEquals(6, res.getInt("credit_type_id"))
        }
    }

    @Test
    @DisplayName("should throw any SQLExceptions and log error")
    fun shouldThrowAnySQLExceptionsAndLogError() {
        val resultSet = mock<ResultSet>()

        whenever(resultSet.getInt("id")).thenThrow(SQLException("test exception"))

        assertThrows<SQLException> { creditExtractor.extractAndInsert(resultSet) }
    }

    companion object {
        private lateinit var mConn: Connection

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            mConn = getTestDbConnection()
            TestDatabaseSetup.setup(dbState = DBState.STEP_TWO_COMPLETE)
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            TestDatabaseSetup.teardown()
            mConn.close()
        }
    }
}