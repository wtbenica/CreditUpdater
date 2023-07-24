/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.TEST_DATABASE
import dev.benica.creditupdater.Credentials.TEST_DATABASE_UPDATE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getDbConnection
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.db_tasks.DBMigrator.Companion.addIssueSeriesToCreditsNew
import dev.benica.creditupdater.db_tasks.DBMigrator.Companion.addTablesNew
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

            verifyGoodTables(queryExecutor)
            verifyMigrateTables(queryExecutor)
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

        verifyIssueAndSeriesColumnsExist(queryExecutor)
        verifyIssueAndSeriesColumnsValues(queryExecutor)
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

        verifyGoodTables(queryExecutor)
        verifyMigrateTables(queryExecutor)
        verifyTargetTablesAreUpdated(queryExecutor)
    }

    @Test
    @DisplayName("should migrate from source to target schema")
    fun shouldMigrate() {
        TestDatabaseSetup.setup(dbState = DBState.INITIALIZED, schema = TEST_DATABASE, sourceSchema = null)

        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_INITIAL,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )

        runBlocking {
            val job = CoroutineScope(Dispatchers.IO).launch {
                DBMigrator(
                    sourceSchema = TEST_DATABASE_UPDATE,
                    targetSchema = TEST_DATABASE
                ).migrate()
            }

            job.join()

            Thread.sleep(2000)

            verifyGoodTables(queryExecutor)
            verifyMigrateTables(queryExecutor)
            verifyTargetTablesAreUpdated(queryExecutor)
            verifyCharactersHaveBeenMigrated(queryExecutor)
            verifyCreditsHaveBeenMigrated(queryExecutor)
        }
    }

    @Test
    fun extractCharactersAndAppearances() {
        TestDatabaseSetup.setup(dbState = DBState.INITIALIZED, schema = TEST_DATABASE, sourceSchema = null)

        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_STEP_4_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )
    }

    companion object {
        internal fun verifyCharactersHaveBeenMigrated(queryExecutor: QueryExecutor, connection: Connection? = null) {
            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.m_character ORDER BY id", connection ?: conn) { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals("Doom Patrol", rs.getString("name"))
                assertNull(rs.getString("alter_ego"))
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("id"))
                assertEquals("Danny the Street", rs.getString("name"))
                assertNull(rs.getString("alter_ego"))
                assertTrue(rs.next())
                assertEquals(3, rs.getInt("id"))
                assertEquals("Flex Mentallo", rs.getString("name"))
                assertNull(rs.getString("alter_ego"))
                assertTrue(rs.next())
                assertEquals(4, rs.getInt("id"))
                assertEquals("Willoughby Kipling", rs.getString("name"))
                assertNull(rs.getString("alter_ego"))
                assertTrue(rs.next())
                assertEquals(5, rs.getInt("id"))
                assertEquals("X-Men", rs.getString("name"))
                assertNull(rs.getString("alter_ego"))
                assertTrue(rs.next())
                assertEquals(6, rs.getInt("id"))
                assertEquals("Individual", rs.getString("name"))
                assertEquals("Alter Ego", rs.getString("alter_ego"))
                assertTrue(rs.next())
                assertEquals(7, rs.getInt("id"))
                assertEquals("Team", rs.getString("name"))
                assertNull(rs.getString("alter_ego"))
                assertFalse(rs.next())
            }
        }

        internal fun verifyCreditsHaveBeenMigrated(queryExecutor: QueryExecutor, connection: Connection? = null) {
            queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.m_character_appearance ORDER BY id", connection ?: conn) {
                assertTrue(it.next())
                assertEquals(1, it.getInt("id"))
                assertEquals(1, it.getInt("character_id"))
                assertEquals(1, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(2, it.getInt("id"))
                assertEquals(2, it.getInt("character_id"))
                assertEquals(1, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(3, it.getInt("id"))
                assertEquals(3, it.getInt("character_id"))
                assertEquals(1, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(4, it.getInt("id"))
                assertEquals(4, it.getInt("character_id"))
                assertEquals(1, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(5, it.getInt("id"))
                assertEquals(5, it.getInt("character_id"))
                assertEquals(2, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(6, it.getInt("id"))
                assertEquals(6, it.getInt("character_id"))
                assertEquals(8, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(7, it.getInt("id"))
                assertEquals(7, it.getInt("character_id"))
                assertEquals(8, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(8, it.getInt("id"))
                assertEquals(6, it.getInt("character_id"))
                assertEquals(9, it.getInt("story_id"))
                assertTrue(it.next())
                assertEquals(9, it.getInt("id"))
                assertEquals(7, it.getInt("character_id"))
                assertEquals(9, it.getInt("story_id"))
                assertFalse(it.next())
            }
        }

        internal fun verifyTargetTablesAreUpdated(queryExecutor: QueryExecutor, connection: Connection? = null) {
            // static tables
            fun verifyStddataCountryHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.stddata_country ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(14, rs.getInt("id"))
                    assertEquals("AU", rs.getString("code"))
                    assertEquals("Australia", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(36, rs.getInt("id"))
                    assertEquals("CA", rs.getString("code"))
                    assertEquals("Canada", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(150, rs.getInt("id"))
                    assertEquals("MX", rs.getString("code"))
                    assertEquals("Mexico", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(225, rs.getInt("id"))
                    assertEquals("US", rs.getString("code"))
                    assertEquals("United States", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyStddataLanguageHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.stddata_language ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(22, rs.getInt("id"))
                    assertEquals("German", rs.getString("name"))
                    assertEquals("de", rs.getString("code"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(25, rs.getInt("id"))
                    assertEquals("English", rs.getString("name"))
                    assertEquals("en", rs.getString("code"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(27, rs.getInt("id"))
                    assertEquals("Spanish", rs.getString("name"))
                    assertEquals("es", rs.getString("code"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(34, rs.getInt("id"))
                    assertEquals("French", rs.getString("name"))
                    assertEquals("fr", rs.getString("code"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyStddataDateHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.stddata_date ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals(2019, rs.getInt("year"))
                    assertEquals(1, rs.getInt("month"))
                    assertEquals(1, rs.getInt("day"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals(2019, rs.getInt("year"))
                    assertEquals(1, rs.getInt("month"))
                    assertEquals(2, rs.getInt("day"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals(2019, rs.getInt("year"))
                    assertEquals(1, rs.getInt("month"))
                    assertEquals(3, rs.getInt("day"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals(2000, rs.getInt("year"))
                    assertEquals(1, rs.getInt("month"))
                    assertEquals(1, rs.getInt("day"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifySeriesPublicationTypeHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.gcd_series_publication_type ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("book", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("magazine", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("album", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("limited series", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyBrandHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_brand ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("DC", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("Marvel", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("Image", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("Vertigo", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyStoryTypeHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_story_type ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("cover", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("story", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("promo", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("letters column", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyNameTypeHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_name_type ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("Changed Name", rs.getString("type"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("Native Language (type is deprecated)", rs.getString("type"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("Name at Birth", rs.getString("type"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("pseudonym", rs.getString("type"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyScriptHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.stddata_script ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("Latin", rs.getString("name"))
                    assertEquals("Latn", rs.getString("code"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("Cyrillic", rs.getString("name"))
                    assertEquals("Cyrl", rs.getString("code"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("Greek", rs.getString("name"))
                    assertEquals("Grek", rs.getString("code"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(7, rs.getInt("id"))
                    assertEquals("Bengali", rs.getString("name"))
                    assertEquals("Beng", rs.getString("code"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            fun verifyCreatorSignatureHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.gcd_creator_signature ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("John Smith", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("Jane Doe", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("John Doe", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("Grant Morrison", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            // Dynamic tables
            fun verifyPublishersHaveBeenMigrated() {
                // select all publishers from sourceSchema.migrate_publishers and verify that they are in targetSchema.publishers
                val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.migrate_publishers
                ORDER BY id
            """.trimIndent()

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_publisher", connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) {
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

                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_series", connection ?: conn) {
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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


                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_indicia_publisher", connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_issue", connection ?: conn) { rs ->
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
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_series_bond", connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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
                        "Doom Patrol [Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]; Joshua Clay]; Danny the Street; Flex Mentallo (cameo, unnamed); Willoughby Kipling;",
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

                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_story ORDER BY id", connection ?: conn) { rs ->
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
                        "Doom Patrol [Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]; Joshua Clay]; Danny the Street; Flex Mentallo (cameo, unnamed); Willoughby Kipling;",
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

            // gcd_creator
            fun verifyCreatorsHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_creator ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("Grant Morrison", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("Frank Quitely", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("Val Semeiks", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("Dan Green", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(5, rs.getInt("id"))
                    assertEquals("Chris Sotomayor", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(6, rs.getInt("id"))
                    assertEquals("Richard Starkings", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(7, rs.getInt("id"))
                    assertEquals("Bob Schreck", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(8, rs.getInt("id"))
                    assertEquals("Michael Wright", rs.getString("name"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(9, rs.getInt("id"))
                    assertEquals("Neil Gaiman", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(10, rs.getInt("id"))
                    assertEquals("Jonathan Hickman", rs.getString("name"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            // gcd_creator_name_detail
            fun verifyCreatorNameDetailsHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.gcd_creator_name_detail ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals("Grant Morrison", rs.getString("name"))
                    assertEquals(1, rs.getInt("creator_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals("Frank Quitely", rs.getString("name"))
                    assertEquals(2, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals("Val Semeiks", rs.getString("name"))
                    assertEquals(3, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals("Dan Green", rs.getString("name"))
                    assertEquals(4, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(5, rs.getInt("id"))
                    assertEquals("Chris Sotomayor", rs.getString("name"))
                    assertEquals(5, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(6, rs.getInt("id"))
                    assertEquals("Richard Starkings", rs.getString("name"))
                    assertEquals(6, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(7, rs.getInt("id"))
                    assertEquals("Bob Schreck", rs.getString("name"))
                    assertEquals(7, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(8, rs.getInt("id"))
                    assertEquals("Michael Wright", rs.getString("name"))
                    assertEquals(8, rs.getInt("creator_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(9, rs.getInt("id"))
                    assertEquals("Grant T. Morrison", rs.getString("name"))
                    assertEquals(1, rs.getInt("creator_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(10, rs.getInt("id"))
                    assertEquals("Neil Richard Gaiman", rs.getString("name"))
                    assertEquals(9, rs.getInt("creator_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(11, rs.getInt("id"))
                    assertEquals("Jonathan Hickman", rs.getString("name"))
                    assertEquals(10, rs.getInt("creator_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            // gcd_story_credit
            fun verifyStoryCreditHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.gcd_story_credit ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals(2, rs.getInt("creator_id"))
                    assertEquals(2, rs.getInt("credit_type_id"))
                    assertEquals(1, rs.getInt("story_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals(3, rs.getInt("creator_id"))
                    assertEquals(2, rs.getInt("credit_type_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(3, rs.getInt("id"))
                    assertEquals(4, rs.getInt("creator_id"))
                    assertEquals(3, rs.getInt("credit_type_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(4, rs.getInt("id"))
                    assertEquals(5, rs.getInt("creator_id"))
                    assertEquals(4, rs.getInt("credit_type_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(5, rs.getInt("id"))
                    assertEquals(6, rs.getInt("creator_id"))
                    assertEquals(5, rs.getInt("credit_type_id"))
                    assertEquals(2, rs.getInt("story_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(11, rs.getInt("id"))
                    assertEquals(4, rs.getInt("creator_id"))
                    assertEquals(1, rs.getInt("credit_type_id"))
                    assertEquals(8, rs.getInt("story_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(12, rs.getInt("id"))
                    assertEquals(3, rs.getInt("creator_id"))
                    assertEquals(5, rs.getInt("credit_type_id"))
                    assertEquals(9, rs.getInt("story_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            // gcd_reprint
            fun verifyReprintsHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo("SELECT * FROM $TEST_DATABASE.gcd_reprint ORDER BY id", connection ?: conn) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals(1, rs.getInt("origin_id"))
                    assertEquals(2, rs.getInt("target_id"))
                    assertEquals(1, rs.getInt("origin_issue_id"))
                    assertEquals(2, rs.getInt("target_issue_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals(2, rs.getInt("origin_id"))
                    assertEquals(1, rs.getInt("target_id"))
                    assertEquals(2, rs.getInt("origin_issue_id"))
                    assertEquals(1, rs.getInt("target_issue_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(8, rs.getInt("id"))
                    assertEquals(1, rs.getInt("origin_id"))
                    assertEquals(9, rs.getInt("target_id"))
                    assertEquals(1, rs.getInt("origin_issue_id"))
                    assertEquals(2, rs.getInt("target_issue_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(12, rs.getInt("id"))
                    assertEquals(1, rs.getInt("origin_id"))
                    assertEquals(2, rs.getInt("target_id"))
                    assertEquals(9, rs.getInt("origin_issue_id"))
                    assertEquals(10, rs.getInt("target_issue_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            // gcd_issue_credit
            fun verifyIssueCreditsHaveBeenMigrated() {
                queryExecutor.executeQueryAndDo(
                    "SELECT * FROM $TEST_DATABASE.gcd_issue_credit ORDER BY id",
                    connection ?: conn                ) { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("id"))
                    assertEquals(1, rs.getInt("creator_id"))
                    assertEquals(1, rs.getInt("credit_type_id"))
                    assertEquals(1, rs.getInt("issue_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(2, rs.getInt("id"))
                    assertEquals(2, rs.getInt("creator_id"))
                    assertEquals(2, rs.getInt("credit_type_id"))
                    assertEquals(1, rs.getInt("issue_id"))
                    assertEquals("2004-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(8, rs.getInt("id"))
                    assertEquals(8, rs.getInt("creator_id"))
                    assertEquals(2, rs.getInt("credit_type_id"))
                    assertEquals(9, rs.getInt("issue_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertTrue(rs.next())
                    assertEquals(9, rs.getInt("id"))
                    assertEquals(7, rs.getInt("creator_id"))
                    assertEquals(3, rs.getInt("credit_type_id"))
                    assertEquals(10, rs.getInt("issue_id"))
                    assertEquals("2023-06-01 19:56:37", rs.getString("modified"))
                    assertFalse(rs.next())
                }
            }

            verifyStddataCountryHaveBeenMigrated()
            verifyStddataLanguageHaveBeenMigrated()
            verifyStddataDateHaveBeenMigrated()
            verifySeriesPublicationTypeHaveBeenMigrated()
            verifyBrandHaveBeenMigrated()
            verifyStoryTypeHaveBeenMigrated()
            verifyNameTypeHaveBeenMigrated()
            verifyScriptHaveBeenMigrated()
            verifyCreatorSignatureHaveBeenMigrated()

            verifyPublishersHaveBeenMigrated()
            verifySeriesHaveBeenMigrated()
            verifyIndiciaPublishersHaveBeenMigrated()
            verifyIssuesHaveBeenMigrated()
            verifySeriesBondsHaveBeenMigrated()
            verifyStoriesHaveBeenMigrated()
            verifyStoryCreditHaveBeenMigrated()
            verifyCreatorsHaveBeenMigrated()
            verifyCreatorNameDetailsHaveBeenMigrated()
            verifyReprintsHaveBeenMigrated()
            verifyIssueCreditsHaveBeenMigrated()
        }

        /**
         * Verify that issue/series id columns have been added to gcd_story_credit,
         * m_story_credit, and m_character_appearance tables
         */
        private fun verifyIssueAndSeriesColumnsExist(queryExecutor: QueryExecutor, connection: Connection? = null) {
            val query = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = '$TEST_DATABASE_UPDATE'
                AND table_name IN ('gcd_story_credit', 'm_story_credit', 'm_character_appearance')
                AND column_name IN ('issue_id', 'series_id')
            """.trimIndent()

            queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
                assertTrue(rs.next())
                assertEquals(6, rs.getInt(1))
            }
        }

        /**  */
        private fun verifyIssueAndSeriesColumnsValues(queryExecutor: QueryExecutor, connection: Connection? = null) {
            fun query(table: String): String {
                return """SELECT * 
                        |FROM $TEST_DATABASE_UPDATE.$table 
                        |WHERE issue_id IS NOT NULL OR series_id IS NOT NULL
                        |""".trimMargin()
            }

            queryExecutor.executeQueryAndDo(query("gcd_story_credit"), connection ?: conn) { rs ->
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

            queryExecutor.executeQueryAndDo(query("m_story_credit"), connection ?: conn) { rs ->
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

            queryExecutor.executeQueryAndDo(query("m_character_appearance"), connection ?: conn) { rs ->
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
                assertEquals(6, rs.getInt("id"))
                assertEquals(1, rs.getInt("issue_id"))
                assertEquals(1, rs.getInt("series_id"))
                assertTrue(rs.next())
                assertEquals(7, rs.getInt("id"))
                assertEquals(9, rs.getInt("issue_id"))
                assertEquals(1, rs.getInt("series_id"))
                assertTrue(rs.next())
                assertEquals(8, rs.getInt("id"))
                assertEquals(9, rs.getInt("issue_id"))
                assertEquals(1, rs.getInt("series_id"))
                assertFalse(rs.next())
            }
        }

        /** verifies that good tables contain the expected records */
        private fun verifyGoodTables(queryExecutor: QueryExecutor, connection: Connection? = null) {
            fun verifyGoodPublishers() {
                val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.good_publishers
                ORDER BY id
            """.trimIndent()

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) {
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) {
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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
        private fun verifyMigrateTables(queryExecutor: QueryExecutor, connection: Connection? = null) {
            fun verifyMigratePublishers() {
                val query = """
                SELECT * 
                FROM $TEST_DATABASE_UPDATE.migrate_publishers
                ORDER BY id
            """.trimIndent()

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) {
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) {
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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

                queryExecutor.executeQueryAndDo(query, connection ?: conn) { rs ->
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