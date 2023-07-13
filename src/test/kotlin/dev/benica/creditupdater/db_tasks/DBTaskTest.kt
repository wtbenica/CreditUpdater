package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import java.sql.Connection

class DBTaskTest {
    @Test
    @DisplayName("extractCharactersAndAppearances")
    fun extractCharactersAndAppearances() {
        TestDatabaseSetup.setup(DBState.STEP_ONE_COMPLETE)

        DBTask(TEST_DATABASE).extractCharactersAndAppearances(schema = TEST_DATABASE, initial = true)

        // give db ops a chance to complete
        Thread.sleep(3000)

        // verify that characters have been added to m_character
        verifyCharactersWereExtracted(conn)

        verifyCharacterAppearancesWereExtracted(conn)
    }

    companion object {
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