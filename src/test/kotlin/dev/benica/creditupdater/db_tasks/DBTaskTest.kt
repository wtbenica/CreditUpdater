package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.extractor.CharacterExtractor
import dev.benica.creditupdater.extractor.CreditExtractor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.contains
import org.mockito.kotlin.*
import java.io.File
import java.sql.Connection

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DBTaskTest {
    @Test
    @Order(1)
    @DisplayName("extractCharactersAndAppearances")
    fun extractCharactersAndAppearances() {
        TestDatabaseSetup.setup(
            DBState.INIT_STEP_1_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        Thread.sleep(6000)

        DBTask(TEST_DATABASE).extractCharactersAndAppearances(
            schema = TEST_DATABASE,
            initial = true,
            startingId = 0
        )

        // give db ops a chance to complete
        Thread.sleep(2000)

        // verify that characters have been added to m_character
        verifyCharactersWereExtracted(conn)

        verifyCharacterAppearancesWereExtracted(conn)
    }

    @Test
    @Order(1)
    @DisplayName("extractCharactersAndAppearances when startingId is null/default")
    fun extractCharactersAndAppearancesWithDefaultStartingId() {
        TestDatabaseSetup.setup(
            dbState = DBState.INIT_STEP_1_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        Thread.sleep(2000)

        DBTask(TEST_DATABASE).extractCharactersAndAppearances(
            schema = TEST_DATABASE,
            initial = true
        )

        // give db ops a chance to complete
        Thread.sleep(2000)

        // verify that characters have been added to m_character
        verifyCharactersWereExtracted(conn)

        verifyCharacterAppearancesWereExtracted(conn)
    }

    @Test
    @Order(3)
    @DisplayName("extractCredits")
    fun extractCredits() {
        TestDatabaseSetup.setup(
            dbState = DBState.INIT_STEP_2_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        Thread.sleep(2000)

        DBTask(TEST_DATABASE).extractCredits(
            schema = TEST_DATABASE,
            initial = true,
            startingId = 0
        )

        // give db ops a chance to complete
        Thread.sleep(2000)

        // verify that credits have been added to m_credit
        verifyCreditsWereExtracted(conn)
    }

    @Test
    @Order(4)
    @DisplayName("extractCredits when startingId is null/default")
    fun extractCreditsWithDefaultStartingId() {
        TestDatabaseSetup.setup(
            DBState.INIT_STEP_2_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        Thread.sleep(2000)

        DBTask(TEST_DATABASE).extractCredits(
            schema = TEST_DATABASE,
            initial = true
        )

        // give db ops a chance to complete
        Thread.sleep(2000)

        // verify that credits have been added to m_credit
        verifyCreditsWereExtracted(conn)
    }

    @Test
    @Order(6)
    @DisplayName("extractCharactersAndAppearances where progress.json file dne")
    fun extractCharactersAndAppearancesWhereProgressFileDne() {
        TestDatabaseSetup.setup(
            dbState = DBState.INIT_STEP_1_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        // rename progress.json file if exists
        val progressFile = File("progress.json")
        if (progressFile.exists()) {
            progressFile.renameTo(File("progress.json.bak"))
        }

        // Arrange
        val mockDBTask = spy(DBTask(TEST_DATABASE))

        // Act
        mockDBTask.extractCharactersAndAppearances(
            schema = TEST_DATABASE,
            initial = true,
            startingId = null
        )

        // Assert
        verify(mockDBTask).extractAndInsertItems(
            selectItemsQuery = contains("WHERE g.id > ${Credentials.CHARACTER_STORY_START_ID}"),
            extractor = any<CharacterExtractor>(),
            batchSize = any()
        )

        // rename progress.json.bak file back to progress.json
        if (progressFile.exists()) {
            progressFile.renameTo(File("progress.json"))
        }
    }

    @Test
    @Order(7)
    @DisplayName("extractCredits where progress.json file dne")
    fun extractCreditsWhereProgressFileDne() {
        TestDatabaseSetup.setup(
            dbState = DBState.INIT_STEP_2_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        // rename progress.json file if exists
        val progressFile = File("progress.json")
        if (progressFile.exists()) {
            progressFile.renameTo(File("progress.json.bak"))
        }

        // Arrange
        val mockDBTask = spy(DBTask(TEST_DATABASE))

        // Act
        mockDBTask.extractCredits(
            schema = TEST_DATABASE,
            initial = true,
            startingId = null
        )

        // Assert
        verify(mockDBTask).extractAndInsertItems(
            selectItemsQuery = contains("WHERE g.id > ${Credentials.CREDITS_STORY_START_ID}"),
            extractor = any<CreditExtractor>(),
            batchSize = any()
        )

        // rename progress.json.bak file back to progress.json
        if (progressFile.exists()) {
            progressFile.renameTo(File("progress.json"))
        }
    }

    companion object {
        internal fun verifyCreditsWereExtracted(connection: Connection) {
            connection.createStatement().use { stmt ->
                stmt.executeQuery(
                    """SELECT msc.creator_id, msc.story_id, msc.credit_type_id
                        |FROM m_story_credit msc
                    """.trimMargin()
                ).use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("creator_id"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertEquals(1, rs.getInt("credit_type_id"))

                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("creator_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals(1, rs.getInt("credit_type_id"))

                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("creator_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals(2, rs.getInt("credit_type_id"))

                    assertFalse(rs.next())
                }
            }
        }

        internal fun verifyCharacterAppearancesWereExtracted(connection: Connection) {
            connection.createStatement().use { stmt ->
                stmt.executeQuery(
                    """SELECT mc.name, mca.details, mca.story_id, mca.notes, mca.membership 
                    |FROM m_character_appearance mca 
                    |JOIN m_character mc on mc.id = mca.character_id 
                    |ORDER BY mc.name""".trimMargin()
                ).use { rs ->
                    assertTrue(rs.next())
                    assertEquals("Danny the Street", rs.getString("name"))
                    assertEquals("", rs.getString("details"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertNull(rs.getString("notes"))
                    assertNull(rs.getString("membership"))

                    assertTrue(rs.next())
                    assertEquals("Doom Patrol", rs.getString("name"))
                    assertEquals("", rs.getString("details"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertNull(rs.getString("notes"))
                    assertEquals(
                        "Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]",
                        rs.getString("membership")
                    )

                    assertTrue(rs.next())
                    assertEquals("Flex Mentallo", rs.getString("name"))
                    assertEquals("cameo, unnamed", rs.getString("details"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertNull(rs.getString("notes"))
                    assertNull(rs.getString("membership"))

                    assertTrue(rs.next())
                    assertEquals("Willoughby Kipling", rs.getString("name"))
                    assertEquals("", rs.getString("details"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertNull(rs.getString("notes"))
                    assertNull(rs.getString("membership"))

                    assertTrue(rs.next())
                    assertEquals("X-Men", rs.getString("name"))
                    assertEquals("", rs.getString("details"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertNull(rs.getString("notes"))
                    assertEquals(
                        "Beast [Hank McCoy]; Cyclops [Scott Summers]; White Queen [Emma Frost]; Marvel Girl [Jean Grey]; Professor X [Charles Xavier]; Wolverine [Logan]",
                        rs.getString("membership")
                    )

                    assertFalse(rs.next())
                }
            }
        }

        internal fun verifyCharactersWereExtracted(connection: Connection) {
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SELECT name, alter_ego, publisher_id FROM m_character ORDER BY name").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("Danny the Street", rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(2, rs.getInt("publisher_id"))

                    assertTrue(rs.next())
                    assertEquals("Doom Patrol", rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(2, rs.getInt("publisher_id"))

                    assertTrue(rs.next())
                    assertEquals("Flex Mentallo", rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(2, rs.getInt("publisher_id"))

                    assertTrue(rs.next())
                    assertEquals("Willoughby Kipling", rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(2, rs.getInt("publisher_id"))

                    assertTrue(rs.next())
                    assertEquals("X-Men", rs.getString("name"))
                    assertNull(rs.getString("alter_ego"))
                    assertEquals(1, rs.getInt("publisher_id"))

                    assertFalse(rs.next())
                }
            }
        }

        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            conn = getTestDbConnection()
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            conn.close()
        }
    }
}