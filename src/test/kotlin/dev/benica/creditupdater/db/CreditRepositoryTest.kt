package dev.benica.creditupdater.db

import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.dropAllTables
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getDbConnection
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.setup
import dev.benica.creditupdater.di.ConnectionSource
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
import java.sql.*

/** Credit Repository Test - tests the CreditRepository class */
class CreditRepositoryTest {

    /** Function to test: createOrUpdateStoryCredit */
    @Test
    @DisplayName("should insert new story credit if one does not exist")
    fun shouldInsertNewStoryCreditIfOneDoesNotExist() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE_CREDIT_REPO)
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
                assert(!resultSet.next())
            }
        }

        // Act
        creditRepository.createOrUpdateStoryCredit(extractedName, storyId, roleId)

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
                assert(resultSet.next())
            }
        }
    }

    @Test
    @DisplayName("createOrUpdateStoryCredit() should update existing story credit if one exists in gcd_story_credit")
    fun shouldUpdateExistingStoryCreditInGcdStoryCredit() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE_CREDIT_REPO)
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
                assert(resultSet.next())
            }
        }

        // Act
        creditRepository.createOrUpdateStoryCredit(extractedName, storyId, roleId)

        // Assert that the story credit was updated
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
                assert(resultSet.next())
            }
        }
    }

    @Test
    @DisplayName("createOrUpdateStoryCredit() should update existing story credit if one exists in m_story_credit")
    fun shouldUpdateExistingStoryCreditInMStoryCredit() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE_CREDIT_REPO)
        val extractedName = "Frank Quitely"
        val roleId = 2
        val storyId = 2

        // verify that the story credit exists in gcd_story_credit
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
                assert(resultSet.next())
            }
        }

        // Act
        creditRepository.createOrUpdateStoryCredit(extractedName, storyId, roleId)

        // Assert that the story credit was updated
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
                assert(resultSet.next())
            }
        }
    }

    // lookupGcndId
    @Test
    @DisplayName("should return the gcd_creator_name_detail id if found, null otherwise")
    fun shouldReturnTheGcdCreatorNameDetailIdIfFoundNullOtherwise() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE_CREDIT_REPO)
        val extractedName = "Grant Morrison"

        // Act
        val gcndId = creditRepository.lookupGcndId(extractedName)

        // Assert
        assert(gcndId == 1)
    }

    @Test
    @DisplayName("should return null if the gcd_creator_name_detail id is not found")
    fun shouldReturnNullIfTheGcdCreatorNameDetailIdIsNotFound() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE_CREDIT_REPO)
        val extractedName = "Bob"

        // Act
        val gcndId = creditRepository.lookupGcndId(extractedName)

        // Assert
        assert(gcndId == null)
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
            targetSchema = TEST_DATABASE_CREDIT_REPO,
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
            targetSchema = TEST_DATABASE_CREDIT_REPO,
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
        val creditRepository = CreditRepository(TEST_DATABASE_CREDIT_REPO, queryExecutor = queryExecutorMock)
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
        private const val TEST_DATABASE_CREDIT_REPO = "credit_updater_test_credit_repo"
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        internal fun setupAll() {
            setup(schema = TEST_DATABASE_CREDIT_REPO)
            conn = getDbConnection(TEST_DATABASE_CREDIT_REPO)
        }

        @AfterAll
        @JvmStatic
        internal fun teardownAll() {
            dropAllTables(conn, TEST_DATABASE_CREDIT_REPO)
            conn.close()
        }
    }
}