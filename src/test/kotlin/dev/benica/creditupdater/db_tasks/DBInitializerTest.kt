package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection

class DBInitializerTest {
    private val queryExecutor = QueryExecutor()

    @Test
    @DisplayName("should drop unused tables")
    fun shouldDropUnusedTables() {
        TestDatabaseSetup.setup(dbState = DBState.INITIAL)

        verifyUnusedTables()

        DBInitializer.dropUnusedTables(queryExecutor, conn)

        verifyUnusedTables(exist = false)
    }

    @Test
    @DisplayName("should drop is_sourced and sourced_by columns")
    fun shouldDropIsSourcedAndSourcedByColumns() {
        TestDatabaseSetup.setup(dbState = DBState.INIT_STEP_1A)

        // verify columns exist in gcd_story_credit table
        verifySourcedColumns()

        DBInitializer.dropIsSourcedAndSourcedByColumns(queryExecutor, conn)

        verifySourcedColumns(false)
    }

    @Test
    @DisplayName("should create the delete views correctly")
    fun shouldCreateDeleteViewsCorrectly() {
        TestDatabaseSetup.setup(dbState = DBState.INIT_STEP_1C)

        DBInitializer.createDeleteViews(queryExecutor, conn)

        verifyDeleteViewsExist()
        verifyDeleteViewsContain()
    }

    @Test
    @DisplayName("should remove unnecessary records correctly")
    fun shouldRemoveUnnecessaryRecordsCorrectly() {
        TestDatabaseSetup.setup(DBState.INIT_STEP_1D)

        verifyRecordCounts()

        DBInitializer.removeUnnecessaryRecords(queryExecutor, conn)

        verifyRecordCounts(unculled = false)
    }

    @Test
    @DisplayName("should add issue_id and series_id columns and constraints to gcd_story_credit, m_story_credit, m_character_appearance")
    fun shouldAddIssueAndSeriesIdColumns() {
        TestDatabaseSetup.setup(DBState.INIT_STEP_3_COMPLETE)

        // verify that issue_id and series_id are all null for gcd_story_credit, m_story_credit, m_character_appearance
        fun verifyIssueAndSeriesIdsAreSetToNull() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_story_credit
                WHERE issue_id IS NOT NULL
                    OR series_id IS NOT NULL;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(0, it.getInt(1))
            }

            val query2 = """
            SELECT COUNT(*)
                FROM m_story_credit
                WHERE issue_id IS NOT NULL
                    OR series_id IS NOT NULL;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query2, conn) {
                assertTrue(it.next())
                assertEquals(0, it.getInt(1))
            }

            val query3 = """
            SELECT COUNT(*)
                FROM m_character_appearance
                WHERE issue_id IS NOT NULL
                    OR series_id IS NOT NULL;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query3, conn) {
                assertTrue(it.next())
                assertEquals(0, it.getInt(1))
            }
        }

        verifyIssueAndSeriesIdsAreSetToNull()

        DBInitializer.addIssueSeriesColumnsAndConstraints(queryExecutor, conn)

        verifyIssueAndSeriesIdsAreSet()
    }

    @Test
    @DisplayName("should prepare database when startAtStep is 1")
    fun shouldPrepareDbFromStep1() {
        TestDatabaseSetup.setup(dbState = DBState.INITIAL)

        Thread.sleep(3000)

        runBlocking {
            val dbInitializer = DBInitializer(
                targetSchema = TEST_DATABASE,
                startAtStep = 0,
                startingId = 0
            )

            val job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    dbInitializer.prepareDb()
                } catch (e: Exception) {
                    e.printStackTrace()
                    fail("Exception thrown: ${e.message}")
                }
            }

            job.join()

            // give db ops a chance to finish
            Thread.sleep(2000)

            verifyDbPrepared()
        }
    }

    private fun verifyDbPrepared() {
        verifyUnusedTables(exist = false)
        verifySourcedColumns(exist = false)
        verifyDeleteViewsExist()
        verifyDeleteViewsContain(isEmpty = true)
        verifyRecordCounts(unculled = false)
        DBTaskTest.verifyCharactersWereExtracted(conn)
        DBTaskTest.verifyCharacterAppearancesWereExtracted(conn)
        verifyIssueAndSeriesIdsAreSet()
    }

    private fun verifyUnusedTables(exist: Boolean = true, targetSchema: String = TEST_DATABASE) {
        val query = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = '$targetSchema'
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

        val expected = if (exist) 34 else 0

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(expected, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifySourcedColumns(exist: Boolean = true, targetSchema: String = TEST_DATABASE) {
        val expected = if (exist) 2 else 0

        val query = """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = '$targetSchema'
                    AND table_name = 'gcd_story_credit'
                    AND column_name IN ('is_sourced', 'sourced_by');
                """.trimIndent()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertEquals(expected, it.getInt(1))
            assertFalse(it.next())
        }
    }

    private fun verifyDeleteViewsExist(targetSchema: String = TEST_DATABASE) {
        fun verifyBadPublishers() {
            // verify bad_publishers view was created
            val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$targetSchema'
                    AND table_name = 'bad_publishers'
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertTrue(it.getBoolean(1))
                assertFalse(it.next())
            }
        }

        fun verifyBadSeries() {
            val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$targetSchema'
                    AND table_name = 'bad_series'
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertTrue(it.getBoolean(1))
                assertFalse(it.next())
            }
        }

        fun verifyBadIssues() {
            // verify bad_issues view was created
            val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$targetSchema'
                    AND table_name = 'bad_issues'
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertTrue(it.getBoolean(1))
                assertFalse(it.next())
            }
        }

        fun verifyBadStories() {
            // verify bad_stories view was created
            val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$targetSchema'
                    AND table_name = 'bad_stories'
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertTrue(it.getBoolean(1))
                assertFalse(it.next())
            }
        }

        fun verifyBadIndiciaPublishers() {
            // verify bad_indicia_publishers view was created
            val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = '$targetSchema'
                    AND table_name = 'bad_indicia_publishers'
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertTrue(it.getBoolean(1))
                assertFalse(it.next())
            }
        }

        verifyBadPublishers()
        verifyBadSeries()
        verifyBadIssues()
        verifyBadStories()
        verifyBadIndiciaPublishers()
    }

    private fun verifyDeleteViewsContain(isEmpty: Boolean = false) {
        fun verifyBadPublishers() {
            // verify that bad publishers contains ids 3, 4
            val query = """
            SELECT id
            FROM bad_publishers
            ORDER BY id;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                if (!isEmpty) {
                    assertTrue(it.next())
                    assertEquals(3, it.getInt(1))
                    assertTrue(it.next())
                    assertEquals(4, it.getInt(1))
                }
                assertFalse(it.next())
            }
        }

        fun verifyBadSeries() {
            // verify that bad series contains ids 6, 7, 8, 9, 10
            val query = """
            SELECT id
            FROM bad_series
            ORDER BY id;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                if (!isEmpty) {
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
                }
                assertFalse(it.next())
            }
        }

        fun verifyBadIssues() {
            // verify that bad issues contains ids 3, 4, 5, 6, 7, 8
            val query = """
            SELECT id
            FROM bad_issues
            ORDER BY id;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                if (!isEmpty) {
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
                    assertTrue(it.next())
                    assertEquals(8, it.getInt(1))
                }
                assertFalse(it.next())
            }
        }

        fun verifyBadStories() {
            // verify that bad stories contains ids 3, 4, 5, 6, 7
            val query = """
            SELECT id
            FROM bad_stories
            ORDER BY id;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                if (!isEmpty) {
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
                }
                assertFalse(it.next())
            }
        }

        fun verifyBadIndiciaPublishers() {
            // verify that bad indicia publishers contains ids 3, 4
            val query = """
            SELECT id
            FROM bad_indicia_publishers
            ORDER BY id;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                if (!isEmpty) {
                    assertTrue(it.next())
                    assertEquals(3, it.getInt(1))
                    assertTrue(it.next())
                    assertEquals(4, it.getInt(1))
                }
                assertFalse(it.next())
            }
        }

        verifyBadPublishers()
        verifyBadSeries()
        verifyBadIssues()
        verifyBadStories()
        verifyBadIndiciaPublishers()
    }

    private fun verifyRecordCounts(unculled: Boolean = true) {
        fun verifyPublishers() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_publisher;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 5 else 3
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifySeries() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_series;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 10 else 5
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyIssues() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_issue;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 8 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyStories() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_story;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifySeriesBonds() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_series_bond;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyStoryCredits() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_story_credit;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 10 else 5
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyReprint() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_reprint;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyIssueCredit() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_issue_credit;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 7 else 2
                assertTrue(it.next())
                assertEquals(expectedCount, it.getInt(1))
            }
        }

        fun verifyIndiciaPublisher() {
            val query = """
            SELECT COUNT(*)
                FROM gcd_indicia_publisher;
        """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                val expectedCount = if (unculled) 4 else 2
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
    }

    private fun verifyIssueAndSeriesIdsAreSet() {
        fun verifyColumnValuesGcdStoryCredit() {
            val query = """
                SELECT COUNT(*)
                FROM gcd_story_credit gsc
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM gcd_story gs
                    JOIN gcd_issue gi on gi.id = gs.issue_id
                    WHERE gs.id = gsc.story_id
                        AND gs.issue_id = gsc.issue_id
                        AND gi.series_id = gsc.series_id
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(0, it.getInt(1))
            }
        }

        fun verifyColumnValuesMStoryCredit() {
            // verify that issue_id and series_id in m_story_credit match the issue_id and series_id in the referenced gcd_story
            val query = """
                SELECT COUNT(*)
                FROM m_story_credit msc
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM gcd_story gs
                    JOIN gcd_issue gi on gi.id = gs.issue_id
                    WHERE gs.id = msc.story_id
                        AND gs.issue_id = msc.issue_id
                        AND gi.series_id = msc.series_id
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(0, it.getInt(1))
            }
        }

        fun verifyColumnValuesMCharacterAppearance() {
            // verify that issue_id and series_id in m_character_appearance match the issue_id and series_id in the referenced gcd_story
            val query = """
                SELECT COUNT(*)
                FROM m_character_appearance mca
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM gcd_story gs
                    JOIN gcd_issue gi on gi.id = gs.issue_id
                    WHERE gs.id = mca.story_id
                        AND gs.issue_id = mca.issue_id
                        AND gi.series_id = mca.series_id
                );
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(0, it.getInt(1))
            }
        }

        fun verifyColumnConstraints() {
            val query = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = '$TEST_DATABASE'
                    AND table_name IN ('gcd_story_credit', 'm_story_credit', 'm_character_appearance')
                    AND column_name IN ('issue_id', 'series_id')
                    AND is_nullable = 'NO';
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, conn) {
                assertTrue(it.next())
                assertEquals(6, it.getInt(1))
            }
        }

        verifyColumnValuesGcdStoryCredit()
        verifyColumnValuesMStoryCredit()
        verifyColumnValuesMCharacterAppearance()
        verifyColumnConstraints()
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