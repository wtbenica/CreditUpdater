package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import org.junit.jupiter.api.AfterAll
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class TestDatabaseSetup {
    companion object {
        /**
         * Creates a base database
         *
         * @param populate adds records or tables to the database
         * @param schema the name of the database to create
         */
        @JvmStatic
        fun setup(populate: DatabaseState = DatabaseState.PREPARED, schema: String = TEST_DATABASE) {
            // create publishers table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS gcd_publisher (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(255) NOT NULL,
                            country_id INTEGER NOT NULL,
                            year_began INTEGER DEFAULT NULL
                        )""".trimIndent()
                    )

                    statement.execute(
                        """INSERT INTO gcd_publisher (name, country_id, year_began)
                            VALUES 
                                ('Marvel', 225, 1939),
                                ('DC', 225, 1935)""".trimIndent()
                    )
                }
            }

            // create series table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS gcd_series (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(255) NOT NULL,
                            year_began INTEGER NOT NULL,
                            publisher_id INT NOT NULL,
                            country_id INTEGER NOT NULL,
                            language_id INTEGER NOT NULL,
                            FOREIGN KEY (publisher_id) REFERENCES gcd_publisher(id)
                        )""".trimIndent()
                    )

                    statement.execute(
                        """INSERT INTO gcd_series (name, year_began, publisher_id, country_id, language_id)
                            VALUES
                                ('Doom Patrol', 1988, 2, 225, 25),
                                ('New X-Men', 2001, 1, 225, 25)
                                """.trimIndent()
                    )
                }
            }

            // create issue table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS gcd_issue (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            number INT NOT NULL,
                            series_id INT NOT NULL,
                            FOREIGN KEY (series_id) REFERENCES gcd_series(id)
                        )""".trimIndent()
                    )

                    statement.execute(
                        """INSERT INTO gcd_issue (number, series_id)
                            VALUES 
                                (35, 1),
                                (114, 2)""".trimIndent()
                    )
                }
            }

            // create story table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_story` (
                          `id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
                          `title` varchar(255) NOT NULL DEFAULT '',
                          `issue_id` INTEGER NOT NULL REFERENCES `gcd_issue`(`id`),
                          `script` longtext NOT NULL,
                          `pencils` longtext NOT NULL,
                          `inks` longtext NOT NULL,
                          `colors` longtext NOT NULL,
                          `letters` longtext NOT NULL,
                          `editing` longtext NOT NULL,
                          `characters` longtext NOT NULL)""".trimIndent()
                    )

                    statement.execute(
                        """INSERT INTO gcd_story (title, issue_id, script, pencils, inks, colors, letters, editing, characters)
                            VALUES 
                                ('Crawling from the Wreckage', 1, 'Grant Morrison', 'Richard Case', 'Richard Case', 'Daniel Vozzo', 'John Workman', 'Tom Peyer', 'Doom Patrol [Crazy Jane [Kay Challis], Robotman [Cliff Steele], Dorothy Spinner, Rebis [Larry Trainor]], Danny the Street, Flex Mentallo, Willoughby Kipling'),
                                ('E for Extinction', 2, 'Grant Morrison', 'Frank Quitely', 'Tim Townsend', 'Liquid!', 'Richard Starkings', 'Mark Powers', 'X-Men [Beast [Hank McKoy], Cyclops [Scott Summers], White Queen [Emma Frost], Marvel Girl [Jean Grey], Professor X [Charles Xavier], Wolverine [Logan]]')""".trimIndent()
                    )
                }
            }

            // create credit_type table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """ CREATE TABLE IF NOT EXISTS gcd_credit_type(
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR (255) NOT NULL
                        )""".trimIndent()
                    )

                    // insert credit_type
                    statement.execute(
                        """ INSERT INTO gcd_credit_type (name)
                                VALUES ('script'),
                                    ('pencils'),
                                    ('inks'),
                                    ('colors'),
                                    ('letters'),
                                    ('editing')""".trimIndent()
                    )
                }
            }

            // create gcd_creator_name_detail table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """ CREATE TABLE IF NOT EXISTS gcd_creator_name_detail(
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR (255) NOT NULL
                        )
                        """.trimIndent()
                    )

                    // insert gcd_creator_name_detail
                    statement.execute(
                        """ INSERT INTO gcd_creator_name_detail (name)
                                VALUES ('Grant Morrison'),
                                    ('Frank Quitely'),
                                    ('Val Semeiks'),
                                    ('Dan Green'),
                                    ('Chris Sotomayor'),
                                    ('Richard Starkings'),
                                    ('Bob Schreck'),
                                    ('Michael Wright')""".trimIndent()
                    )
                }
            }

            // create gcd_story_credit table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """ CREATE TABLE IF NOT EXISTS `gcd_story_credit`(
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `creator_id` int (11) NOT NULL,
                            `credit_type_id` int (11) NOT NULL,
                            `story_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail`(`id`),
                            FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type`(`id`),
                            FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`)
                        )
                        """.trimIndent()
                    )

                    // insert story_credit
                    statement.execute(
                        """INSERT INTO gcd_story_credit (creator_id, credit_type_id, story_id) 
                            VALUES
                            (2, 2, 1),
                            (3, 2, 2),
                            (4, 3, 2),
                            (5, 4, 2),
                            (6, 5, 2)""".trimIndent()
                    )
                }
            }

            // create gcd_indicia_publisher table, with fk to gcd_publisher
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS gcd_indicia_publisher (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR (255) NOT NULL,
                            parent_id INT NOT NULL REFERENCES gcd_publisher(id)
                        )""".trimIndent()
                    )

                    // insert gcd_indicia_publisher
                    statement.execute(
                        """INSERT INTO gcd_indicia_publisher (name, parent_id)
                            VALUES
                            ('Wildstorm Comics', 1),
                            ('Marvel Comics', 2)""".trimIndent()
                    )
                }
            }

            // create gcd_brand_group table with fk parent_id to gcd_publisher
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS gcd_brand_group (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR (255) NOT NULL,
                            parent_id INT NOT NULL REFERENCES gcd_publisher(id)
                        )""".trimIndent()
                    )

                    // insert gcd_brand_group
                    statement.execute(
                        """INSERT INTO gcd_brand_group (name, parent_id)
                            VALUES
                            ('Vertigo', 1),
                            ('Marvel Knights', 2)""".trimIndent()
                    )
                }
            }

            when (populate) {
                DatabaseState.EMPTY -> {}
                DatabaseState.RAW -> addRecordsToBeRemoved(schema)
                DatabaseState.PREPARED -> addExtractedTables(schema)
            }
        }

        private fun addExtractedTables(schema: String = TEST_DATABASE) {
            // create m_story_credit table
            getDbConnection(schema).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `m_story_credit` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `creator_id` int (11) NOT NULL,
                            `credit_type_id` int (11) NOT NULL,
                            `story_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail`(`id`),
                            FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type`(`id`),
                            FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`)
                        )""".trimIndent()
                    )

                    // insert 1 record
                    statement.execute(
                        """INSERT INTO m_story_credit (creator_id, credit_type_id, story_id)
                            VALUES
                            (2, 2, 2)""".trimIndent()
                    )
                }
            }
        }

        private fun addRecordsToBeRemoved(schema: String = TEST_DATABASE) {
            getDbConnection(schema).use { conn ->
                conn.createStatement().use { statement ->
                    // insert publisher
                    /*
                    - Editori Laterza - IT-IT (bad - non-US)
                    - HarperCollins - US-EN (bad - no 20+th century series)
                     */
                    statement.execute(
                        """INSERT INTO gcd_publisher (name, country_id, year_began)
                            VALUES 
                                ('Editori Laterza', 106, 1901),
                                ('HarperCollins', 225, 1817)""".trimIndent()
                    )

                    // insert series
                    /*
                    - I fumetti di Mao - IT-IT (bad - foreign publisher)
                    - Simpsons Comics Extravaganze - US-EN (bad - publisher has no 20th/21st century series)
                    - Batman: Une Lecture de Bon Conseil - US-FR (bad - foreign language)
                    - Wolverine: Son of Canada - US-EN (bad - foreign country)
                    - Little Red Riding Hood - US-EN (bad - publisher has post-20th century series, but this series isn't one of them)
                     */
                    statement.execute(
                        """INSERT INTO gcd_series (name, year_began, publisher_id, country_id, language_id)
                            VALUES
                                ('I fumetti di Mao', 1990, 3, 106, 51),
                                ('Simpsons Comics Extravaganze', 1899, 4, 225, 25),
                                ('Batman: Une Lecture de Bon Conseil', 2010, 2, 225, 34),
                                ('Wolverine: Son of Canada', 1983, 1, 36, 25),
                                ('Little Red Riding Hood', 1890, 2, 225, 25)
                                """.trimMargin()
                    )

                    // insert issues
                    /*
                    - I fumetti di Mao - 1
                    - Simposons Comics Extravaganze - 1
                    - Batman: Une Lecture de Bon Conseil - 1
                    - Wolverine: Son of Canada - 1
                     */
                    statement.execute(
                        """INSERT INTO gcd_issue (number, series_id)
                            VALUES 
                                (1, 3),
                                (1, 4),
                                (1, 5),
                                (1, 6)
                                """.trimIndent()
                    )

                    // insert story
                    statement.execute(
                        """INSERT INTO gcd_story (title, issue_id, script, pencils, inks, colors, letters, editing, characters)
                            VALUES 
                                ('Il fumetto di Mao', 3, 'A', 'B', 'C', 'D', 'E', 'F', 'Mao'),
                                ('Simpsons Comics Extravaganze', 4, 'A', 'B', 'C', 'D', 'E', 'F', 'Simpsons'),
                                ('Batman: Une Lecture de Bon Conseil', 5, 'A', 'B', 'C', 'D', 'E', 'F', 'Batman'),
                                ('Wolverine: Son of Canada', 6, 'A', 'B', 'C', 'D', 'E', 'F', 'Wolverine')
                                """.trimIndent()
                    )

                    // insert gcd_indicia_publishers with links to bad gcd_publishers
                    statement.execute(
                        """INSERT INTO gcd_indicia_publisher (name, parent_id)
                            VALUES
                                ('Editori Laterza', 3),
                                ('HarperCollins', 4)""".trimIndent()
                    )

                    // insert gcd_brand_groups with links to bad gcd_publishers
                    statement.execute(
                        """INSERT INTO gcd_brand_group (name, parent_id)
                            VALUES
                                ('Viva Italia', 3),
                                ('Etch', 4)""".trimIndent()
                    )
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            // remove all tables
            getDbConnection(TEST_DATABASE).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("DROP TABLE IF EXISTS m_character_appearance")
                    statement.execute("DROP TABLE IF EXISTS m_character")
                    statement.execute("DROP TABLE IF EXISTS m_story_credit")
                    statement.execute("DROP TABLE IF EXISTS gcd_story_credit")
                    statement.execute("DROP TABLE IF EXISTS gcd_creator_name_detail")
                    statement.execute("DROP TABLE IF EXISTS gcd_credit_type")
                    statement.execute("DROP TABLE IF EXISTS gcd_story")
                    statement.execute("DROP TABLE IF EXISTS gcd_issue")
                    statement.execute("DROP TABLE IF EXISTS gcd_series")
                    statement.execute("DROP TABLE IF EXISTS gcd_indicia_publisher")
                    statement.execute("DROP TABLE IF EXISTS gcd_brand_group")
                    statement.execute("DROP TABLE IF EXISTS gcd_publisher")
                }
            }
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
    RAW,
    PREPARED
}