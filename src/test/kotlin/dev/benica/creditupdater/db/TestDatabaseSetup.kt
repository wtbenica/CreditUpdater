package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.INIT_CREATE_BAD_VIEWS
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.ISSUE_SERIES_PATH
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class TestDatabaseSetup {
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
            schema: String = TEST_DATABASE,
            sourceSchema: String? = null
        ) {
            if (sourceSchema == null) {
                dropAllTablesAndViews(schema)
                createTestDatabaseTables(schema)
            } else {
                dropAllTablesAndViews(sourceSchema)
                createTestDatabaseTables(sourceSchema)
            }

            when (dbState) {
                DBState.INITIAL -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    addUnusedTables(schema)
                }

                DBState.INIT_STEP_1A -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                }

                DBState.INIT_STEP_1B -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    dropSourcedColumns(schema)
                }

                DBState.INIT_STEP_1C -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                }

                DBState.INIT_STEP_1D -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                }

                DBState.INIT_STEP_1_COMPLETE -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                }

                DBState.INIT_STEP_2_COMPLETE -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                    populateCharacterTables(schema)
                }

                DBState.INIT_STEP_3_COMPLETE -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                    populateCharacterTables(schema)
                    populateCreditTables(schema)
                }

                DBState.INITIALIZED -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                    populateCharacterTables(schema)
                    populateCreditTables(schema)
                    addIssueSeriesId(schema)
                }

                DBState.MIGRATE_INITIAL -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema)
                        populateTestDatabaseBad(sourceSchema)
                        populateTestDatabaseNew(sourceSchema)
                        addUnusedTables(sourceSchema)
                    }
                }

                DBState.MIGRATE_STEP_1_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema)
                        populateTestDatabaseBad(sourceSchema)
                        populateTestDatabaseNew(sourceSchema)
                        addUnusedTables(sourceSchema)
                        createExtractedTables(sourceSchema)
                    }
                }

                DBState.MIGRATE_STEP_2_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema)
                        populateTestDatabaseBad(sourceSchema)
                        populateTestDatabaseNew(sourceSchema)
                        addUnusedTables(sourceSchema)
                        createExtractedTables(sourceSchema)
                        createMigrateTables(schema, sourceSchema)
                        populateTestDatabaseCharacters(sourceSchema)
                    }
                }

                DBState.MIGRATE_STEP_3_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema)
                        populateTestDatabaseBad(sourceSchema)
                        populateTestDatabaseNew(sourceSchema)
                        addUnusedTables(sourceSchema)
                        createExtractedTables(sourceSchema)
                        createMigrateTables(schema, sourceSchema)
                        populateTestDatabaseCharacters(sourceSchema)
                        populateTestDatabaseCredits(sourceSchema)
                    }
                }

                DBState.MIGRATE_STEP_4_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema)
                        populateTestDatabaseBad(sourceSchema)
                        populateTestDatabaseNew(sourceSchema)
                        addUnusedTables(sourceSchema)
                        createExtractedTables(sourceSchema)
                        createMigrateTables(schema, sourceSchema)
                        populateCharacterTables(sourceSchema)
                        populateCreditTables(sourceSchema)
                        addIssueSeriesIdNew(sourceSchema)
                    }
                }

                DBState.MIGRATED -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema)
                        populateTestDatabaseBad(sourceSchema)
                        populateTestDatabaseNew(sourceSchema)
                        addUnusedTables(sourceSchema)
                        createExtractedTables(sourceSchema)
                        createMigrateTables(schema, sourceSchema)
                        populateCharacterTables(sourceSchema)
                        populateCreditTables(sourceSchema)
                        addIssueSeriesIdNew(sourceSchema)
                        // migrate records
                    }
                }
            }
        }

        @JvmStatic
        fun teardown(schema: String = TEST_DATABASE) {
            dropAllTablesAndViews(schema)
        }

        fun getTestDbConnection(): Connection = getDbConnection(TEST_DATABASE)

        fun getDbConnection(schemaName: String): Connection = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$schemaName",
            Credentials.USERNAME_INITIALIZER,
            Credentials.PASSWORD_INITIALIZER
        )

        internal fun dropAllTablesAndViews(schema: String = TEST_DATABASE) {
            try {
                getDbConnection(schema).use { connection ->
                    connection.createStatement().use { stmt ->
                        // Disable foreign key checks
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 0")

                        // Drop all views and tables
                        val query = """
                    SELECT CONCAT('DROP TABLE IF EXISTS ', table_name, ';') AS statement
                    FROM information_schema.tables
                    WHERE table_schema = '{{targetSchema}}'
                    UNION
                    SELECT CONCAT('DROP VIEW IF EXISTS ', table_name, ';') AS statement
                    FROM information_schema.views
                    WHERE table_schema = '{{targetSchema}}'
                """.trimIndent()

                        stmt.executeQuery(query.replace("{{targetSchema}}", schema)).use { rs ->
                            while (rs.next()) {
                                val statement = rs.getString("statement")
                                println(statement)
                                connection.createStatement().use { stmt2 ->
                                    stmt2.execute(statement)
                                }
                            }

                        }

                        // Enable foreign key checks
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                throw e
            }
        }

        // Private functions
        // Initialize
        /**
         * Creates empty, minimal versions of the tables that are used both in the
         * GCD and Infinite Longbox.
         */
        private fun createTestDatabaseTables(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = "src/main/resources/sql/test_setup_database.sql")

        /**
         * Populates the database with records that will be used by Infinite
         * Longbox.
         */
        private fun populateTestDatabaseGood(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = "src/main/resources/sql/test_setup_populate_good.sql")

        /**
         * Populates the database with records that will not be used by Infinite
         * Longbox. There are also a few records that will be used by Infinite
         * Longbox.
         */
        private fun populateTestDatabaseBad(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = "src/main/resources/sql/test_setup_populate_bad.sql")

        /**
         * Adds tables that are in the GCD, but not used in the Infinite Longbox
         * database.
         */
        private fun addUnusedTables(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = "src/main/resources/sql/test_setup_add_unused_tables.sql")

        private fun dropSourcedColumns(schema: String = TEST_DATABASE) {
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { stmt ->
                    val sqlStmt = """ALTER TABLE gcd_story_credit
                                DROP COLUMN IF EXISTS is_sourced,
                                DROP COLUMN IF EXISTS sourced_by;
                            """.trimIndent()

                    stmt.execute(sqlStmt)
                }
            }
        }

        /**
         * Creates the m_character, m_character_appearance, and m_story_credit
         * tables and adds issue/series id columns to gcd_story_credit.
         */
        private fun createExtractedTables(targetSchema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = targetSchema,
                sqlScript = "src/main/resources/sql/test_setup_create_extracted_tables.sql"
            )

        /**
         * Creates bad_publisher, bad_series, bad_issues, bad_stories, and
         * bad_indicia_publishers views
         */
        private fun createBadViews(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = INIT_CREATE_BAD_VIEWS)

        /**
         * Adds a couple characters and appearances to m_character and
         * m_character_appearance.
         */
        private fun populateCharacterTables(schema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/populate_extracted_character_tables.sql"
            )

        /** Adds a couple story credits to m_story_credit. */
        private fun populateCreditTables(schema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/populate_extracted_credits_tables.sql"
            )

        private fun addIssueSeriesId(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = ISSUE_SERIES_PATH)

        // Migrate
        private fun addIssueSeriesIdNew(schema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/migrate_fill_id_columns.sql"
            )

        private fun createMigrateTables(targetSchema: String = TEST_DATABASE, sourceSchema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = sourceSchema,
                sqlScript = "src/main/resources/sql/test_setup_create_migrate_tables.sql"
            )

        /**
         * Populates the database with a mixture of good and bad records. Intended
         * to reflect a newer version of the GCD.
         */
        private fun populateTestDatabaseNew(schema: String = TEST_DATABASE) =
            executeScript(targetSchema = schema, sqlScript = "src/main/resources/sql/test_setup_populate_new.sql")

        private fun populateTestDatabaseCharacters(schema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_migrate_populate_character_tables.sql"
            )

        private fun populateTestDatabaseCredits(schema: String = TEST_DATABASE) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_migrate_populate_credits.sql"
            )

        private fun executeScript(
            targetSchema: String = TEST_DATABASE,
            sourceSchema: String? = null,
            sqlScript: String
        ) {
            val statements = File(sqlScript).parseSqlScript(targetSchema = targetSchema, sourceSchema = sourceSchema)
            getDbConnection(targetSchema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
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