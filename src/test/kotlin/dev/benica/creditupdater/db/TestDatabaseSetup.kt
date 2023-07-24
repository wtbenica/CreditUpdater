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

package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.TEST_DATABASE
import dev.benica.creditupdater.Credentials.TEST_DATABASE_UPDATE
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.INIT_CREATE_BAD_VIEWS
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.ISSUE_SERIES_PATH
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class TestDatabaseSetup {
    @Test
    fun test() {
        setup(
            dbState = DBState.INITIALIZED,
            schema = TEST_DATABASE,
            sourceSchema = null
        )
        setup(
            dbState = DBState.MIGRATE_STEP_3_COMPLETE,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )
    }

    companion object {
        /**
         * Creates a base database
         *
         * @param dbState the state of the database to create
         * @param schema the name of the database to create
         */
        @JvmStatic
        fun setup(
            dbState: DBState = DBState.INITIALIZED,
            schema: String,
            sourceSchema: String?
        ) {
            val testConn: Connection = getDbConnection(schema)
            var updateConn: Connection? = null

            if (sourceSchema == null) {
                dropAllTablesAndViews(schema = schema, conn = testConn)
                createTestDatabaseTables(schema = schema, conn = testConn)
            } else {
                updateConn = getDbConnection(targetSchema = sourceSchema)
                dropAllTablesAndViews(schema = sourceSchema, conn = updateConn)
                createTestDatabaseTables(schema = sourceSchema, conn = updateConn)
            }

            when (dbState) {
                DBState.INITIAL -> {
                    addUnusedTables(schema, testConn)
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                }

                DBState.INIT_STEP_1A -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                }

                DBState.INIT_STEP_1B -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                }

                DBState.INIT_STEP_1C -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(targetSchema = schema, conn = testConn)
                    populateTestDatabaseBad(targetSchema = schema, conn = testConn)
                    dropSourcedColumns(targetSchema = schema, conn = testConn)
                    createInitExtractedTables(targetSchema = schema, conn = testConn)
                }

                DBState.INIT_STEP_1D -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createInitExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                }

                DBState.INIT_STEP_1_COMPLETE -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createInitExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                }

                DBState.INIT_STEP_2_COMPLETE -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createInitExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                    populateCharacterTables(schema, testConn)
                }

                DBState.INIT_STEP_3_COMPLETE -> {
                    addStaticTables(schema, testConn)
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createInitExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                    populateCharacterTables(schema, testConn)
                    populateCreditTables(schema, testConn)
                }

                DBState.INITIALIZED -> {
                    addStaticTables(targetSchema = schema, conn = testConn)
                    populateTestDatabaseGood(targetSchema = schema, conn = testConn)
                    dropSourcedColumns(targetSchema = schema, conn = testConn)
                    createInitExtractedTables(targetSchema = schema, conn = testConn)
                    createBadViews(targetSchema = schema, conn = testConn)
                    populateCharacterTables(targetSchema = schema, conn = testConn)
                    populateCreditTables(targetSchema = schema, conn = testConn)
                    addIssueSeriesId(targetSchema = schema, conn = testConn)
                }

                DBState.MIGRATE_INITIAL -> {
                    if (sourceSchema != null) {
                        addStaticTables(targetSchema = sourceSchema, conn = updateConn!!)
                        populateTestDatabaseGood(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseBad(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseUpdated(targetSchema = sourceSchema, conn = updateConn)
                        addUnusedTables(targetSchema = sourceSchema, conn = updateConn)
                    }
                }

                DBState.MIGRATE_STEP_1_COMPLETE -> {
                    if (sourceSchema != null) {
                        addStaticTables(targetSchema = sourceSchema, conn = updateConn!!)
                        dropSourcedColumns(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseGood(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseBad(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseUpdated(targetSchema = sourceSchema, conn = updateConn)
                        addUnusedTables(targetSchema = sourceSchema, conn = updateConn)
                        createMigrateExtractedTables(
                            targetSchema = schema,
                            sourceSchema = sourceSchema,
                            conn = updateConn
                        )
                    }
                }

                DBState.MIGRATE_STEP_2_COMPLETE -> {
                    if (sourceSchema != null) {
                        addStaticTables(targetSchema = sourceSchema, conn = updateConn!!)
                        dropSourcedColumns(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseGood(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseBad(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseUpdated(targetSchema = sourceSchema, conn = updateConn)
                        addUnusedTables(targetSchema = sourceSchema, conn = updateConn)
                        createMigrateExtractedTables(
                            targetSchema = schema,
                            sourceSchema = sourceSchema,
                            conn = updateConn
                        )
                        populateTestDatabaseCharacters(schema = sourceSchema, conn = updateConn)
                    }
                }

                DBState.MIGRATE_STEP_3_COMPLETE -> {
                    if (sourceSchema != null) {
                        addStaticTables(targetSchema = sourceSchema, conn = updateConn!!)
                        dropSourcedColumns(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseGood(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseBad(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseUpdated(targetSchema = sourceSchema, conn = updateConn)
                        addUnusedTables(targetSchema = sourceSchema, conn = updateConn)
                        createMigrateExtractedTables(
                            targetSchema = schema,
                            sourceSchema = sourceSchema,
                            conn = updateConn
                        )
                        populateTestDatabaseCharacters(schema = sourceSchema, conn = updateConn)
                        populateTestDatabaseCredits(schema = sourceSchema, conn = updateConn)
                    }
                }

                DBState.MIGRATE_STEP_4_COMPLETE -> {
                    if (sourceSchema != null) {
                        addStaticTables(targetSchema = sourceSchema, conn = updateConn!!)
                        dropSourcedColumns(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseGood(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseBad(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseUpdated(targetSchema = sourceSchema, conn = updateConn)
                        addUnusedTables(targetSchema = sourceSchema, conn = updateConn)
                        createMigrateExtractedTables(
                            targetSchema = schema,
                            sourceSchema = sourceSchema,
                            conn = updateConn
                        )
                        populateTestDatabaseCharacters(schema = sourceSchema, conn = updateConn)
                        populateTestDatabaseCredits(schema = sourceSchema, conn = updateConn)
                        addIssueSeriesIdNew(targetSchema = sourceSchema, conn = updateConn)
                    }
                }

                DBState.MIGRATED -> {
                    if (sourceSchema != null) {
                        addStaticTables(targetSchema = sourceSchema, conn = updateConn!!)
                        addUnusedTables(targetSchema = sourceSchema, conn = updateConn)
                        dropSourcedColumns(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseGood(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseBad(targetSchema = sourceSchema, conn = updateConn)
                        populateTestDatabaseUpdated(targetSchema = sourceSchema, conn = updateConn)
                        createMigrateExtractedTables(
                            targetSchema = schema,
                            sourceSchema = sourceSchema,
                            conn = updateConn
                        )
                        populateTestDatabaseCharacters(schema = sourceSchema, conn = updateConn)
                        populateTestDatabaseCredits(schema = sourceSchema, conn = updateConn)
                        addIssueSeriesIdNew(targetSchema = sourceSchema, conn = updateConn)
                        // migrate records
                    }
                }
            }
        }

        @JvmStatic
        fun teardown(schema: String, conn: Connection) {
            dropAllTablesAndViews(schema, conn)
        }

        fun getDbConnection(targetSchema: String): Connection =
            ConnectionProvider.getConnection(targetSchema).connection

        fun getTestDbConnection(): Connection = getDbConnection(TEST_DATABASE)

        // test setup sql scripts
        /**
         * Target Schema:
         * - Creates empty, minimal versions of the tables that are used both in
         *   the GCD and Infinite Longbox.
         */
        private const val SETUP_DATABASE = "src/main/resources/sql/test_setup_database.sql"

        /**
         * Target Schema:
         * - Adds tables that are not referenced in any of the other tables used by
         *   Infinite Longbox. All are safe to delete.
         */
        private const val ADD_UNUSED_TABLES = "src/main/resources/sql/test_setup_add_unused_tables.sql"

        /**
         * Target Schema:
         * - Inserts a set of records to gcd_publisher, gcd_series, gcd_issue,
         *   gcd_series_bond, gcd_story, gcd_credit_type, gcd_creator,
         *   gcd_creator_name_detail, gcd_story_credit, gcd_reprint,
         *   gcd_issue_credit and gcd_indicia_publisher tables that meet criteria
         *   for Infinite Longbox.
         */
        private const val POPULATE_GOOD = "src/main/resources/sql/test_setup_populate_good.sql"

        /**
         * Target Schema:
         * - Inserts a set of records to gcd_publisher, gcd_series,
         *   gcd_indicia_publisher, gcd_issue, gcd_series_bond, gcd_story,
         *   gcd_story_credit, gcd_reprint, and gcd_issue_credit tables that do not
         *   meet the criteria for Infinite Longbox.
         */
        private const val POPULATE_BAD = "src/main/resources/sql/test_setup_populate_bad.sql"

        /**
         * Target Schema:
         * - Adds issue and series columns to gcd_story_credit.
         * - Creates m_character, m_character_appearance, and m_story_credit
         *   tables.
         */
        private const val CREATE_INIT_EXTRACTED_TABLES =
            "src/main/resources/sql/test_setup_create_init_extracted_tables.sql"

        /**
         * Target Schema:
         * - Inserts the values that would be extracted from the GCD into the
         *   m_character and m_character_appearance tables.
         */
        private const val POPULATE_CHARACTER_TABLES =
            "src/main/resources/sql/test_setup_init_populate_character_tables.sql"

        /**
         * Target Schema:
         * - Inserts the values that would be extracted from the GCD into the
         *   m_story_credit table.
         */
        private const val POPULATE_CREDIT_TABLE = "src/main/resources/sql/test_setup_init_populate_credits.sql"

        /**
         * Target Schema:
         * - Drops, creates, and populates stddata_country, stddata_language,
         *   stddata_date, gcd_series_publication_type, gcd_brand, gcd_story_type,
         *   gcd_name_type, stddata_script, gcd_creator_signature,
         */
        private const val ADD_STATIC_TABLES = "src/main/resources/sql/test_setup_add_static_tables.sql"

        /**
         * Target Schema:
         * - Populates the database with a mixture of good and bad records.
         *   Intended to reflect a newer version of the GCD.
         */
        private const val TEST_POPULATE_UPDATED = "src/main/resources/sql/test_setup_populate_updated.sql"

        /**
         * Target Schema:
         * - Adds issue and series columns to gcd_story_credit.
         * - Creates m_character, m_character_appearance, and m_story_credit
         *   tables.
         *
         * Source Schema:
         * - Drops and Creates good_publishers, good_series, good_issue,
         *   good_story, and good_indicia_publishers views.
         * - Drops and Creates migrate_publishers, migrate_series, migrate_issues,
         *   migrate_stories, and migrate_indicia_publishers views.
         */
        private const val TEST_CREATE_MIGRATE_EXTRACTED_TABLES =
            "src/main/resources/sql/test_setup_migrate_create_extracted_tables.sql"

        /**
         * Target Schema:
         * - Populates the m_character and m_character_appearance tables with
         *   characters that would be extracted from the migrate_stories table.
         */
        private const val TEST_MIGRATE_POPULATE_CHARACTERS =
            "src/main/resources/sql/test_setup_migrate_populate_character_tables.sql"

        /**
         * Target Schema:
         * - Populates the m_story_credit table with credits that would be
         *   extracted from the migrate_stories table.
         */
        private const val TEST_MIGRATE_POPULATE_CREDITS =
            "src/main/resources/sql/test_setup_migrate_populate_credits.sql"

        /**
         * Target Schema:
         * - Sets issue and series ids in gcd_story, gcd_story_credit,
         *   migrate_story_credits, m_story_credit, and m_character_appearance for
         *   stories in migrate_stories
         */
        private const val MIGRATE_FILL_ID_COLUMNS = "src/main/resources/sql/migrate_fill_id_columns.sql"

        // Private functions
        /** Drops all tables and views in database [schema] */
        internal fun dropAllTablesAndViews(schema: String, conn: Connection) {
            try {
                conn.createStatement().use { stmt ->
                    // Disable foreign key checks
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 0")

                    // Drop all views and tables
                    val query = """
                    SELECT CONCAT('DROP TABLE IF EXISTS {{targetSchema}}.', table_name, ';') AS statement
                    FROM information_schema.tables
                    WHERE table_schema = '{{targetSchema}}'
                    UNION
                    SELECT CONCAT('DROP VIEW IF EXISTS {{targetSchema}}.', table_name, ';') AS statement
                    FROM information_schema.views
                    WHERE table_schema = '{{targetSchema}}'
                """.trimIndent()

                    stmt.executeQuery(query.replace("{{targetSchema}}", schema)).use { rs ->
                        while (rs.next()) {
                            val statement = rs.getString("statement")
                            conn.createStatement().use { stmt2 ->
                                stmt2.execute(statement)
                            }
                        }

                    }

                    // Enable foreign key checks
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                throw e
            }
        }

        // script executors
        /**
         * Executes [SETUP_DATABASE]
         *
         * Target Schema:
         * - Creates empty, minimal versions of the tables that are used both in
         *   the GCD and Infinite Longbox.
         */
        private fun createTestDatabaseTables(schema: String, conn: Connection) {
            executeScript(
                targetSchema = schema,
                sourceSchema = null,
                sqlScript = SETUP_DATABASE,
                conn = conn
            )
        }

        /**
         * Executes [ADD_UNUSED_TABLES]
         *
         * Target Schema:
         * - Adds tables that are not referenced in any of the other tables used by
         *   Infinite Longbox. All are safe to delete.
         */
        private fun addUnusedTables(
            targetSchema: String,
            conn: Connection
        ) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = ADD_UNUSED_TABLES,
                conn = conn
            )

        /**
         * Executes [POPULATE_GOOD]
         *
         * Target Schema:
         * - Inserts a set of records to gcd_publisher, gcd_series, gcd_issue,
         *   gcd_series_bond, gcd_story, gcd_credit_type, gcd_creator,
         *   gcd_creator_name_detail, gcd_story_credit, gcd_reprint,
         *   gcd_issue_credit and gcd_indicia_publisher tables that meet criteria
         *   for Infinite Longbox.
         */
        private fun populateTestDatabaseGood(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = POPULATE_GOOD,
                conn = conn
            )

        /**
         * Executes [POPULATE_BAD]
         *
         * Target Schema:
         * - Inserts a set of records to gcd_publisher, gcd_series,
         *   gcd_indicia_publisher, gcd_issue, gcd_series_bond, gcd_story,
         *   gcd_story_credit, gcd_reprint, and gcd_issue_credit tables that do not
         *   meet the criteria for Infinite Longbox.
         */
        private fun populateTestDatabaseBad(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = POPULATE_BAD,
                conn = conn
            )

        // Initialize
        /**
         * Target Schema:
         * - Drops the is_sourced and sourced_by columns from
         *   [targetSchema].gcd_story_credit.
         */
        private fun dropSourcedColumns(targetSchema: String, conn: Connection) {
            conn.createStatement().use { stmt ->
                val sqlStmt = """ALTER TABLE $targetSchema.gcd_story_credit
                                DROP COLUMN IF EXISTS is_sourced,
                                DROP COLUMN IF EXISTS sourced_by;
                            """.trimIndent()

                stmt.execute(sqlStmt)
            }
        }

        /**
         * Executes [CREATE_INIT_EXTRACTED_TABLES]
         *
         * Target Schema:
         * - Adds issue and series columns to gcd_story_credit.
         * - Creates m_character, m_character_appearance, and m_story_credit
         *   tables.
         */
        private fun createInitExtractedTables(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = CREATE_INIT_EXTRACTED_TABLES,
                conn = conn
            )

        /**
         * Executes [INIT_CREATE_BAD_VIEWS]
         *
         * Target Schema:
         * - Creates views `bad_publishers`, `bad_indicia_publishers`,
         *   `bad_series`, `bad_issues`, and `bad_stories`.
         */
        private fun createBadViews(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = INIT_CREATE_BAD_VIEWS,
                conn = conn
            )

        /**
         * Executes [POPULATE_CHARACTER_TABLES]
         *
         * Target Schema:
         * - Inserts the values that would be extracted from the GCD into the
         *   m_character and m_character_appearance tables.
         */
        private fun populateCharacterTables(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = POPULATE_CHARACTER_TABLES,
                conn = conn
            )

        /**
         * Executes [POPULATE_CREDIT_TABLE]
         *
         * Target Schema:
         * - Inserts the values that would be extracted from the GCD into the
         *   m_story_credit table.
         */
        private fun populateCreditTables(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = POPULATE_CREDIT_TABLE,
                conn = conn
            )

        /**
         * Executes [ADD_STATIC_TABLES]
         *
         * Target Schema:
         * - Drops, creates, and populates stddata_country, stddata_language,
         *   stddata_date, gcd_series_publication_type, gcd_brand, gcd_story_type,
         *   gcd_name_type, stddata_script, gcd_creator_signature,
         */
        private fun addStaticTables(
            targetSchema: String,
            conn: Connection
        ) {
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = ADD_STATIC_TABLES,
                conn = conn
            )
        }

        /**
         * Executes [ISSUE_SERIES_PATH]
         *
         * Target Schema:
         * - Adds the 'issue' and 'series' columns to the 'gcd_story_credit',
         *   'm_story_credit', and 'm_character_appearance' tables.
         * - Removes any m_character_appearances whose story is missing an issue_id
         * - Adds NOT NULL constraints to the 'issue' and 'series' columns in the
         *   'gcd_story_credit', 'm_story_credit', and 'm_character_appearance'
         *   tables.
         */
        private fun addIssueSeriesId(targetSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = ISSUE_SERIES_PATH,
                conn = conn
            )

        // Migrate
        /**
         * Executes [TEST_POPULATE_UPDATED]
         *
         * Target Schema:
         * - Populates the database with a mixture of good and bad records.
         *   Intended to reflect a newer version of the GCD.
         */
        private fun populateTestDatabaseUpdated(targetSchema: String, conn: Connection) {
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = TEST_POPULATE_UPDATED,
                conn = conn
            )
        }

        /**
         * Executes [MIGRATE_FILL_ID_COLUMNS]
         *
         * Target Schema:
         * - Sets issue and series ids in gcd_story, gcd_story_credit,
         *   migrate_story_credits, m_story_credit, and m_character_appearance for
         *   stories in migrate_stories
         */
        private fun addIssueSeriesIdNew(targetSchema: String, conn: Connection) {
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = null,
                sqlScript = MIGRATE_FILL_ID_COLUMNS,
                conn = conn
            )
        }

        /**
         * Executes [TEST_CREATE_MIGRATE_EXTRACTED_TABLES]
         *
         * Target Schema:
         * - Adds issue and series columns to gcd_story_credit.
         * - Creates m_character, m_character_appearance, and m_story_credit
         *   tables.
         *
         * Source Schema:
         * - Drops and Creates good_publishers, good_series, good_issue,
         *   good_story, and good_indicia_publishers views.
         * - Drops and Creates migrate_publishers, migrate_series, migrate_issues,
         *   migrate_stories, and migrate_indicia_publishers views.
         */
        private fun createMigrateExtractedTables(targetSchema: String, sourceSchema: String, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = sourceSchema,
                sqlScript = TEST_CREATE_MIGRATE_EXTRACTED_TABLES,
                conn = conn
            )

        /**
         * Executes [TEST_MIGRATE_POPULATE_CHARACTERS]
         *
         * Target Schema:
         * - Populates the m_character and m_character_appearance tables with
         *   characters that would be extracted from the migrate_stories table.
         */
        private fun populateTestDatabaseCharacters(schema: String, conn: Connection) {
            executeScript(
                targetSchema = schema,
                sourceSchema = null,
                sqlScript = TEST_MIGRATE_POPULATE_CHARACTERS,
                conn = conn
            )
        }

        /**
         * Executes [TEST_MIGRATE_POPULATE_CREDITS]
         *
         * Target Schema:
         * - Populates the m_story_credit table with credits that would be
         *   extracted from the migrate_stories table.
         */
        private fun populateTestDatabaseCredits(schema: String, conn: Connection) {
            executeScript(
                targetSchema = schema,
                sourceSchema = null,
                sqlScript = TEST_MIGRATE_POPULATE_CREDITS,
                conn = conn
            )
        }

        private fun executeScript(
            targetSchema: String,
            sourceSchema: String?,
            sqlScript: String,
            conn: Connection
        ) {
            val statements = File(sqlScript).parseSqlScript(targetSchema = targetSchema, sourceSchema = sourceSchema)
            statements.filter { it.isNotBlank() }.forEach { statement ->
                conn.createStatement().use { stmt ->
                    stmt.execute(statement)
                }
            }
        }
    }
}

enum class DBState {
    /** A database with good & bad records, good & bad tables */
    INITIAL,

    /** A database with good & bad records, good tables only */
    INIT_STEP_1A,
    INIT_STEP_1B,
    INIT_STEP_1C,
    INIT_STEP_1D,
    INIT_STEP_1_COMPLETE,
    INIT_STEP_2_COMPLETE,
    INIT_STEP_3_COMPLETE,
    INITIALIZED,
    MIGRATE_INITIAL,
    MIGRATE_STEP_1_COMPLETE,
    MIGRATE_STEP_2_COMPLETE,
    MIGRATE_STEP_3_COMPLETE,
    MIGRATE_STEP_4_COMPLETE,
    MIGRATED
}