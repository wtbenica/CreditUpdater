package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.DatabaseState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.dropAllTables
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.sql.Connection

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DBInitCreateDeleteViewsTest {

    private val queryExecutor = QueryExecutor(TEST_DATABASE)
    private var dbInitCreateDeleteViews: DBInitCreateDeleteViews =
        DBInitCreateDeleteViews(queryExecutor, TEST_DATABASE, conn)

    @BeforeEach
    fun setup() {
        dbInitCreateDeleteViews = DBInitCreateDeleteViews(queryExecutor, TEST_DATABASE, conn)
    }

    @Test
    @DisplayName("should call each function once when createDeleteViews is called")
    fun callEachFunctionOnce() {
        val dbInitCreateDeleteViewsMock = spy(DBInitCreateDeleteViews(queryExecutor, TEST_DATABASE, conn))

        // Mock the functions in createDeleteViews
        doNothing().whenever(dbInitCreateDeleteViewsMock).createBadPublishersView()
        doNothing().whenever(dbInitCreateDeleteViewsMock).createBadSeriesView()
        doNothing().whenever(dbInitCreateDeleteViewsMock).createBadIssuesView()
        doNothing().whenever(dbInitCreateDeleteViewsMock).createBadStoriesView()
        doNothing().whenever(dbInitCreateDeleteViewsMock).createBadIndiciaPublishersView()
        doNothing().whenever(dbInitCreateDeleteViewsMock).createBadBrandGroupsView()

        dbInitCreateDeleteViewsMock.createDeleteViews()

        // Verify that each function was called once
        verify(dbInitCreateDeleteViewsMock).createBadPublishersView()
        verify(dbInitCreateDeleteViewsMock).createBadSeriesView()
        verify(dbInitCreateDeleteViewsMock).createBadIssuesView()
        verify(dbInitCreateDeleteViewsMock).createBadStoriesView()
        verify(dbInitCreateDeleteViewsMock).createBadIndiciaPublishersView()
        verify(dbInitCreateDeleteViewsMock).createBadBrandGroupsView()
    }

    @Test
    @Order(1)
    @DisplayName("should create bad_publishers view")
    fun createBadPublishersView() {
        dbInitCreateDeleteViews.createBadPublishersView()

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

        // verify that bad publishers contains ids 3, 4
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

    @Test
    @Order(2)
    @DisplayName("should create bad_series view")
    fun createBadSeriesView() {
        dbInitCreateDeleteViews.createBadSeriesView()

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

    @Test
    @Order(3)
    @DisplayName("should create bad_issues view")
    fun createBadIssuesView() {
        dbInitCreateDeleteViews.createBadIssuesView()

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
            assertFalse(it.next())
        }
    }

    @Test
    @Order(4)
    @DisplayName("should create bad_stories view")
    fun createBadStoriesView() {
        dbInitCreateDeleteViews.createBadStoriesView()

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

    @Test
    @Order(2)
    @DisplayName("should create bad_indicia_publishers view")
    fun createBadIndiciaPublishersView() {
        dbInitCreateDeleteViews.createBadIndiciaPublishersView()

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

    @Test
    @Order(2)
    @DisplayName("should create bad_brand_groups view")
    fun createBadBrandsView() {
        dbInitCreateDeleteViews.createBadBrandGroupsView()

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
            conn = getTestDbConnection()
            TestDatabaseSetup.setup(populate = DatabaseState.RAW)
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            conn.close()
            dropAllTables(getTestDbConnection(), TEST_DATABASE)
        }
    }
}