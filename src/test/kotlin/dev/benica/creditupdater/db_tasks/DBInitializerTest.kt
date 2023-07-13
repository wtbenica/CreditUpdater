package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection

class DBInitializerTest {
    private val queryExecutor = QueryExecutor(TEST_DATABASE)

    @Test
    @DisplayName("should drop unused tables")
    fun shouldDropUnusedTables() {
        TestDatabaseSetup.setup(dbState = DBState.INITIAL)

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
                    'gcd_feature_logo_2_feature', 'gcd_indicia_printer', 'gcd_issue_indicia_printer', 
                    'gcd_membership_type', 'gcd_non_comic_work_role', 'gcd_non_comic_work_type', 
                    'gcd_non_comic_work_year', 'gcd_printer', 'gcd_received_award', 'gcd_relation_type', 
                    'gcd_school', 'gcd_story_feature_logo', 'gcd_story_feature_object', 'taggit_tag', 
                    'taggit_taggeditem');
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(34, it.getInt(1))
            assertFalse(it.next())
        }

        DBInitializer.dropUnusedTables(queryExecutor, conn)

        // verify that tables no longer exist
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(0, it.getInt(1))
            assertFalse(it.next())
        }

        TestDatabaseSetup.dropAllTables(conn, TEST_DATABASE)
    }

    @Test
    @DisplayName("should drop is_sourced and sourced_by columns")
    fun shouldDropIsSourcedAndSourcedByColumns() {
        TestDatabaseSetup.setup(dbState = DBState.UNUSED_TABLES_DROPPED)

        // verify columns exist in gcd_story_credit table
        val query = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = '$TEST_DATABASE'
                AND table_name = 'gcd_story_credit'
                AND column_name IN ('is_sourced', 'sourced_by');
            """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(2, it.getInt(1))
            assertFalse(it.next())
        }

        DBInitializer.dropIsSourcedAndSourcedByColumns(queryExecutor, conn)
    }

    @Test
    @DisplayName("should create the delete views correctly")
    fun shouldCreateDeleteViewsCorrectly() {
        TestDatabaseSetup.setup(dbState = DBState.EXTRACTED_TABLES_ADDED)

        fun verifyBadPublishers() {
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

        fun verifyBadSeries() {
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

        fun verifyBadIssues() {
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

        fun verifyBadStories() {
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

        fun verifyBadIndiciaPublishers() {
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

            // verify that bad indicia publishers contains ids 3, 4, 5
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

        DBInitializer.createDeleteViews(queryExecutor, conn)

        verifyBadPublishers()
        verifyBadIndiciaPublishers()
        verifyBadSeries()
        verifyBadIssues()
        verifyBadStories()

        TestDatabaseSetup.dropAllTables(conn, TEST_DATABASE)
    }

    @Test
    @DisplayName("should remove unnecessary records correctly")
    fun shouldRemoveUnnecessaryRecordsCorrectly() {
        TestDatabaseSetup.setup(DBState.DELETE_VIEWS_CREATED)

        // verify that the bad records exist
        fun verifyPublishers(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_publisher;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 5 else 3
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifySeries(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_series;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 10 else 5
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyIssues(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_issue;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 8 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyStories(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_story;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifySeriesBonds(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_series_bond;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyStoryCredits(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_story_credit;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 10 else 5
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyReprint(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_reprint;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyIssueCredit(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_issue_credit;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyIndiciaPublisher(exists: Boolean = true) {
            val query = """
            SELECT COUNT(*)
                FROM $TEST_DATABASE.gcd_indicia_publisher;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (exists) 4 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        verifyPublishers()
        verifySeries()
        verifySeriesBonds()
        verifyIssues()
        verifyStories()
        verifyStoryCredits()
        verifyReprint()
        verifyIssueCredit()
        verifyIndiciaPublisher()

        DBInitializer.removeUnnecessaryRecords(queryExecutor, conn)

        verifyPublishers(exists = false)
        verifySeries(exists = false)
        verifySeriesBonds(exists = false)
        verifyIssues(exists = false)
        verifyStories(exists = false)
        verifyStoryCredits(exists = false)
        verifyReprint(exists = false)
        verifyIssueCredit(exists = false)
        verifyIndiciaPublisher(exists = false)
    }

    @Test
    @DisplayName("should add issue_id and series_id columns and constraints to gcd_story_credit, m_story_credit, m_character_appearance")
    fun shouldAddIssueAndSeriesIdColumns() {
        // verify that gcd_story_credit, m_story_credit, m_character_appearance do not have issue_id and series_id columns
        fun verifyNoIssueAndSeriesIdColumns() {
            val query = """
            SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = '$TEST_DATABASE'
                AND table_name IN ('gcd_story_credit', 'm_story_credit', 'm_character_appearance')
                AND column_name IN ('issue_id', 'series_id');
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertFalse(it.next())
            }
        }

        //
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