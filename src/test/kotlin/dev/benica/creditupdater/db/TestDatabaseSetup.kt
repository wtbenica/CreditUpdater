package dev.benica.creditupdater.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE_UPDATE
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.INIT_CREATE_BAD_VIEWS
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.ISSUE_SERIES_PATH
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class TestDatabaseSetup {
    @Test
    fun testSetup() {
        setup(dbState = DBState.INITIAL)
        //setup(dbState = DBState.MIGRATE_STEP_4_COMPLETE, sourceSchema = TEST_DATABASE_UPDATE)
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
            schema: String = TEST_DATABASE,
            sourceSchema: String? = null
        ) {
            val testConn: Connection = getDbConnection(schema)
            var updateConn: Connection? = null

            if (sourceSchema == null) {
                dropAllTablesAndViews(schema, testConn)
                createTestDatabaseTables(schema, testConn)
            } else {
                updateConn = getDbConnection(sourceSchema)
                dropAllTablesAndViews(sourceSchema, updateConn)
                createTestDatabaseTables(sourceSchema, updateConn)
            }

            when (dbState) {
                DBState.INITIAL -> {
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                    addUnusedTables(schema, testConn)
                }

                DBState.INIT_STEP_1A -> {
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                }

                DBState.INIT_STEP_1B -> {
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                }

                DBState.INIT_STEP_1C -> {
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createExtractedTables(schema, testConn)
                }

                DBState.INIT_STEP_1D -> {
                    populateTestDatabaseGood(schema, testConn)
                    populateTestDatabaseBad(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                }

                DBState.INIT_STEP_1_COMPLETE -> {
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                }

                DBState.INIT_STEP_2_COMPLETE -> {
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                    populateCharacterTables(schema, testConn)
                }

                DBState.INIT_STEP_3_COMPLETE -> {
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                    populateCharacterTables(schema, testConn)
                    populateCreditTables(schema, testConn)
                }

                DBState.INITIALIZED -> {
                    populateTestDatabaseGood(schema, testConn)
                    dropSourcedColumns(schema, testConn)
                    createExtractedTables(schema, testConn)
                    createBadViews(schema, testConn)
                    populateCharacterTables(schema, testConn)
                    populateCreditTables(schema, testConn)
                    addIssueSeriesId(schema, testConn)
                }

                DBState.MIGRATE_INITIAL -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema, updateConn!!)
                        populateTestDatabaseBad(sourceSchema, updateConn)
                        populateTestDatabaseNew(sourceSchema, updateConn)
                        addUnusedTables(sourceSchema, updateConn)
                    }
                }

                DBState.MIGRATE_STEP_1_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema, updateConn!!)
                        populateTestDatabaseBad(sourceSchema, updateConn)
                        populateTestDatabaseNew(sourceSchema, updateConn)
                        addUnusedTables(sourceSchema, updateConn)
                        createExtractedTables(sourceSchema, updateConn)
                    }
                }

                DBState.MIGRATE_STEP_2_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema, updateConn!!)
                        populateTestDatabaseBad(sourceSchema, updateConn)
                        populateTestDatabaseNew(sourceSchema, updateConn)
                        addUnusedTables(sourceSchema, updateConn)
                        createExtractedTables(sourceSchema, updateConn)
                        createMigrateTables(schema, sourceSchema, updateConn)
                        populateTestDatabaseCharacters(sourceSchema, updateConn)
                    }
                }

                DBState.MIGRATE_STEP_3_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema, updateConn!!)
                        populateTestDatabaseBad(sourceSchema, updateConn)
                        populateTestDatabaseNew(sourceSchema, updateConn)
                        addUnusedTables(sourceSchema, updateConn)
                        createExtractedTables(sourceSchema, updateConn)
                        createMigrateTables(schema, sourceSchema, updateConn)
                        populateTestDatabaseCharacters(sourceSchema, updateConn)
                        populateTestDatabaseCredits(sourceSchema, updateConn)
                    }
                }

                DBState.MIGRATE_STEP_4_COMPLETE -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema, updateConn!!)
                        populateTestDatabaseBad(sourceSchema, updateConn)
                        populateTestDatabaseNew(sourceSchema, updateConn)
                        addUnusedTables(sourceSchema, updateConn)
                        createExtractedTables(sourceSchema, updateConn)
                        createMigrateTables(schema, sourceSchema, updateConn)
                        populateTestDatabaseCharacters(sourceSchema, updateConn)
                        populateTestDatabaseCredits(sourceSchema, updateConn)
                        addIssueSeriesIdNew(sourceSchema, updateConn)
                    }
                }

                DBState.MIGRATED -> {
                    if (sourceSchema != null) {
                        populateTestDatabaseGood(sourceSchema, updateConn!!)
                        populateTestDatabaseBad(sourceSchema, updateConn)
                        populateTestDatabaseNew(sourceSchema, updateConn)
                        addUnusedTables(sourceSchema, updateConn)
                        createExtractedTables(sourceSchema, updateConn)
                        createMigrateTables(schema, sourceSchema, updateConn)
                        populateTestDatabaseCharacters(sourceSchema, updateConn)
                        populateTestDatabaseCredits(sourceSchema, updateConn)
                        addIssueSeriesIdNew(sourceSchema, updateConn)
                        // migrate records
                    }
                }
            }
        }

        @JvmStatic
        fun teardown(schema: String = TEST_DATABASE, conn: Connection) {
            dropAllTablesAndViews(schema, conn)
        }

        private var connectionMap: MutableMap<String, HikariDataSource> = mutableMapOf()

        fun getDbConnection(targetSchema: String): Connection {
            if (!connectionMap.containsKey(targetSchema) || connectionMap[targetSchema]?.isClosed == true) {
                connectionMap[targetSchema] = createConnection(targetSchema)
            }
            @Suppress("kotlin:S6611")
            return connectionMap[targetSchema]!!.connection
        }

        fun getTestDbConnection(): Connection = getDbConnection(TEST_DATABASE)

        private fun createConnection(schemaName: String): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://localhost:3306/$schemaName"
                username = Credentials.USERNAME_INITIALIZER
                password = Credentials.PASSWORD_INITIALIZER
                driverClassName = "com.mysql.cj.jdbc.Driver"
                maximumPoolSize = ConnectionProvider.MAX_CONNECTION_POOL_SIZE
                minimumIdle = 5
                idleTimeout = 10000
                connectionTimeout = 5000
            }
        )

        internal fun dropAllTablesAndViews(schema: String, conn: Connection) {
            try {
                conn.createStatement().use { stmt ->
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

        // Private functions
        /**
         * Creates empty, minimal versions of the tables that are used both in the
         * GCD and Infinite Longbox.
         */
        private fun createTestDatabaseTables(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_database.sql",
                conn = conn
            )

        /**
         * Populates the database with records that will be used by Infinite
         * Longbox.
         */
        private fun populateTestDatabaseGood(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_populate_good.sql",
                conn = conn
            )

        /**
         * Populates the database with records that will not be used by Infinite
         * Longbox. There are also a few records that will be used by Infinite
         * Longbox.
         */
        private fun populateTestDatabaseBad(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_populate_bad.sql",
                conn = conn
            )

        /**
         * Adds tables that are in the GCD, but not used in the Infinite Longbox
         * database.
         */
        private fun addUnusedTables(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_add_unused_tables.sql",
                conn = conn
            )

        // Initialize
        private fun dropSourcedColumns(schema: String = TEST_DATABASE, conn: Connection) {
            conn.createStatement().use { stmt ->
                val sqlStmt = """ALTER TABLE $schema.gcd_story_credit
                                DROP COLUMN IF EXISTS is_sourced,
                                DROP COLUMN IF EXISTS sourced_by;
                            """.trimIndent()

                stmt.execute(sqlStmt)
            }
        }

        /**
         * Creates the m_character, m_character_appearance, and m_story_credit
         * tables and adds issue/series id columns to gcd_story_credit.
         */
        private fun createExtractedTables(targetSchema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = targetSchema,
                sqlScript = "src/main/resources/sql/test_setup_create_extracted_tables.sql", conn = conn
            )

        /**
         * Creates bad_publisher, bad_series, bad_issues, bad_stories, and
         * bad_indicia_publishers views
         */
        private fun createBadViews(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(targetSchema = schema, sqlScript = INIT_CREATE_BAD_VIEWS, conn = conn)

        /**
         * Adds a couple characters and appearances to m_character and
         * m_character_appearance.
         */
        private fun populateCharacterTables(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_init_populate_character_tables.sql",
                conn = conn
            )

        /** Adds a couple story credits to m_story_credit. */
        private fun populateCreditTables(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_init_populate_credits.sql",
                conn = conn
            )

        private fun addIssueSeriesId(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = ISSUE_SERIES_PATH,
                conn = conn
            )

        // Migrate
        private fun addIssueSeriesIdNew(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/migrate_fill_id_columns.sql",
                conn = conn
            )

        private fun createMigrateTables(
            targetSchema: String = TEST_DATABASE,
            sourceSchema: String = TEST_DATABASE,
            conn: Connection
        ) =
            executeScript(
                targetSchema = targetSchema,
                sourceSchema = sourceSchema,
                sqlScript = "src/main/resources/sql/test_setup_create_migrate_tables.sql",
                conn = conn
            )

        /**
         * Populates the database with a mixture of good and bad records. Intended
         * to reflect a newer version of the GCD.
         */
        private fun populateTestDatabaseNew(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_populate_new.sql",
                conn = conn
            )

        private fun populateTestDatabaseCharacters(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_migrate_populate_character_tables.sql",
                conn = conn
            )

        private fun populateTestDatabaseCredits(schema: String = TEST_DATABASE, conn: Connection) =
            executeScript(
                targetSchema = schema,
                sqlScript = "src/main/resources/sql/test_setup_migrate_populate_credits.sql",
                conn = conn
            )

        private fun executeScript(
            targetSchema: String = TEST_DATABASE,
            sourceSchema: String? = null,
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