package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE_UPDATE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getDbConnection
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.db_tasks.DBMigrator.Companion.addTablesNew
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.sql.Connection

class DBMigratorTest {
    private val queryExecutor = QueryExecutor()

    @Test
    fun shouldSetupTestDatabaseUpdateWithNewRecords() {
        TestDatabaseSetup.setup(dbState = DBState.INITIAL_PLUS_NEW_RECORDS, schema = TEST_DATABASE_UPDATE)
    }

    // addTablesNew()
    @Test
    @DisplayName("should add tables and constraints as appropriate")
    fun testAddTablesNew() {
        TestDatabaseSetup.setup(dbState = DBState.INITIAL_PLUS_NEW_RECORDS, schema = TEST_DATABASE_UPDATE)
        TestDatabaseSetup.setup(dbState = DBState.PREPARED, schema = TEST_DATABASE)

        Thread.sleep(2000)

        addTablesNew(
            queryExecutor = queryExecutor,
            conn = conn,
            sourceSchema = TEST_DATABASE_UPDATE,
            targetSchema = TEST_DATABASE
        )

        Thread.sleep(2000)

        // verify TEST_DATABASE_UPDATE:
        // - has issue_id and series_id columns added to gcd_story_credit table
        getDbConnection(TEST_DATABASE_UPDATE).use { conn ->
            val query = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = '$TEST_DATABASE_UPDATE'
                AND table_name = 'gcd_story_credit'
                AND column_name IN ('issue_id', 'series_id')
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertTrue(rs.next())
                assertFalse(rs.next())
            }

            // - has m_character, m_story_credit, and m_character_appearance tables added
            val tables = listOf(
                "m_character",
                "m_story_credit",
                "m_character_appearance"
            )

            tables.forEach { table ->
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE_UPDATE.$table", conn) { rs ->
                    assertFalse(rs.next())
                }
            }

            // - has good_publishers, good_indicia_publishers, good_series, good_issue, good_story, and good_story_credits tables added
            val goodTables = listOf(
                "good_publishers",
                "good_indicia_publishers",
                "good_series",
                "good_issue",
                "good_story",
                "good_story_credit"
            )

            queryExecutor.executeQueryAndDo(
                """SELECT COUNT(*) 
                    |FROM information_schema.tables
                    |WHERE table_schema = '$TEST_DATABASE_UPDATE'
                    |AND table_name IN (${goodTables.joinToString(",") { "'$it'" }})
                """.trimMargin(), conn
            ) { rs ->
                assertTrue(rs.next())
                assertEquals(goodTables.size, rs.getInt(1))
            }

            verifyGoodTables()
            verifyMigrateTables()

            // - has gcd_story_credit, m_character_appearance, and m_character tables with constraints and indexes added
        }
    }

    /** verifies that good tables contain the expected records */
    private fun verifyGoodTables() {
        fun verifyGoodPublishers() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_publishers
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Marvel", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals("DC", rs.getString("name"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(5, rs.getInt("id"))
                assertEquals("GOOD old pub, series >= 1900", rs.getString("name"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(6, rs.getInt("id"))
                assertEquals("GOOD New publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        fun verifyGoodSeries() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_series
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(1, it.getInt("id"))
                assertEquals("Doom Patrol", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(2, it.getInt("id"))
                assertEquals("New X-Men", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(8, it.getInt("id"))
                assertEquals("GOOD >= 1900, old pub", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(5, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(9, it.getInt("id"))
                assertEquals("GOOD bad first_issue_id", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(10, it.getInt("id"))
                assertEquals("GOOD bad last_issue_id", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(11, it.getInt("id"))
                assertEquals("GOOD New series existing publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(12, it.getInt("id"))
                assertEquals("GOOD New series new publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(6, it.getInt("publisher_id"))
                assertFalse(it.next())
            }
        }

        fun verifyGoodIssue() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_issue
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(35, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals(114, rs.getInt("number"))
                assertEquals(2, rs.getInt("series_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals(11, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(10, rs.getInt("id"))
                assertEquals(12, rs.getInt("number"))
                assertEquals(11, rs.getInt("series_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        fun verifyGoodIndiciaPublishers() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_indicia_publishers
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(1, it.getInt("id"))
                assertEquals("Marvel Comics", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("parent_id"))
                assertTrue(it.next())
                assertEquals(2, it.getInt("id"))
                assertEquals("DC Comics", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("parent_id"))
                assertTrue(it.next())
                assertEquals(5, it.getInt("id"))
                assertEquals("GOOD existing publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("parent_id"))
                assertTrue(it.next())
                assertEquals(6, it.getInt("id"))
                assertEquals("GOOD new publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(6, it.getInt("parent_id"))
                assertFalse(it.next())
            }
        }

        fun verifyGoodStory() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_story
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Crawling from the Wreckage", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals("E for Extinction", rs.getString("title"))
                assertEquals(2, rs.getInt("issue_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(8, rs.getInt("id"))
                assertEquals("GOOD issue 1", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals("GOOD issue 9", rs.getString("title"))
                assertEquals(9, rs.getInt("issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        fun verifyGoodStoryCredits() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_story_credit
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(1, rs.getInt("story_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals(2, rs.getInt("story_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(3, rs.getInt("id"))
                assertEquals(2, rs.getInt("story_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(4, rs.getInt("id"))
                assertEquals(2, rs.getInt("story_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(5, rs.getInt("id"))
                assertEquals(2, rs.getInt("story_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(11, rs.getInt("id"))
                assertEquals(8, rs.getInt("story_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(12, rs.getInt("id"))
                assertEquals(9, rs.getInt("story_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        verifyGoodPublishers()
        verifyGoodSeries()
        verifyGoodIssue()
        verifyGoodIndiciaPublishers()
        verifyGoodStory()
        verifyGoodStoryCredits()
    }

    /** verifies that migrate tables contain the expected records */
    private fun verifyMigrateTables() {
        fun verifyMigratePublishers() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.migrate_publishers
                ORDER BY id
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Marvel", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(5, rs.getInt("id"))
                assertEquals("GOOD old pub, series >= 1900", rs.getString("name"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(6, rs.getInt("id"))
                assertEquals("GOOD New publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        fun verifyMigrateSeries() {
            val query = """
                    SELECT * 
                    FROM $TEST_DATABASE_UPDATE.migrate_series
                    ORDER BY id
                """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(1, it.getInt("id"))
                assertEquals("Doom Patrol", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(8, it.getInt("id"))
                assertEquals("GOOD >= 1900, old pub", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(5, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(9, it.getInt("id"))
                assertEquals("GOOD bad first_issue_id", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(10, it.getInt("id"))
                assertEquals("GOOD bad last_issue_id", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(11, it.getInt("id"))
                assertEquals("GOOD New series existing publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(12, it.getInt("id"))
                assertEquals("GOOD New series new publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(6, it.getInt("publisher_id"))
                assertFalse(it.next())
            }
        }

        fun verifyMigrateIssue() {
            val query = """
                    SELECT * 
                    FROM $TEST_DATABASE_UPDATE.migrate_issues
                    ORDER BY id
                """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(35, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals(11, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(10, rs.getInt("id"))
                assertEquals(12, rs.getInt("number"))
                assertEquals(11, rs.getInt("series_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        fun verifyMigrateIndiciaPublishers() {
            val query = """
                    SELECT * 
                    FROM $TEST_DATABASE_UPDATE.migrate_indicia_publishers
                    ORDER BY id
                """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(1, it.getInt("id"))
                assertEquals("Marvel Comics", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("parent_id"))
                assertTrue(it.next())
                assertEquals(5, it.getInt("id"))
                assertEquals("GOOD existing publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(1, it.getInt("parent_id"))
                assertTrue(it.next())
                assertEquals(6, it.getInt("id"))
                assertEquals("GOOD new publisher", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(6, it.getInt("parent_id"))
                assertFalse(it.next())

            }
        }

        fun verifyMigrateStory() {
            val query = """
                    SELECT * 
                    FROM $TEST_DATABASE_UPDATE.migrate_stories
                    ORDER BY id
                """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Crawling from the Wreckage", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(8, rs.getInt("id"))
                assertEquals("GOOD issue 1", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals("GOOD issue 9", rs.getString("title"))
                assertEquals(9, rs.getInt("issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        fun verifyMigrateStoryCredits() {
            val query = """
                    SELECT * 
                    FROM $TEST_DATABASE_UPDATE.migrate_story_credits
                    ORDER BY id
                """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(1, rs.getInt("story_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(11, rs.getInt("id"))
                assertEquals(8, rs.getInt("story_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(12, rs.getInt("id"))
                assertEquals(9, rs.getInt("story_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        verifyMigratePublishers()
        verifyMigrateSeries()
        verifyMigrateIssue()
        verifyMigrateIndiciaPublishers()
        verifyMigrateStory()
        verifyMigrateStoryCredits()
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            conn = getTestDbConnection()
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            conn.close()
        }
    }
}