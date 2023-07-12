package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DatabaseState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DBInitializerTest {
    private val queryExecutor = QueryExecutor(TEST_DATABASE)

    @Test
    @Order(1)
    @DisplayName("should drop unused tables")
    fun shouldDropUnusedTables() {
        // verify that tables exist
        val query = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = '$TEST_DATABASE'
                AND table_name IN ('django_content_type', 'gcd_award', 'gcd_brand_group', 'gcd_biblio_entry',
                    'gcd_brand_use', 'gcd_brand_emblem_use', 'gcd_creator_art_influence', 'gcd_creator_degree',
                    'gcd_creator_membership', 'gcd_creator_non_comic_work', 'gcd_creator_relation',
                    'gcd_creator_relation_creator_name', 'gcd_creator_school', 'gcd_degree', 'gcd_feature',
                    'gcd_feature_logo', 'gcd_feature_type', 'gcd_feature_relation', 'gcd_feature_relation_type',
                    'gcd_feature_logo_2_feature', 'gcd_indicia_printer', 'gcd_indicia_publisher',
                    'gcd_issue_indicia_printer', 'gcd_membership_type', 'gcd_non_comic_work_role',
                    'gcd_non_comic_work_type', 'gcd_non_comic_work_year', 'gcd_printer', 'gcd_received_award',
                    'gcd_relation_type', 'gcd_school', 'gcd_story_feature_logo', 'gcd_story_feature_object',
                    'taggit_tag', 'taggit_taggeditem');
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(35, it.getInt(1))
            assertFalse(it.next())
        }

        DBInitializer.dropUnusedTables(queryExecutor, conn)

        // verify that tables no longer exist
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(0, it.getInt(1))
            assertFalse(it.next())
        }
    }

    @Test
    @Order(2)
    @DisplayName("should create the delete views correctly")
    fun shouldCreateDeleteViewsCorrectly() {
        DBInitializer.createDeleteViews(queryExecutor, conn)

        verifyBadPublishers()
        verifyBadSeries()
        verifyBadIssues()
        verifyBadStories()
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

        // verify that bad stories contains ids 3, 4, 5, 6, 7
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
            assertTrue(it.next())
            assertEquals(7, it.getInt(1))
            assertFalse(it.next())
        }
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            conn = getTestDbConnection()
            TestDatabaseSetup.setup(populateWith = DatabaseState.EMPTY)
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            conn.close()
            //TestDatabaseSetup.dropAllTables(getTestDbConnection(), TEST_DATABASE)
        }
    }
}