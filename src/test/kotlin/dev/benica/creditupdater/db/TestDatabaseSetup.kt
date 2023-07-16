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
        private fun dropAllTablesAndViews(schema: String = TEST_DATABASE) {
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

                    stmt.executeQuery(query.replace("{{targetSchema}}", TEST_DATABASE)).use { rs ->
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
        }

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

        private fun addUnusedTables(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/add_unused_tables.sql").parseSqlScript(TEST_DATABASE)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun createTestDatabaseTables(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/create_test_database.sql").parseSqlScript(TEST_DATABASE)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun populateTestDatabaseGood(schema: String = TEST_DATABASE) {
            val statements =
                File("src/main/resources/sql/populate_test_database_good.sql").parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun populateTestDatabaseBad(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/populate_test_database_bad.sql").parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun populateTestDatabaseNew(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/populate_test_database_new.sql").parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun createExtractedTables(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/create_extracted_tables.sql").parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun populateCharacterTables(schema: String = TEST_DATABASE) {
            val statements =
                File("src/main/resources/sql/populate_extracted_character_tables.sql").parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun populateCreditTables(schema: String = TEST_DATABASE) {
            val statements =
                File("src/main/resources/sql/populate_extracted_credits_tables.sql").parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun createBadViews(schema: String = TEST_DATABASE) {
            val statements = File(INIT_CREATE_BAD_VIEWS).parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun addIssueSeriesId(schema: String = TEST_DATABASE) {
            val statements = File(ISSUE_SERIES_PATH).parseSqlScript(schema)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        /**
         * Creates a base database
         *
         * @param dbState the state of the database to create
         * @param schema the name of the database to create
         */
        @JvmStatic
        fun setup(dbState: DBState = DBState.PREPARED, schema: String = TEST_DATABASE) {
            dropAllTablesAndViews(schema)
            createTestDatabaseTables(schema)

            when (dbState) {
                DBState.INITIAL -> {
                    addUnusedTables(schema)
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                }

                DBState.INITIAL_PLUS_NEW_RECORDS -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    populateTestDatabaseNew(schema)
                    addUnusedTables(schema)
                }

                DBState.UNUSED_TABLES_DROPPED -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                }

                DBState.SOURCED_COLUMNS_DROPPED -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    dropSourcedColumns(schema)
                }

                DBState.EXTRACTED_TABLES_ADDED -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                }

                DBState.DELETE_VIEWS_CREATED -> {
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                }

                DBState.STEP_ONE_COMPLETE -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                }

                DBState.STEP_TWO_COMPLETE -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                    populateCharacterTables(schema)
                }

                DBState.STEP_THREE_COMPLETE -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                    populateCharacterTables(schema)
                    populateCreditTables(schema)
                }

                DBState.PREPARED -> {
                    populateTestDatabaseGood(schema)
                    dropSourcedColumns(schema)
                    createExtractedTables(schema)
                    createBadViews(schema)
                    populateCharacterTables(schema)
                    populateCreditTables(schema)
                    addIssueSeriesId(schema)
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

        internal fun dropAllTables(conn: Connection, schema: String) {
            try {
                // disable foreign key checks
                conn.createStatement().use { stmt ->
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 0")
                }

                val tablesQuery =
                    """SELECT table_name 
                            |FROM information_schema.tables 
                            |WHERE table_schema = '$schema' 
                            |AND table_type = 'BASE TABLE'""".trimMargin()

                val viewsQuery =
                    """SELECT table_name 
                            |FROM information_schema.tables 
                            |WHERE table_schema = '$schema' 
                            |AND table_type = 'VIEW'""".trimMargin()

                conn.createStatement().use { stmt ->
                    // Retrieve the names of all tables in the database
                    stmt.executeQuery(tablesQuery).use { resultSet ->
                        val tableNames = mutableListOf<String>()

                        // Store the table names in a list
                        while (resultSet.next()) {
                            val tableName = resultSet.getString("table_name")
                            tableNames.add(tableName)
                        }

                        // Generate and execute DROP TABLE statements for each table
                        tableNames.forEach { tableName ->
                            val dropStatement = "DROP TABLE $tableName"
                            stmt.executeUpdate(dropStatement)
                        }
                    }

                    // Retrieve the names of all views in the database
                    stmt.executeQuery(viewsQuery).use { resultSet ->
                        val viewNames = mutableListOf<String>()

                        // Store the view names in a list
                        while (resultSet.next()) {
                            val viewName = resultSet.getString("table_name")
                            viewNames.add(viewName)
                        }

                        // Generate and execute DROP VIEW statements for each view
                        viewNames.forEach { viewName ->
                            val dropStatement = "DROP VIEW $viewName"
                            stmt.executeUpdate(dropStatement)
                        }
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                // enable foreign key checks
                conn.createStatement().use { stmt ->
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
                }
            }
        }
    }
}

enum class DBState {
    INITIAL,
    UNUSED_TABLES_DROPPED,
    SOURCED_COLUMNS_DROPPED,
    EXTRACTED_TABLES_ADDED,
    DELETE_VIEWS_CREATED,
    STEP_ONE_COMPLETE,
    STEP_TWO_COMPLETE,
    STEP_THREE_COMPLETE,
    PREPARED,
    INITIAL_PLUS_NEW_RECORDS
}