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
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.sql.Connection

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DBInitAddTablesTest {

    private lateinit var dbInit: DBInitAddTables
    private val queryExecutor = QueryExecutor()

    @BeforeEach
    fun setUp() {
        dbInit = DBInitAddTables(queryExecutor, TEST_DATABASE, conn)
    }

    @Test
    @Order(1)
    @DisplayName("should call each function once when addTablesAndConstraints is called")
    fun callEachFunctionOnce() {
        val dbInitAddTablesMock = spy(DBInitAddTables(queryExecutor, TEST_DATABASE, conn))

        // Mock the functions in addTablesAndConstraints
        doNothing().whenever(dbInitAddTablesMock).addIssueColumnIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addIssueIdForeignKeyIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addSeriesColumnIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addSeriesIdForeignKeyIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).createExtractedCharactersTableIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).createCharacterAppearancesTableIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).createExtractedStoryCreditsTableIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists()
        doNothing().whenever(dbInitAddTablesMock).addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists()

        dbInitAddTablesMock.addTablesAndConstraints()

        // verify each function was called once
        verify(dbInitAddTablesMock).addTablesAndConstraints()
        verify(dbInitAddTablesMock).addIssueColumnIfNotExists()
        verify(dbInitAddTablesMock).addIssueIdForeignKeyIfNotExists()
        verify(dbInitAddTablesMock).addSeriesColumnIfNotExists()
        verify(dbInitAddTablesMock).addSeriesIdForeignKeyIfNotExists()
        verify(dbInitAddTablesMock).createExtractedCharactersTableIfNotExists()
        verify(dbInitAddTablesMock).createCharacterAppearancesTableIfNotExists()
        verify(dbInitAddTablesMock).createExtractedStoryCreditsTableIfNotExists()
        verify(dbInitAddTablesMock).addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists()
        verify(dbInitAddTablesMock).addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists()
        verify(dbInitAddTablesMock).addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists()
        verify(dbInitAddTablesMock).addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists()
        verify(dbInitAddTablesMock).addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists()
        verify(dbInitAddTablesMock).addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists()

        // verify no other functions were called
        verifyNoMoreInteractions(dbInitAddTablesMock)
    }

    @Test
    @Order(1)
    @DisplayName("should add issue_id column to gcd_story_credit table if it does not exist")
    fun addIssueColumnIfNotExists() {
        // verify there is no issue_id column
        val query = """SELECT column_name
            |FROM information_schema.columns 
            |WHERE table_name = 'gcd_story_credit' 
            |AND column_name = 'issue_id' 
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the column
        dbInit.addIssueColumnIfNotExists()

        // verify the column was added
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("issue_id", it.getString("column_name"))
        }

        // add the column again
        dbInit.addIssueColumnIfNotExists()

        // verify the column was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("issue_id", it.getString("column_name"))
            assertFalse(it.next())
        }
    }

    @Test
    @Order(1)
    @DisplayName("should add series_id column to gcd_story_credit table if it does not exist")
    fun addSeriesColumnIfNotExists() {
        // verify there is no series_id column
        val query =
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'gcd_story_credit' AND column_name = 'series_id' AND table_schema = '$TEST_DATABASE'"
        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the column
        dbInit.addSeriesColumnIfNotExists()

        // verify the column was added
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("series_id", it.getString("column_name"))
        }

        // add the column again
        dbInit.addSeriesColumnIfNotExists()

        // verify the column was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("series_id", it.getString("column_name"))
            assertFalse(it.next())
        }
    }

    @Test
    @Order(1)
    @DisplayName("should add m_character table if it does not exist")
    fun createExtractedCharactersTableIfNotExists() {
        // verify there is no m_character table
        val query =
            """SELECT table_name 
                |FROM information_schema.tables 
                |WHERE table_name = 'm_character' 
                |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the table
        dbInit.createExtractedCharactersTableIfNotExists()

        // verify the table was added
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("m_character", it.getString("table_name"))
        }

        // add the table again
        dbInit.createExtractedCharactersTableIfNotExists()

        // verify the table was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("m_character", it.getString("table_name"))
            assertFalse(it.next())
        }
    }

    @Test
    @Order(2)
    @DisplayName("should create m_story_credit table if it does not exist")
    fun createExtractedStoryCreditsTableIfNotExists() {
        // verify there is no m_story_credit table
        val query =
            "SELECT table_name FROM information_schema.tables WHERE table_name = 'm_story_credit' AND table_schema = '$TEST_DATABASE'"
        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the table
        dbInit.createExtractedStoryCreditsTableIfNotExists()

        // verify the table was added
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("m_story_credit", it.getString("table_name"))
        }

        // add the table again
        dbInit.createExtractedStoryCreditsTableIfNotExists()

        // verify the table was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("m_story_credit", it.getString("table_name"))
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should add series_id foreign key to gcd_story_credit table if it does not exist")
    fun addSeriesIdForeignKeyIfNotExists() {
        // verify there is no series_id foreign key
        val query =
            """SELECT constraint_name
                |FROM information_schema.key_column_usage 
                |WHERE table_schema = '$TEST_DATABASE' 
                |AND table_name = 'gcd_story_credit' 
                |AND column_name = 'series_id'
                |AND referenced_table_name = 'gcd_series'
                |AND referenced_column_name = 'id'""".trimMargin()
        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the foreign key
        dbInit.addSeriesIdForeignKeyIfNotExists()

        // verify the foreign key was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the foreign key again
        dbInit.addSeriesIdForeignKeyIfNotExists()

        // verify the foreign key was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_character_appearance table if it does not exist")
    fun createCharacterAppearanceTableIfNotExists() {
        // verify there is no m_character_appearance table
        val query =
            "SELECT table_name FROM information_schema.tables WHERE table_name = 'm_character_appearance' AND table_schema = '$TEST_DATABASE'"

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the table
        dbInit.createCharacterAppearancesTableIfNotExists()

        // verify the table was added
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("m_character_appearance", it.getString("table_name"))
        }

        // add the table again
        dbInit.createCharacterAppearancesTableIfNotExists()

        // verify the table was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertEquals("m_character_appearance", it.getString("table_name"))
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should add issue_id foreign key to gcd_story_credit table if it does not exist")
    fun addIssueIdForeignKeyIfNotExists() {
        // verify there is no issue_id foreign key
        val query =
            """SELECT constraint_name
                |FROM information_schema.key_column_usage 
                |WHERE table_schema = '$TEST_DATABASE' 
                |AND table_name = 'gcd_story_credit' 
                |AND column_name = 'issue_id'
                |AND referenced_table_name = 'gcd_issue'
                |AND referenced_column_name = 'id'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the foreign key
        dbInit.addIssueIdForeignKeyIfNotExists()

        // verify the foreign key was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the foreign key again
        dbInit.addIssueIdForeignKeyIfNotExists()

        // verify the foreign key was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_story_credit primary key constraint if it does not exist")
    fun addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists() {
        // verify there is one or none m_story_credit primary key constraint
        val query = """SELECT constraint_name 
            |FROM information_schema.table_constraints 
            |WHERE table_name = 'm_story_credit' 
            |AND constraint_type = 'PRIMARY KEY' 
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        // check that there is one or less primary key constraints
        queryExecutor.executeQueryAndDo(query, conn) {
            it.next()
            assertFalse(it.next())
        }

        // add the constraint
        dbInit.addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists()

        // verify the constraint was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the constraint again
        dbInit.addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists()

        // verify the constraint was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_story_credit creator_id foreign key constraint if it does not exist")
    fun addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists() {
        // verify there is no m_story_credit creator_id foreign key constraint
        val query = """SELECT constraint_name 
            |FROM information_schema.key_column_usage
            |WHERE table_name = 'm_story_credit'
            |AND column_name = 'creator_id'
            |AND referenced_table_name = 'gcd_creator_name_detail'
            |AND referenced_column_name = 'id'
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the constraint
        dbInit.addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists()

        // verify the constraint was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the constraint again
        dbInit.addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists()

        // verify the constraint was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_story_credit credit_type_id foreign key constraint if it does not exist")
    fun addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists() {
        // verify there is no m_story_credit credit_type_id foreign key constraint
        val query = """SELECT constraint_name 
            |FROM information_schema.key_column_usage
            |WHERE table_name = 'm_story_credit'
            |AND column_name = 'credit_type_id'
            |AND referenced_table_name = 'gcd_credit_type'
            |AND referenced_column_name = 'id'
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the constraint
        dbInit.addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists()

        // verify the constraint was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the constraint again
        dbInit.addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists()

        // verify the constraint was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_story_credit story_id foreign key constraint if it does not exist")
    fun addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists() {
        // verify there is no m_story_credit story_id foreign key constraint
        val query = """SELECT constraint_name 
            |FROM information_schema.key_column_usage
            |WHERE table_name = 'm_story_credit'
            |AND column_name = 'story_id'
            |AND referenced_table_name = 'gcd_story'
            |AND referenced_column_name = 'id'
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the constraint
        dbInit.addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists()

        // verify the constraint was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the constraint again
        dbInit.addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists()

        // verify the constraint was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_story_credit issue_id foreign key constraint if it does not exist")
    fun addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists() {
        // verify there is no m_story_credit issue_id foreign key constraint
        val query = """SELECT constraint_name 
            |FROM information_schema.key_column_usage
            |WHERE table_name = 'm_story_credit'
            |AND column_name = 'issue_id'
            |AND referenced_table_name = 'gcd_issue'
            |AND referenced_column_name = 'id'
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the constraint
        dbInit.addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists()

        // verify the constraint was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the constraint again
        dbInit.addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists()

        // verify the constraint was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(3)
    @DisplayName("should create m_story_credit series_id foreign key constraint if it does not exist")
    fun addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists() {
        // verify there is no m_story_credit series_id foreign key constraint
        val query = """SELECT constraint_name 
            |FROM information_schema.key_column_usage
            |WHERE table_name = 'm_story_credit'
            |AND column_name = 'series_id'
            |AND referenced_table_name = 'gcd_series'
            |AND referenced_column_name = 'id'
            |AND table_schema = '$TEST_DATABASE'""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertFalse(it.next())
        }

        // add the constraint
        dbInit.addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists()

        // verify the constraint was added
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // add the constraint again
        dbInit.addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists()

        // verify the constraint was not added again
        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertFalse(it.next())
        }
    }

    @Test
    @Order(4)
    @DisplayName("should add tables and constraints")
    fun shouldAddTablesAndConstraints() {
        TestDatabaseSetup.setup(DBState.INIT_STEP_1B, schema = TEST_DATABASE, sourceSchema = null)
        dbInit.addTablesAndConstraints()

        // verify the m_character, m_character_appearance, and m_story_credit tables were added
        val query = """SELECT table_name 
            |FROM information_schema.tables 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name IN ('m_character', 'm_character_appearance', 'm_story_credit')""".trimMargin()

        queryExecutor.executeQueryAndDo(query, conn) {
            assertTrue(it.next())
            assertTrue(it.next())
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // verify gcd_story_credit has series_id and issue_id columns
        val query2 = """SELECT column_name 
            |FROM information_schema.columns 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'gcd_story_credit' 
            |AND column_name IN ('series_id', 'issue_id')""".trimMargin()

        queryExecutor.executeQueryAndDo(query2, conn) {
            assertTrue(it.next())
            assertTrue(it.next())
            assertFalse(it.next())
        }

        // verify fkey constraints on series_id and issue_id
        val query3 = """SELECT column_name, referenced_table_name 
            |FROM information_schema.key_column_usage 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'gcd_story_credit' 
            |AND column_name IN ('series_id', 'issue_id') 
            |AND referenced_table_name IN ('gcd_series', 'gcd_issue')
            |ORDER BY column_name, referenced_table_name""".trimMargin()

        queryExecutor.executeQueryAndDo(query3, conn) {
            assertTrue(it.next())
            assertEquals("issue_id", it.getString("column_name"))
            assertEquals("gcd_issue", it.getString("referenced_table_name"))
            assertTrue(it.next())
            assertEquals("series_id", it.getString("column_name"))
            assertEquals("gcd_series", it.getString("referenced_table_name"))
            assertFalse(it.next())
        }

        // verify m_characters table was created
        val query4 = """SELECT COUNT(*) 
            |FROM information_schema.columns 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'm_character' 
            |AND column_name IN ('id', 'name', 'alter_ego', 'publisher_id')""".trimMargin()

        queryExecutor.executeQueryAndDo(query4, conn) {
            assertTrue(it.next())
            assertEquals(4, it.getInt(1))
        }

        // verify m_character_appearance table was created
        val query5 = """SELECT COUNT(*) 
            |FROM information_schema.columns 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'm_character_appearance' 
            |AND column_name IN ('id', 'details', 'character_id', 'story_id', 'notes', 'membership', 'issue_id', 'series_id')""".trimMargin()

        queryExecutor.executeQueryAndDo(query5, conn) {
            assertTrue(it.next())
            assertEquals(8, it.getInt(1))
        }

        // verify m_story_credit table was created and has the same columns & constraints as gcd_story_credit
        val query6 = """SELECT COUNT(*) 
            |FROM information_schema.columns 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'm_story_credit' 
            |AND column_name IN ('id', 'creator_id', 'credit_type_id', 'story_id', 'issue_id', 'series_id')""".trimMargin()

        queryExecutor.executeQueryAndDo(query6, conn) {
            assertTrue(it.next())
            assertEquals(6, it.getInt(1))
        }

        val query7 = """SELECT column_name, referenced_table_name 
            |FROM information_schema.key_column_usage 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'm_story_credit' 
            |AND column_name IN ('series_id', 'issue_id', 'story_id', 'creator_id', 'credit_type_id') 
            |AND referenced_table_name IN ('gcd_series', 'gcd_issue', 'gcd_story', 'gcd_creator_name_detail', 'gcd_credit_type')
            |ORDER BY column_name, referenced_table_name""".trimMargin()

        queryExecutor.executeQueryAndDo(query7, conn) {
            assertTrue(it.next())
            assertEquals("creator_id", it.getString("column_name"))
            assertEquals("gcd_creator_name_detail", it.getString("referenced_table_name"))
            assertTrue(it.next())
            assertEquals("credit_type_id", it.getString("column_name"))
            assertEquals("gcd_credit_type", it.getString("referenced_table_name"))
            assertTrue(it.next())
            assertEquals("issue_id", it.getString("column_name"))
            assertEquals("gcd_issue", it.getString("referenced_table_name"))
            assertTrue(it.next())
            assertEquals("series_id", it.getString("column_name"))
            assertEquals("gcd_series", it.getString("referenced_table_name"))
            assertTrue(it.next())
            assertEquals("story_id", it.getString("column_name"))
            assertEquals("gcd_story", it.getString("referenced_table_name"))
            assertFalse(it.next())
        }

        // verify m_story_credit has primary key constraint on id
        val query9 = """SELECT COUNT(*) 
            |FROM information_schema.table_constraints 
            |WHERE table_schema = '$TEST_DATABASE' 
            |AND table_name = 'm_story_credit' 
            |AND constraint_type = 'PRIMARY KEY'""".trimMargin()

        queryExecutor.executeQueryAndDo(query9, conn) {
            assertTrue(it.next())
            assertEquals(1, it.getInt(1))
        }
    }

    companion object {
        private lateinit var conn: Connection

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            conn = TestDatabaseSetup.getTestDbConnection()
            TestDatabaseSetup.setup(dbState = DBState.INIT_STEP_1B, schema = TEST_DATABASE, sourceSchema = null)
        }

        @AfterAll
        @JvmStatic
        fun teardownAll() {
            TestDatabaseSetup.teardown(schema = TEST_DATABASE, conn = conn)
            conn.close()
        }
    }
}