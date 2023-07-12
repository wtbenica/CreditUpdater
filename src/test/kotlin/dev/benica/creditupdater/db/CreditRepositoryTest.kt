package dev.benica.creditupdater.db

import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getDbConnection
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.setup
import dev.benica.creditupdater.di.ConnectionSource
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.sql.*

/** Credit Repository Test - tests the CreditRepository class */
class CreditRepositoryTest {

    /** Function to test: createOrUpdateStoryCredit */
    @Test
    @DisplayName("should insert new story credit if one does not exist")
    fun shouldInsertNewStoryCreditIfOneDoesNotExist() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Grant Morrison"
        val storyId = 1
        val roleId = 1

        // verify that the story credit does not exist
        conn.prepareStatement(
            """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertFalse(resultSet.next())
            }
        }

        // Act
        creditRepository.insertStoryCreditIfNotExists(extractedName, storyId, roleId)

        // Assert that the story credit was inserted
        conn.prepareStatement(
            """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertTrue(resultSet.next())
                assertEquals(1, resultSet.getInt("creator_id"))
                assertEquals(storyId, resultSet.getInt("story_id"))
                assertEquals(roleId, resultSet.getInt("credit_type_id"))
                assertFalse(resultSet.next())
            }
        }
    }

    @Test
    @DisplayName("should not insert new story credit if one exists in gcd_story_credit")
    fun shouldNotChangeIfStoryCreditExistsInGcdStoryCredit() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Frank Quitely"
        val roleId = 2
        val storyId = 1

        // verify that the story credit exists in gcd_story_credit
        conn.prepareStatement(
            """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertTrue(resultSet.next())
            }
        }

        // Act
        creditRepository.insertStoryCreditIfNotExists(extractedName, storyId, roleId)

        // Assert that the story credit still exists in gcd_story_credit unchanged
        conn.prepareStatement(
            """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertTrue(resultSet.next())
            }
        }

        // Assert that the story credit was not inserted into m_story_credit
        conn.prepareStatement(
            """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertFalse(resultSet.next())
            }
        }
    }

    @Test
    @DisplayName("should not insert new story credit if one exists in m_story_credit")
    fun shouldReturnExistingStoryCreditInMStoryCredit() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val id = 2
        val extractedName = "Frank Quitely"
        val storyId = 1
        val roleId = 1

        // insert a new story credit into m_story_credit
        conn.prepareStatement(
            """
                INSERT INTO m_story_credit (id, creator_id, story_id, credit_type_id)
                VALUES (
                    ?,
                    (SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?),
                    ?,
                    ?
                )
                """.trimIndent()
        ).use { statement ->
            statement.setInt(1, id)
            statement.setString(2, extractedName)
            statement.setInt(3, storyId)
            statement.setInt(4, roleId)

            statement.executeUpdate()
        }

        // verify that the story credit exists in m_story_credit
        conn.prepareStatement(
            """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertTrue(resultSet.next())
            }
        }

        // verify that the story credit dne in gcd_story_credit
        conn.prepareStatement(
            """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertFalse(resultSet.next())
            }
        }

        // Act
        creditRepository.insertStoryCreditIfNotExists(extractedName, storyId, roleId)

        // Assert that the story credit in m_story_credit is unchanged
        conn.prepareStatement(
            """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertTrue(resultSet.next())
            }
        }

        // Assert that the story credit was not inserted into gcd_story_credit
        conn.prepareStatement(
            """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, extractedName)
            statement.setInt(2, storyId)
            statement.setInt(3, roleId)

            statement.executeQuery().use { resultSet ->
                assertFalse(resultSet.next())
            }
        }
    }

    // lookupGcndId
    @Test
    @DisplayName("should return the gcd_creator_name_detail id if found, null otherwise")
    fun shouldReturnTheGcdCreatorNameDetailIdIfFoundNullOtherwise() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Grant Morrison"

        // Act
        val gcndId = creditRepository.lookupGcndId(extractedName)

        // Assert
        assertTrue(gcndId == 1)
    }

    @Test
    @DisplayName("should return null if the gcd_creator_name_detail id is not found")
    fun shouldReturnNullIfTheGcdCreatorNameDetailIdIsNotFound() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Bob"

        // Act
        val gcndId = creditRepository.lookupGcndId(extractedName)

        // Assert
        assertTrue(gcndId == null)
    }

    @Test
    @DisplayName("should throw any SQLExceptions | lookupGcndId")
    fun shouldThrowAnySQLExceptionsLookupGcndId() {
        // Mock queryExecutor
        val queryExecutorMock = mock<QueryExecutor>()
        whenever(
            queryExecutorMock.executePreparedStatement(any(), any(), any())
        ).thenAnswer { throw SQLException("Test Exception") }

        // Arrange
        val creditRepository = CreditRepository(
            targetSchema = TEST_DATABASE,
            queryExecutor = queryExecutorMock
        )
        val extractedName = "Grant Morrison"

        // Assert
        assertThrows<SQLException> { creditRepository.lookupGcndId(extractedName) }
    }

    // lookupStoryCreditId
    @Test
    @DisplayName("should throw any SQLExceptions | lookupStoryCreditId")
    fun shouldThrowAnySQLExceptionsLookupStoryCreditId() {
        // Mock queryExecutor
        val queryExecutorMock = mock<QueryExecutor>()
        whenever(
            queryExecutorMock.executePreparedStatement(any(), any(), any())
        ).thenAnswer { throw SQLException("Test Exception") }

        // Arrange
        val creditRepository = CreditRepository(
            targetSchema = TEST_DATABASE,
            queryExecutor = queryExecutorMock
        )

        val extractedName = 1
        val storyId = 1
        val roleId = 1

        // Assert
        assertThrows<SQLException> { creditRepository.lookupStoryCreditId(extractedName, storyId, roleId) }
    }

    @Test
    @DisplayName("should throw any SQLExceptions | lookupStoryCreditId, second")
    fun shouldThrowAnySQLExceptionsLookupStoryCreditIdSecond() {
        // Mock queryExecutor
        val queryExecutorMock = mock<QueryExecutor>()
        whenever(
            queryExecutorMock.executePreparedStatement(any(), any(), any())
        ).thenAnswer { }.thenAnswer { throw SQLException("Test Exception") }

        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)
        val extractedName = 1
        val storyId = 1
        val roleId = 1

        // Assert
        assertThrows<SQLException> { creditRepository.lookupStoryCreditId(extractedName, storyId, roleId) }
    }

    @Test
    @DisplayName("should call close() and conn.close() when used in try-with-resources")
    fun shouldCallCloseAndConnCloseWhenUsedInTryWithResources() {
        // Create the repository
        val repoMock = spy(CreditRepository("mock", null))
        val connectionSourceMock = mock<ConnectionSource>()
        val hikariDataSourceMock = mock<HikariDataSource>()
        val connectionMock = mock<Connection>()

        repoMock.connectionSource = connectionSourceMock
        repoMock.conn = connectionMock

        whenever(repoMock.connectionSource).thenReturn(connectionSourceMock)
        whenever(connectionSourceMock.getConnection(any())).thenReturn(hikariDataSourceMock)
        whenever(hikariDataSourceMock.connection).thenReturn(connectionMock)
        doCallRealMethod().whenever(repoMock).close()
        doNothing().whenever(connectionMock).close()

        // Use in try-with-resources
        repoMock.use { }

        // Verify that close() and conn.close() were called
        verify(repoMock, times(1)).close()
        verify(connectionMock, times(1)).close()
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        internal fun setupAll() {
            setup()
            conn = getDbConnection(TEST_DATABASE)
        }

        @AfterAll
        @JvmStatic
        internal fun teardownAll() {
            //dropAllTables(conn, TEST_DATABASE)
            conn.close()
        }
    }
}