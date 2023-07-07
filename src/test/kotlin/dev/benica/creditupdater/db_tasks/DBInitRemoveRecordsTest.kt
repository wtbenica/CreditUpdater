package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.db.DatabaseState
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.dropAllTables
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.sql.Connection

class DBInitRemoveRecordsTest {

    @Test
    @DisplayName("should remove records from gcd_biblio_entry with a link to an id in bad_stories")
    fun removeRecordsFromGcdBiblioEntryWithBadStoriesLink() {
        val conn = getTestDbConnection()
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_biblio_entry
                WHERE story_id IN (
                    SELECT id
                    FROM bad_stories
                )
            )
        """.trimIndent()

        val result = conn.createStatement().executeQuery(query)
        result.next()
        assertTrue(result.getBoolean(1))
        conn.close()
    }

    @Test
    @DisplayName("should remove records from gcd_reprint with an origin_id or target_id link to an id in bad_issues")
    fun removeRecordsFromGcdReprintWithBadIssuesLink() {
        val conn = getTestDbConnection()
        val query = """
            SELECT EXISTS (
                SELECT 1
                FROM gcd_reprint
                WHERE origin_id IN (
                    SELECT id
                    FROM bad_issues
                )
                OR target_id IN (
                    SELECT id
                    FROM bad_issues
                )
            )
        """.trimIndent()

        val result = conn.createStatement().executeQuery(query)
        result.next()
        assertTrue(result.getBoolean(1))
        conn.close()
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            conn = getTestDbConnection()
            TestDatabaseSetup.setup(populate = DatabaseState.RAW_FOR_CULLING)
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            conn.close()
            dropAllTables(getTestDbConnection(), Credentials.TEST_DATABASE)
        }
    }
}