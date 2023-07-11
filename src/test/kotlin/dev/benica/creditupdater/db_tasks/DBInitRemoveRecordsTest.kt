package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DatabaseState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.dropAllTables
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import java.sql.Connection

@TestMethodOrder(OrderAnnotation::class)
class DBInitRemoveRecordsTest {

    @Test
    @DisplayName("should remove records from gcd_biblio_entry with a link to an id in bad_stories")
    @Order(1)
    fun removeRecordsFromGcdBiblioEntryWithBadStoriesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_biblio_entry
                WHERE story_ptr_id IN (
                    SELECT id
                    FROM bad_stories
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdBiblioEntryFromBadStories()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_story_credit with a story_id link to an id in bad_stories")
    @Order(1)
    fun removeRecordsFromGcdStoryCreditWithBadStoriesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_story_credit
                WHERE story_id IN (
                    SELECT id
                    FROM bad_stories
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdStoryCreditFromBadStories()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_story_feature_object with a id link to an id in bad_stories")
    @Order(1)
    fun removeRecordsFromGcdStoryFeatureObjectWithBadStoriesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_story_feature_object
                WHERE story_id IN (
                    SELECT id
                    FROM bad_stories
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdStoryFeatureObjectFromBadStories()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_story_feature_logo with a id link to an id in bad_stories")
    @Order(1)
    fun removeRecordsFromGcdStoryFeatureLogoWithBadStoriesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_story_feature_logo
                WHERE story_id IN (
                    SELECT id
                    FROM bad_stories
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdStoryFeatureLogoFromBadStories()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_issue_credit with a id link to an id in bad_issues")
    @Order(1)
    fun removeRecordsFromGcdIssueCreditWithBadIssuesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_issue_credit
                WHERE issue_id IN (SELECT id FROM bad_issues)
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdIssueCreditFromBadIssues()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_issue_indicia_printer with a id link to an id in bad_issues")
    @Order(1)
    fun removeRecordsFromGcdIssueIndiciaPrinterWithBadIssuesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_issue_indicia_printer
                WHERE issue_id IN (
                    SELECT id
                    FROM bad_issues
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdIssueIndiciaPrinterFromBadIssues()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_series_bond with an origin_issue_id or target_issue_id in bad_issues")
    @Order(1)
    fun removeRecordsFromGcdSeriesBondWithBadIssuesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_series_bond
                WHERE origin_issue_id IN (SELECT id FROM bad_issues)
                OR target_issue_id IN (SELECT id FROM bad_issues)
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdSeriesBondFromBadIssues()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }


    @Test
    @DisplayName("should remove records from gcd_reprint with an origin_id or target_id link to an id in bad_issues")
    @Order(1)
    fun removeRecordsFromGcdReprintWithBadIssuesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_reprint
                WHERE origin_id IN (SELECT id FROM bad_issues)
                OR target_id IN (SELECT id FROM bad_issues)
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdReprintFromBadIssues()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_story with an issue_id link to an id in bad_issues")
    @Order(2)
    fun removeRecordsFromGcdStoryWithBadIssuesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_story
                WHERE issue_id IN (
                    SELECT id
                    FROM bad_issues
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdStoryFromBadIssues()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should set variant to null if it references an issue whose series is bad or whose series' publisher is bad")
    @Order(2)
    fun setVariantToNullIfIssueSeriesOrPublisherIsBad() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_issue
                WHERE variant_of_id IN (
                    SELECT id
                    FROM bad_issues
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.setVariantOfIdToNullIfItIsBad()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should set first_issue_id or last_issue_id to null if it references a bad issue")
    @Order(2)
    fun setFirstIssueIdAndLastIssueIdToNullIfIssueIsBad() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_series
                WHERE first_issue_id IN (
                    SELECT id
                    FROM bad_issues
                )
                OR last_issue_id IN (
                    SELECT id
                    FROM bad_issues
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.setFirstLastIssueToNullIfItIsBad()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_issue with a series_id in bad_series")
    @Order(3)
    fun removeRecordsFromGcdIssueWithBadSeriesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_issue
                WHERE series_id IN (
                    SELECT id
                    FROM bad_series
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdIssueFromBadSeries()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_series_bond if origin_id or target_id references a bad series")
    @Order(2)
    fun removeRecordsFromGcdSeriesBondWithBadSeriesLink() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_series_bond
                WHERE origin_id IN (
                    SELECT id
                    FROM bad_series
                )
                OR target_id IN (
                    SELECT id
                    FROM bad_series
                )
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdSeriesBondFromBadSeries()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    @Test
    @DisplayName("should remove records from gcd_series if publisher_id references a bad publisher or meets other bad series criteria")
    @Order(4)
    fun shouldRemoveBadSeries() {
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_series
                WHERE publisher_id IN (
                    SELECT id
                    FROM bad_publishers
                )
                OR (country_id != 225 OR language_id != 25 OR year_began < 1900)
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            val result = stmt.executeQuery(query)
            result.next()
            assertTrue(result.getBoolean(1))

            val dbInitRemoveRecords = DBInitRemoveRecords(
                queryExecutor = QueryExecutor(TEST_DATABASE),
                targetSchema = TEST_DATABASE,
                conn = conn
            )

            dbInitRemoveRecords.deleteGcdSeriesFromBadPublishers()

            val result2 = stmt.executeQuery(query)
            result2.next()
            assertFalse(result2.getBoolean(1))
        }
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            conn = getTestDbConnection()
            TestDatabaseSetup.setup(populateWith = DatabaseState.RAW_FOR_CULLING)
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            conn.close()
            dropAllTables(getTestDbConnection(), TEST_DATABASE)
        }
    }
}