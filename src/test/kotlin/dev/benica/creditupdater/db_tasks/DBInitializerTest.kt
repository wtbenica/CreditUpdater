package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DatabaseState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection

class DBInitializerTest {
    private val queryExecutor = QueryExecutor(TEST_DATABASE)

    @Test
    @DisplayName("should create the delete views correctly")
    fun shouldCreateDeleteViewsCorrectly() {
        DBInitializer.createDeleteViews(queryExecutor, conn)

        verifyBadPublishers()
        verifyBadSeries()
        verifyBadIssues()
        verifyBadStories()
        verifyBadIndiciaPublishers()
        verifyBadBrandGroups()
    }

    private fun verifyBadPublishers() {
        // verify bad_publishers view was created
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$TEST_DATABASE'
                    AND table_name = 'bad_publishers'
                );
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.getBoolean(1))
            assertFalse(it.next())
        }

        // verify that bad publishers contains ids 3, 4, and not 5
        val query2 = """
            SELECT id
            FROM $TEST_DATABASE.bad_publishers
            ORDER BY id;
        """.trimIndent()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertEquals(3, it.getInt(1))
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifyBadSeries() {
        // verify bad_series view was created
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$TEST_DATABASE'
                    AND table_name = 'bad_series'
                );
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.getBoolean(1))
            assertFalse(it.next())
        }

        // verify that bad series contains ids 3, 4, 5, 6, 7
        val query2 = """
            SELECT id
            FROM $TEST_DATABASE.bad_series
            ORDER BY id;
        """.trimIndent()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertEquals(3, it.getInt(1))
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
            assertTrue(it.next())
            assertEquals(5, it.getInt(1))
            assertTrue(it.next())
            assertEquals(6, it.getInt(1))
            assertTrue(it.next())
            assertEquals(7, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifyBadIssues() {
        // verify bad_issues view was created
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$TEST_DATABASE'
                    AND table_name = 'bad_issues'
                );
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.getBoolean(1))
            assertFalse(it.next())
        }

        // verify that bad issues contains ids 3, 4, 5, 6
        val query2 = """
            SELECT id
            FROM $TEST_DATABASE.bad_issues
            ORDER BY id;
        """.trimIndent()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertEquals(3, it.getInt(1))
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
            assertTrue(it.next())
            assertEquals(5, it.getInt(1))
            assertTrue(it.next())
            assertEquals(6, it.getInt(1))
            assertTrue(it.next())
            assertEquals(7, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifyBadStories() {
        // verify bad_stories view was created
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$TEST_DATABASE'
                    AND table_name = 'bad_stories'
                );
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.getBoolean(1))
            assertFalse(it.next())
        }

        // verify that bad stories contains ids 3, 4, 5, 6
        val query2 = """
            SELECT id
            FROM $TEST_DATABASE.bad_stories
            ORDER BY id;
        """.trimIndent()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertEquals(3, it.getInt(1))
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
            assertTrue(it.next())
            assertEquals(5, it.getInt(1))
            assertTrue(it.next())
            assertEquals(6, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifyBadIndiciaPublishers() {
        // verify bad_indicia_publishers view was created
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$TEST_DATABASE'
                    AND table_name = 'bad_indicia_publishers'
                );
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.getBoolean(1))
            assertFalse(it.next())
        }

        // verify that bad indicia publishers contains ids 3, 4
        val query2 = """
            SELECT id
            FROM $TEST_DATABASE.bad_indicia_publishers
            ORDER BY id;
        """.trimIndent()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertEquals(3, it.getInt(1))
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifyBadBrandGroups() {
        // verify bad_brand_groups view was created
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$TEST_DATABASE'
                    AND table_name = 'bad_brand_groups'
                );
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.getBoolean(1))
            assertFalse(it.next())
        }

        // verify that bad brands contains ids 3, 4
        val query2 = """
            SELECT id
            FROM $TEST_DATABASE.bad_brand_groups
            ORDER BY id;
        """.trimIndent()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertEquals(3, it.getInt(1))
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
            assertFalse(it.next())
        }
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            conn = TestDatabaseSetup.getTestDbConnection()
            TestDatabaseSetup.setup(populateWith = DatabaseState.RAW_FOR_BAD_VIEWS)
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            conn.close()
            TestDatabaseSetup.dropAllTables(TestDatabaseSetup.getTestDbConnection(), TEST_DATABASE)
        }
    }
}