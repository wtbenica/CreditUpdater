package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.CharacterRepositoryTest
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.extractor.CharacterExtractor.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class CharacterExtractorTest {
    private lateinit var connection: Connection
    private lateinit var characterExtractor: CharacterExtractor

    @BeforeEach
    fun setUp() {
        connection = mock<Connection>()

        characterExtractor = CharacterExtractor(TEST_DATABASE)

        conn.createStatement().use {
            it.execute("TRUNCATE TABLE m_character_appearance")
            it.execute("TRUNCATE TABLE m_character")
        }
    }

    @Test
    @DisplayName("should extract characters and insert them into the database")
    fun shouldExtractCharactersAndInsertThemIntoTheDatabase() {
        val resultSet = mock<ResultSet>()
        whenever(resultSet.getInt("id")).thenReturn(1)
        whenever(resultSet.getString("characters")).thenReturn("Batman [Bruce Wayne] (cameo); Robin; Joker")
        whenever(resultSet.getInt("publisher_id")).thenReturn(2)

        val result = characterExtractor.extractAndInsert(resultSet, conn)

        assertEquals(1, result)

        // verify against database
        conn.createStatement().use {
            val rs = it.executeQuery("SELECT * FROM m_character_appearance WHERE story_id = 1")
            rs.next()
            assertEquals(1, rs.getInt("story_id"))
            assertEquals(1, rs.getInt("character_id"))
            assertEquals("cameo", rs.getString("details"))
            assertNull(rs.getString("notes"))
            assertNull(rs.getString("membership"))
            rs.next()
            assertEquals(1, rs.getInt("story_id"))
            assertEquals(2, rs.getInt("character_id"))
            assertEquals("", rs.getString("details"))
            assertNull(rs.getString("notes"))
            assertNull(rs.getString("membership"))
            rs.next()
            assertEquals(1, rs.getInt("story_id"))
            assertEquals(3, rs.getInt("character_id"))
            assertEquals("", rs.getString("details"))
            assertNull(rs.getString("notes"))
            assertNull(rs.getString("membership"))

            val rs2 = it.executeQuery("SELECT * FROM m_character")
            rs2.next()
            assertEquals(1, rs2.getInt("id"))
            assertEquals("Batman", rs2.getString("name"))
            assertEquals("Bruce Wayne", rs2.getString("alter_ego"))
            assertEquals(2, rs2.getInt("publisher_id"))
            rs2.next()
            assertEquals(2, rs2.getInt("id"))
            assertEquals("Robin", rs2.getString("name"))
            assertNull(rs2.getString("alter_ego"))
            assertEquals(2, rs2.getInt("publisher_id"))
            rs2.next()
            assertEquals(3, rs2.getInt("id"))
            assertEquals("Joker", rs2.getString("name"))
            assertNull(rs2.getString("alter_ego"))
            assertEquals(2, rs2.getInt("publisher_id"))
        }
    }

    @Test
    @DisplayName("should extract characters and team and insert them and their appearances into the database")
    fun shouldExtractCharactersAndTeamAndInsertThemAndTheirAppearancesIntoTheDatabase() {
        val resultSet = mock<ResultSet>()
        whenever(resultSet.getInt("id")).thenReturn(1)
        whenever(resultSet.getString("characters")).thenReturn("Batman [Bruce Wayne]; Justice League of America [Superman [Clark Kent]; Green Lantern [Hal Jordan]; The Flash [Barry Allen]; Aquaman [Arthur Curry]; Martian Manhunter [J'onn J'onzz]];")
        whenever(resultSet.getInt("publisher_id")).thenReturn(2)

        val result = characterExtractor.extractAndInsert(resultSet, conn)

        assertEquals(1, result)

        // verify against database
        conn.createStatement().use {
            val rs = it.executeQuery("SELECT * FROM m_character_appearance WHERE story_id = 1")
            rs.next()
            assertEquals(1, rs.getInt("story_id"))
            assertEquals(1, rs.getInt("character_id"))
            assertEquals("", rs.getString("details"))
            assertNull(rs.getString("notes"))
            assertNull(rs.getString("membership"))
            rs.next()
            assertEquals(1, rs.getInt("story_id"))
            assertEquals(2, rs.getInt("character_id"))
            assertEquals("", rs.getString("details"))
            assertNull(rs.getString("notes"))
            assertEquals(
                "Superman [Clark Kent]; Green Lantern [Hal Jordan]; The Flash [Barry Allen]; Aquaman [Arthur Curry]; Martian Manhunter [J'onn J'onzz]",
                rs.getString("membership")
            )
            assertFalse(rs.next())

            val rs2 = it.executeQuery("SELECT * FROM m_character")
            rs2.next()
            assertEquals(1, rs2.getInt("id"))
            assertEquals("Batman", rs2.getString("name"))
            assertEquals("Bruce Wayne", rs2.getString("alter_ego"))
            assertEquals(2, rs2.getInt("publisher_id"))
            rs2.next()
            assertEquals(2, rs2.getInt("id"))
            assertEquals("Justice League of America", rs2.getString("name"))
            assertNull(rs2.getString("alter_ego"))
            assertEquals(2, rs2.getInt("publisher_id"))
            assertFalse(rs2.next())
        }
    }

    @Test
    @DisplayName("should throw and SQLExceptions and log error")
    fun shouldThrowAndSQLExceptionsAndLogError() {
        val resultSet = mock<ResultSet>()

        whenever(resultSet.getInt("id")).thenThrow(SQLException("test exception"))

        assertThrows<SQLException> { characterExtractor.extractAndInsert(resultSet, conn) }
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setUpDb() {
            conn = getTestDbConnection()
            CharacterRepositoryTest.setUpDatabase(conn)
        }

        @AfterAll
        @JvmStatic
        fun tearDownDb() {
            conn.close()
        }
    }
}