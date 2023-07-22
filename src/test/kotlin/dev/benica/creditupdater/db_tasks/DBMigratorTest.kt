package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE_UPDATE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getDbConnection
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.db_tasks.DBMigrator.Companion.addIssueSeriesToCreditsNew
import dev.benica.creditupdater.db_tasks.DBMigrator.Companion.addTablesNew
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.sql.Connection

class DBMigratorTest {
    private val queryExecutor = QueryExecutor()

    // addTablesNew()
    @Test
    @DisplayName("should add tables and constraints as appropriate")
    fun testAddTablesNew() {
        TestDatabaseSetup.setup(dbState = DBState.INITIALIZED, schema = TEST_DATABASE, sourceSchema = null)
        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_INITIAL,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )

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
        }
    }

    // addIssueSeriesToCreditsNew
    @Test
    @DisplayName("should add issue_id and series_id to gcd_story_credit, m_story_credit, and m_character_appearance tables")
    fun shouldAddIssueAndSeriesIdColumns() {
        TestDatabaseSetup.setup(dbState = DBState.INITIALIZED, schema = TEST_DATABASE, sourceSchema = null)

        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_STEP_3_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )


        addIssueSeriesToCreditsNew(
            queryExecutor = queryExecutor,
            conn = conn,
            targetSchema = TEST_DATABASE_UPDATE
        )

        verifyIssueAndSeriesColumnsExist()
        verifyIssueAndSeriesColumnsValues()
    }

    @Test
    @DisplayName("should migrate records from source to target schema")
    fun shouldMigrateRecords() {
        TestDatabaseSetup.setup(dbState = DBState.INITIALIZED, schema = TEST_DATABASE, sourceSchema = null)

        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_STEP_4_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )

        DBMigrator.migrateRecords(
            queryExecutor = queryExecutor,
            conn = conn,
            sourceSchema = TEST_DATABASE_UPDATE,
            targetSchema = TEST_DATABASE
        )

        verifyGoodTables()
        verifyMigrateTables()
        verifyTargetTablesAreUpdated()
    }

    private fun verifyTargetTablesAreUpdated() {
        fun verifyPublishersHaveBeenMigrated() {
            // select all publishers from sourceSchema.migrate_publishers and verify that they are in targetSchema.publishers
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
                assertEquals(6, rs.getInt("id"))
                assertEquals("GOOD New publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }

            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_publisher", conn) { rs ->
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

        // series
        fun verifySeriesHaveBeenMigrated() {
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

            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_series", conn) {
                assertTrue(it.next())
                assertEquals(1, it.getInt("id"))
                assertEquals("Doom Patrol", it.getString("name"))
                assertEquals("2023-06-01 19:56:37", it.getString("modified"))
                assertEquals(2, it.getInt("publisher_id"))
                assertTrue(it.next())
                assertEquals(2, it.getInt("id"))
                assertEquals("New X-Men", it.getString("name"))
                assertEquals("2004-06-01 19:56:37", it.getString("modified"))
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

        // gcd_indicia_publisher
        fun verifyIndiciaPublishersHaveBeenMigrated() {
            val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.migrate_indicia_publishers
                ORDER BY id
            """.trimIndent()

            //INSERT IGNORE INTO `{{targetSchema}}`.`gcd_indicia_publisher` (`id`, `name`, `parent_id`, `modified`)
            //VALUES (3, 'BAD parent_id', 3, '2023-06-01 19:56:37'),
            //(4, 'BAD parent_id', 4, '2023-06-01 19:56:37'),
            //(5, 'GOOD existing publisher', 1, '2023-06-01 19:56:37'),
            //(6, 'GOOD new publisher', 6, '2023-06-01 19:56:37'),
            //(7, 'BAD parent_id', 4, '2023-06-01 19:56:37');

            queryExecutor.executeQueryAndDo(query, conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Marvel Comics", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertEquals(1, rs.getInt("parent_id"))
                assertTrue(rs.next())
                assertEquals(5, rs.getInt("id"))
                assertEquals("GOOD existing publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertEquals(1, rs.getInt("parent_id"))
                assertTrue(rs.next())
                assertEquals(6, rs.getInt("id"))
                assertEquals("GOOD new publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertEquals(6, rs.getInt("parent_id"))
                assertFalse(rs.next())
            }


            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_indicia_publisher", conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Marvel Comics", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertEquals(1, rs.getInt("parent_id"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals("DC Comics", rs.getString("name"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertEquals(2, rs.getInt("parent_id"))
                assertTrue(rs.next())
                assertEquals(5, rs.getInt("id"))
                assertEquals("GOOD existing publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertEquals(1, rs.getInt("parent_id"))
                assertTrue(rs.next())
                assertEquals(6, rs.getInt("id"))
                assertEquals("GOOD new publisher", rs.getString("name"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertEquals(6, rs.getInt("parent_id"))
                assertFalse(rs.next())
            }
        }

        // gcd_issue
        fun verifyIssuesHaveBeenMigrated() {
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
                assertEquals(1, rs.getInt("indicia_publisher_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals(11, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals(1, rs.getInt("indicia_publisher_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(10, rs.getInt("id"))
                assertEquals(12, rs.getInt("number"))
                assertEquals(11, rs.getInt("series_id"))
                assertEquals(6, rs.getInt("indicia_publisher_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }

            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_issue", conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(35, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals(1, rs.getInt("indicia_publisher_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals(114, rs.getInt("number"))
                assertEquals(2, rs.getInt("series_id"))
                assertEquals(2, rs.getInt("indicia_publisher_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals(11, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals(1, rs.getInt("indicia_publisher_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(10, rs.getInt("id"))
                assertEquals(12, rs.getInt("number"))
                assertEquals(11, rs.getInt("series_id"))
                assertEquals(6, rs.getInt("indicia_publisher_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(13, rs.getInt("id"))
                assertEquals(17, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals(1, rs.getInt("indicia_publisher_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(14, rs.getInt("id"))
                assertEquals(18, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals(1, rs.getInt("indicia_publisher_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        // gcd_series_bond
        fun verifySeriesBondsHaveBeenMigrated() {
            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_series_bond", conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(1, rs.getInt("origin_id"))
                assertEquals(2, rs.getInt("target_id"))
                assertEquals(1, rs.getInt("origin_issue_id"))
                assertEquals(2, rs.getInt("target_issue_id"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals(2, rs.getInt("origin_id"))
                assertEquals(1, rs.getInt("target_id"))
                assertEquals(2, rs.getInt("origin_issue_id"))
                assertEquals(1, rs.getInt("target_issue_id"))
                assertTrue(rs.next())
                assertEquals(8, rs.getInt("id"))
                assertEquals(1, rs.getInt("origin_id"))
                assertEquals(9, rs.getInt("target_id"))
                assertEquals(1, rs.getInt("origin_issue_id"))
                assertEquals(2, rs.getInt("target_issue_id"))
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        // gcd_story
        fun verifyStoriesHaveBeenMigrated() {
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
                assertEquals("Grant Morrison", rs.getString("script"))
                assertEquals("Richard Case", rs.getString("pencils"))
                assertEquals("Richard Case", rs.getString("inks"))
                assertEquals("Daniel Vozzo", rs.getString("colors"))
                assertEquals("John Workman", rs.getString("letters"))
                assertEquals("Tom Peyer", rs.getString("editing"))
                assertEquals(
                    "Doom Patrol [Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]]; Danny the Street; Flex Mentallo (cameo, unnamed); Willoughby Kipling;",
                    rs.getString("characters")
                )
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(8, rs.getInt("id"))
                assertEquals("GOOD issue 1", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("Grant Morrison", rs.getString("script"))
                assertEquals("Richard Case", rs.getString("pencils"))
                assertEquals("John Nyberg", rs.getString("inks"))
                assertEquals("Daniel Vozzo", rs.getString("colors"))
                assertEquals("John Workman", rs.getString("letters"))
                assertEquals("Art Young", rs.getString("editing"))
                assertEquals(
                    "Individual [Alter Ego] (detail); Team [Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)]",
                    rs.getString("characters")
                )
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals("GOOD issue 9", rs.getString("title"))
                assertEquals(9, rs.getInt("issue_id"))
                assertEquals("Neil Gaiman", rs.getString("script"))
                assertEquals("Chris Bachalo", rs.getString("pencils"))
                assertEquals("Mark Buckingham", rs.getString("inks"))
                assertEquals("Steve Oliff", rs.getString("colors"))
                assertEquals("Todd Klein", rs.getString("letters"))
                assertEquals("Karen Berger", rs.getString("editing"))
                assertEquals(
                    "Individual [Alter Ego] (detail); Team [Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)]",
                    rs.getString("characters")
                )
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }

            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_story ORDER BY id", conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Crawling from the Wreckage", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("Grant Morrison", rs.getString("script"))
                assertEquals("Richard Case", rs.getString("pencils"))
                assertEquals("Richard Case", rs.getString("inks"))
                assertEquals("Daniel Vozzo", rs.getString("colors"))
                assertEquals("John Workman", rs.getString("letters"))
                assertEquals("Tom Peyer", rs.getString("editing"))
                assertEquals(
                    "Doom Patrol [Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]]; Danny the Street; Flex Mentallo (cameo, unnamed); Willoughby Kipling;",
                    rs.getString("characters")
                )
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals("E for Extinction", rs.getString("title"))
                assertEquals(2, rs.getInt("issue_id"))
                assertEquals("Grant Morrison", rs.getString("script"))
                assertEquals("Frank Quitely", rs.getString("pencils"))
                assertEquals("Tim Townsend", rs.getString("inks"))
                assertEquals("Liquid!", rs.getString("colors"))
                assertEquals("Richard Starkings", rs.getString("letters"))
                assertEquals("Mark Powers", rs.getString("editing"))
                assertEquals(
                    "X-Men [Beast [Hank McCoy]; Cyclops [Scott Summers]; White Queen [Emma Frost]; Marvel Girl [Jean Grey]; Professor X [Charles Xavier]; Wolverine [Logan]];",
                    rs.getString("characters")
                )
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(8, rs.getInt("id"))
                assertEquals("GOOD issue 1", rs.getString("title"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals("Grant Morrison", rs.getString("script"))
                assertEquals("Richard Case", rs.getString("pencils"))
                assertEquals("John Nyberg", rs.getString("inks"))
                assertEquals("Daniel Vozzo", rs.getString("colors"))
                assertEquals("John Workman", rs.getString("letters"))
                assertEquals("Art Young", rs.getString("editing"))
                assertEquals(
                    "Individual [Alter Ego] (detail); Team [Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)]",
                    rs.getString("characters")
                )
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(9, rs.getInt("id"))
                assertEquals("GOOD issue 9", rs.getString("title"))
                assertEquals(9, rs.getInt("issue_id"))
                assertEquals("Neil Gaiman", rs.getString("script"))
                assertEquals("Chris Bachalo", rs.getString("pencils"))
                assertEquals("Mark Buckingham", rs.getString("inks"))
                assertEquals("Steve Oliff", rs.getString("colors"))
                assertEquals("Todd Klein", rs.getString("letters"))
                assertEquals("Karen Berger", rs.getString("editing"))
                assertEquals(
                    "Individual [Alter Ego] (detail); Team [Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)]",
                    rs.getString("characters")
                )
                assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                assertFalse(rs.next())
            }
        }

        verifyPublishersHaveBeenMigrated()
        verifySeriesHaveBeenMigrated()
        verifyIndiciaPublishersHaveBeenMigrated()
        verifyIssuesHaveBeenMigrated()
        verifySeriesBondsHaveBeenMigrated()
        verifyStoriesHaveBeenMigrated()
    }

    /**
     * Verify that issue/series id columns have been added to gcd_story_credit,
     * m_story_credit, and m_character_appearance tables
     */
    private fun verifyIssueAndSeriesColumnsExist() {
        val query = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = '$TEST_DATABASE_UPDATE'
                AND table_name IN ('gcd_story_credit', 'm_story_credit', 'm_character_appearance')
                AND column_name IN ('issue_id', 'series_id')
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) { rs ->
            assertTrue(rs.next())
            assertEquals(6, rs.getInt(1))
        }
    }

    /**  */
    private fun verifyIssueAndSeriesColumnsValues() {
        fun query(table: String): String {
            return """SELECT * 
                        |FROM $TEST_DATABASE_UPDATE.$table 
                        |WHERE issue_id IS NOT NULL OR series_id IS NOT NULL
                        |""".trimMargin()
        }

        queryExecutor.executeQueryAndDo(query("gcd_story_credit"), conn) { rs ->
            assertTrue(rs.next())
            assertEquals(1, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(11, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(12, rs.getInt("id"))
            assertEquals(9, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertFalse(rs.next())
        }

        queryExecutor.executeQueryAndDo(query("m_story_credit"), conn) { rs ->
            assertTrue(rs.next())
            assertEquals(1, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(2, rs.getInt("id"))
            assertEquals(2, rs.getInt("issue_id"))
            assertEquals(2, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(3, rs.getInt("id"))
            assertEquals(2, rs.getInt("issue_id"))
            assertEquals(2, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(4, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertFalse(rs.next())
        }

        queryExecutor.executeQueryAndDo(query("m_character_appearance"), conn) { rs ->
            assertTrue(rs.next())
            assertEquals(1, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(2, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(3, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(4, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(5, rs.getInt("id"))
            assertEquals(1, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertTrue(rs.next())
            assertEquals(7, rs.getInt("id"))
            assertEquals(9, rs.getInt("issue_id"))
            assertEquals(1, rs.getInt("series_id"))
            assertFalse(rs.next())
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
                assertTrue(rs.next())
                assertEquals(13, rs.getInt("id"))
                assertEquals(17, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                assertTrue(rs.next())
                assertEquals(14, rs.getInt("id"))
                assertEquals(18, rs.getInt("number"))
                assertEquals(1, rs.getInt("series_id"))
                assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
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