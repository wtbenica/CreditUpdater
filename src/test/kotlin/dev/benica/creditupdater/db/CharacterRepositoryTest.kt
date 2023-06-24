package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.creditupdater.models.Appearance
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class CharacterRepositoryTest {

    // upsertCharacter
    @Test
    @DisplayName("should insert a new character if it does not exist in the database")
    fun shouldInsertNewCharacter() {
        val tableName = "m_character"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM $tableName")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val id = repo.upsertCharacter("Test Character", null, 1)
            assertNotNull(id)
            assertEquals(1, id)

            // Check that the character was inserted
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM $tableName").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("Test Character", rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(1, rs.getInt("publisher_id"))
                    assertFalse(rs.next())
                }
            }
        }
    }

    @Test
    @DisplayName("should return the ID of an existing character")
    fun shouldReturnExistingCharacter() {
        val tableName = "m_character"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("INSERT INTO $tableName (name, alter_ego, publisher_id) VALUES ('Test Character', NULL, 1)")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val id = repo.upsertCharacter("Test Character", null, 1, false)
            assertNotNull(id)
            assertEquals(1, id)
        }
    }

    @Test
    @DisplayName("should truncate alter ego to 255 characters")
    fun shouldTruncateAlterEgo() {
        val tableName = "m_character"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM $tableName")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val id = repo.upsertCharacter("Test Character", "a".repeat(300), 1)
            assertNotNull(id)
            assertEquals(1, id)

            // Check that the character was inserted
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM $tableName").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("Test Character", rs.getString("name"))
                    assertEquals("a".repeat(255), rs.getString("alter_ego"))
                    assertEquals(1, rs.getInt("publisher_id"))
                    assertFalse(rs.next())
                }
            }
        }
    }

    @Test
    @DisplayName("upsert name > 255 characters")
    fun shouldTruncateName() {
        val tableName = "m_character"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM $tableName")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val id = repo.upsertCharacter("a".repeat(300), null, 1)
            assertNotNull(id)
            assertEquals(1, id)

            // Check that the character was inserted
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM $tableName").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("a".repeat(255), rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(1, rs.getInt("publisher_id"))
                    assertFalse(rs.next())
                }
            }
        }
    }

    // lookupCharacter
    @Test
    @DisplayName("should return null if the character does not exist")
    fun lookupCharacterShouldReturnNull() {
        val tableName = "m_character"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM $tableName")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val characterId = repo.lookupCharacter("Test Character", null, 1)
            assertNull(characterId)
        }
    }

    @Test
    @DisplayName("should return the ID of an existing character")
    fun lookupCharacterShouldReturnId() {
        val tableName = "m_character"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("INSERT INTO $tableName (name, alter_ego, publisher_id) VALUES ('Test Character', NULL, 1)")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val characterId = repo.lookupCharacter("Test Character", null, 1)
            assertNotNull(characterId)
            assertEquals(1, characterId)
        }
    }

    @Test
    @DisplayName("should throw an exception if the database throws an exception")
    fun shouldThrowExceptionIfDatabaseThrowsException() {
        // Create mock objects
        val queryExecutorMock = mock<QueryExecutor>()

        // Create the repository
        val repo = CharacterRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)

        whenever(queryExecutorMock.executePreparedStatementBatch(any(), any(), any())).thenAnswer {
            throw SQLException()
        }

        assertThrows<SQLException> {
            repo.lookupCharacter("Test Character", null, 1)
        }
    }

    // insertCharacterAppearances
    @Test
    @DisplayName("should insert a set of new appearances with appearances")
    fun shouldInsertASetOfNewAppearancesWithAppearances() {
        val tableName = "m_character_appearance"

        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM $tableName")
            }

            val repo = CharacterRepository(TEST_DATABASE)
            val appearances = setOf(
                Appearance(1, 1, "appearanceInfo1", "notes1", "members1"),
                Appearance(1, 2, "appearanceInfo2", "notes2", "members2"),
                Appearance(1, 3, "appearanceInfo3", "notes3", "members3"),
                Appearance(2, 1, "appearanceInfo4", "notes4", "members4"),
                Appearance(2, 2, "appearanceInfo5", "notes5", "members5"),
                Appearance(2, 3, "appearanceInfo6", "notes6", "members6")
            )

            repo.insertCharacterAppearances(appearances)

            // Check that the appearances were inserted
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM $tableName").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("appearanceInfo1", rs.getString("details"))
                    assertEquals(1, rs.getInt("character_id"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertEquals("notes1", rs.getString("notes"))
                    assertEquals("members1", rs.getString("membership"))
                    assertEquals(0, rs.getInt("issue_id"))
                    assertEquals(0, rs.getInt("series_id"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("appearanceInfo2", rs.getString("details"))
                    assertEquals(2, rs.getInt("character_id"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertEquals("notes2", rs.getString("notes"))
                    assertEquals("members2", rs.getString("membership"))
                    assertEquals(0, rs.getInt("issue_id"))
                    assertEquals(0, rs.getInt("series_id"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("appearanceInfo3", rs.getString("details"))
                    assertEquals(3, rs.getInt("character_id"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertEquals("notes3", rs.getString("notes"))
                    assertEquals("members3", rs.getString("membership"))
                    assertEquals(0, rs.getInt("issue_id"))
                    assertEquals(0, rs.getInt("series_id"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("appearanceInfo4", rs.getString("details"))
                    assertEquals(1, rs.getInt("character_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("notes4", rs.getString("notes"))
                    assertEquals("members4", rs.getString("membership"))
                    assertEquals(0, rs.getInt("issue_id"))
                    assertEquals(0, rs.getInt("series_id"))
                    assertTrue(rs.next())
                    assertEquals(5, rs.getInt("id"))
                    assertEquals("appearanceInfo5", rs.getString("details"))
                    assertEquals(2, rs.getInt("character_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("notes5", rs.getString("notes"))
                    assertEquals("members5", rs.getString("membership"))
                    assertEquals(0, rs.getInt("issue_id"))
                    assertEquals(0, rs.getInt("series_id"))
                    assertTrue(rs.next())
                    assertEquals(6, rs.getInt("id"))
                    assertEquals("appearanceInfo6", rs.getString("details"))
                    assertEquals(3, rs.getInt("character_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("notes6", rs.getString("notes"))
                    assertEquals("members6", rs.getString("membership"))
                    assertEquals(0, rs.getInt("issue_id"))
                    assertEquals(0, rs.getInt("series_id"))
                    assertFalse(rs.next())
                }
            }
        }
    }

    @Test
    @DisplayName("should throw an error and log error message when insertCharacterAppearances fails")
    fun shouldThrowAnErrorAndLogErrorMessageWhenInsertCharacterAppearancesFails() {
        // Create mock objects
        val queryExecutorMock = mock<QueryExecutor>()

        // Create the repository
        val repo = CharacterRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)

        whenever(
            queryExecutorMock.executePreparedStatementBatch(any(), any(), any())
        ).thenAnswer { throw SQLException("Test Exception") }

        val appearances = setOf(
            Appearance(1, 1, null, null, null),
        )

        assertThrows<SQLException> { repo.insertCharacterAppearances(appearances) }
    }

    // insertCharacter
    @Test
    @DisplayName("should throw SQLException when insertCharacter fails")
    fun shouldThrowSQLExceptionWhenInsertCharacterFails() {
        // Create mock objects
        val queryExecutorMock = mock<QueryExecutor>()

        // Create the repository
        val repo = CharacterRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)

        whenever(queryExecutorMock.executePreparedStatementBatch(any(), any(), any())).thenAnswer {
            val action = it.getArgument<(PreparedStatement) -> Unit>(2)
            val stmtMock = mock<PreparedStatement>()
            whenever(stmtMock.executeUpdate()).thenAnswer { throw SQLException("Test Exception") }
            action(stmtMock)
        }

        assertThrows<SQLException> { repo.insertCharacter("Test Character", null, 1) }
    }

    @Test
    @DisplayName("insert character with nonexistent publisher id should throw SQLException")
    fun insertCharacterWithNonexistentPublisherIdShouldThrowSQLException() {
        // Create the repository
        val repo = CharacterRepository(TEST_DATABASE)

        assertThrows<SQLException> { repo.insertCharacter("Test Character", null, 3) }
    }

    // Setup and teardown
    @BeforeEach
    fun initEach() {
        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("TRUNCATE TABLE m_character")
            }
        }
    }

    @AfterEach
    fun tearDown() {
        getDbConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("TRUNCATE TABLE m_character")
            }
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            getDbConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    // create publishers table
                    stmt.execute(
                        """CREATE TABLE IF NOT EXISTS gcd_publisher
                        (
                        id           INTEGER PRIMARY KEY AUTO_INCREMENT,
                        name         VARCHAR(255) NOT NULL
                        )""".trimMargin()
                    )

                    stmt.execute(
                        """INSERT INTO gcd_publisher (id, name)
                            |VALUES (1, 'Test Publisher')""".trimMargin()
                    )
                    stmt.execute(
                        """INSERT INTO gcd_publisher (id, name)
                            |VALUES (2, 'Test Publisher 2')""".trimMargin()
                    )

                    stmt.execute(
                        """CREATE TABLE IF NOT EXISTS m_character
                        (
                        id           INTEGER PRIMARY KEY AUTO_INCREMENT,
                        name         VARCHAR(255) NOT NULL,
                        alter_ego    VARCHAR(255),
                        publisher_id INTEGER NOT NULL REFERENCES gcd_publisher (id)
                        )""".trimMargin()
                    )

                    stmt.execute(
                        """CREATE TABLE IF NOT EXISTS m_character_appearance
                        (
                        id           INTEGER PRIMARY KEY AUTO_INCREMENT,
                        details      VARCHAR(255),
                        character_id    INTEGER NOT NULL,
                        story_id     INTEGER NOT NULL,
                        notes        VARCHAR(255),
                        membership   LONGTEXT,
                        issue_id     INTEGER NOT NULL,
                        series_id    INTEGER NOT NULL
                        )""".trimMargin()
                    )
                }
            }
        }

        @AfterAll
        @JvmStatic
        fun breakDown() {
            QueryExecutorTest.dropAllTables()
        }

        private fun getDbConnection(): Connection = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE",
            USERNAME_INITIALIZER,
            PASSWORD_INITIALIZER
        )
    }
}