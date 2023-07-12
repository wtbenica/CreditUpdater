package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.INIT_CREATE_BAD_VIEWS
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

private fun File.parseSqlScript(schema: String): List<String> =
    useLines(Charsets.UTF_8) { lines: Sequence<String> ->
        lines.filter { it.isNotBlank() }
            .map { it.replace("{{targetSchema}}", schema).trim() }
            .joinToString(separator = " ")
            .split(";")
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }


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
                File("src/main/resources/sql/populate_test_database_good.sql").parseSqlScript(TEST_DATABASE)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun populateTestDatabaseBad(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/populate_test_database_bad.sql").parseSqlScript(TEST_DATABASE)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun createExtractedTables(schema: String = TEST_DATABASE) {
            val statements = File("src/main/resources/sql/create_extracted_tables.sql").parseSqlScript(TEST_DATABASE)
            getDbConnection(schema).use { connection ->
                statements.filter { it.isNotBlank() }.forEach { statement ->
                    connection.createStatement().use { stmt ->
                        stmt.execute(statement)
                    }
                }
            }
        }

        private fun createBadViews(schema: String = TEST_DATABASE) {
            val statements = File(INIT_CREATE_BAD_VIEWS).parseSqlScript(TEST_DATABASE)
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
         * @param populateWith adds records or tables to the database
         * @param schema the name of the database to create
         */
        @JvmStatic
        fun setup(populateWith: DatabaseState = DatabaseState.PREPARED, schema: String = TEST_DATABASE) {
            dropAllTablesAndViews(schema)
            createTestDatabaseTables(schema)

            when (populateWith) {
                DatabaseState.EMPTY -> {
                    addUnusedTables(schema)
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                }
                DatabaseState.GOOD_RECORDS -> populateTestDatabaseGood(schema)
                DatabaseState.ALL_RECORDS -> {
                    createBadViews()
                    populateTestDatabaseGood(schema)
                    populateTestDatabaseBad(schema)
                }

                DatabaseState.PREPARED -> {
                    populateTestDatabaseGood(schema)
                    createExtractedTables(schema)
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

enum class DatabaseState {
    EMPTY,
    GOOD_RECORDS,
    ALL_RECORDS,
    PREPARED
}