package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class TestDatabaseSetup {
    //@Test
    //fun testRaw() {
    //    setup(DatabaseState.RAW_FOR_BAD_VIEWS)
    //}
    //
    //@Test
    //fun testBadStories() {
    //    truncateAllTables()
    //    setup(DatabaseState.RAW_FOR_CULLING)
    //}
    //
    //@Test
    //fun testPrepared() {
    //    setup(DatabaseState.PREPARED)
    //}
    //
    //@Test
    //fun teardownTest() {
    //    teardown()
    //}
    //
    //@Test
    //fun truncateAllTables() {
    //    getTestDbConnection().use { connection ->
    //        connection.createStatement().use { statement ->
    //            statement.execute("SET FOREIGN_KEY_CHECKS = 0")
    //            statement.execute("TRUNCATE TABLE gcd_biblio_entry")
    //            statement.execute("TRUNCATE TABLE gcd_story_credit")
    //            statement.execute("TRUNCATE TABLE gcd_story_feature_object")
    //            statement.execute("TRUNCATE TABLE gcd_story_feature_logo")
    //            statement.execute("TRUNCATE TABLE gcd_issue_credit")
    //            statement.execute("TRUNCATE TABLE gcd_issue_indicia_printer")
    //            statement.execute("TRUNCATE TABLE gcd_issue")
    //            statement.execute("TRUNCATE TABLE gcd_series")
    //            statement.execute("TRUNCATE TABLE gcd_publisher")
    //            statement.execute("SET FOREIGN_KEY_CHECKS = 1")
    //        }
    //    }
    //}

    companion object {
        /**
         * Creates a base database
         *
         * @param populateWith adds records or tables to the database
         * @param schema the name of the database to create
         */
        @JvmStatic
        fun setup(populateWith: DatabaseState = DatabaseState.PREPARED, schema: String = TEST_DATABASE) {
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
                            first_issue_id INT DEFAULT NULL,
                            last_issue_id INT DEFAULT NULL,
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
                            variant_of_id INT DEFAULT NULL,
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

            when (populateWith) {
                DatabaseState.EMPTY -> {}
                DatabaseState.RAW_FOR_BAD_VIEWS -> addBadRecords(schema)
                DatabaseState.RAW_FOR_CULLING -> {
                    addBadRecords(schema)
                    addBadViews(schema)
                    addBadRecordsByAssociation(schema)
                }

                DatabaseState.PREPARED -> addExtractedStoryCreditTable(schema)
            }
        }

        /** Adds the m_story_credit table */
        private fun addExtractedStoryCreditTable(schema: String = TEST_DATABASE) {
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

        /**
         * Creates or replaces bad_publishers, bad_series, bad_issues, and
         * bad_stories views
         */
        private fun addBadViews(schema: String = TEST_DATABASE) {
            getDbConnection(schema).use { conn ->
                conn.createStatement().use { statement ->
                    statement.execute(
                        """CREATE OR REPLACE VIEW `bad_publishers` AS (
                                    SELECT gp.id
                                    FROM gcd_publisher gp
                                    WHERE gp.country_id != 225
                                    OR gp.id NOT IN (
                                        SELECT DISTINCT gp.id
                                        FROM gcd_publisher gp
                                        INNER JOIN gcd_series gs ON gp.id = gs.publisher_id
                                        WHERE gs.year_began >= 1900
                                    )
                                )""".trimIndent()
                    )

                    statement.execute(
                        """CREATE OR REPLACE VIEW `bad_series` AS (
                                    SELECT gs.id
                                    FROM gcd_series gs
                                    WHERE gs.country_id != 225
                                    OR gs.language_id != 25
                                    OR gs.year_began < 1900
                                    OR gs.publisher_id IN (SELECT id FROM bad_publishers)
                                )""".trimIndent()
                    )

                    statement.execute(
                        """CREATE OR REPLACE VIEW `bad_issues` AS (
                                    SELECT gi.id
                                    FROM gcd_issue gi
                                    WHERE gi.series_id IN (
                                        SELECT id
                                        FROM bad_series
                                    )
                                )""".trimIndent()
                    )

                    statement.execute(
                        """CREATE OR REPLACE VIEW `bad_stories` AS (
                                    SELECT gs.id
                                    FROM gcd_story gs
                                    WHERE gs.issue_id IN (
                                        SELECT id
                                        FROM bad_issues
                                    )
                                )""".trimIndent()
                    )

                    statement.execute(
                        """CREATE OR REPLACE VIEW `bad_indicia_publishers` AS (
                                    SELECT gip.id
                                    FROM gcd_indicia_publisher gip
                                    WHERE gip.parent_id IN (SELECT id FROM bad_publishers)
                                )""".trimIndent()
                    )

                    statement.execute(
                        """CREATE OR REPLACE VIEW `bad_brand_groups` AS (
                                    SELECT gbg.id
                                    FROM gcd_brand_group gbg
                                    WHERE gbg.parent_id IN (SELECT id FROM bad_publishers)
                                )""".trimIndent()
                    )
                }
            }
        }

        /** Add records that meet the bad views' criteria. */
        private fun addBadRecords(schema: String = TEST_DATABASE) {
            getDbConnection(schema).use { conn ->
                conn.createStatement().use { statement ->
                    // insert publisher / ids: 3, 4, 5
                    statement.execute(
                        """INSERT INTO gcd_publisher (name, country_id, year_began)
                            VALUES 
                                ('country_id != 225', 106, 1901),
                                ('no series >= 1900', 225, 1817),
                                ('old publisher, series >= 1900', 225, 1800)""".trimIndent()
                    )

                    // insert series / ids: 3, 4, 5, 6, 7
                    statement.execute(
                        """INSERT INTO gcd_series (name, year_began, publisher_id, country_id, language_id)
                            VALUES
                                ('bad publisher', 1990, 3, 106, 51),
                                ('< 1900, bad publisher', 1899, 4, 225, 25),
                                ('language_id != 25', 2010, 2, 225, 34),
                                ('country_id != 225', 1983, 1, 36, 25),
                                ('< 1900, good publisher', 1890, 2, 225, 25),
                                ('>= 1900, old but good publisher', 1900, 5, 225, 25)
                                """.trimMargin()
                    )

                    // insert issues / ids: 3, 4, 5, 6, 7
                    statement.execute(
                        """INSERT INTO gcd_issue (number, series_id)
                            VALUES 
                                (1, 3),
                                (1, 4),
                                (1, 5),
                                (1, 6),
                                (1, 7)
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

        /** Create tables for and add records that are bad by association. */
        private fun addBadRecordsByAssociation(schema: String = TEST_DATABASE) {
            getDbConnection(schema).use { conn ->
                conn.createStatement().use { statement ->

                    // insert gcd_story_credit's with links to bad gcd_story's
                    statement.execute(
                        """INSERT INTO gcd_story_credit (creator_id, credit_type_id, story_id)
                            VALUES
                                (1, 1, 3),
                                (2, 2, 4),
                                (3, 3, 5),
                                (4, 4, 6)""".trimIndent()
                    )

                    // create gcd_biblio_entry table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_biblio_entry` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `name` varchar (255) NOT NULL,
                            `story_ptr_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`story_ptr_id`) REFERENCES `gcd_story`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_biblio_entry's with links to bad gcd_story's
                    statement.execute(
                        """INSERT INTO gcd_biblio_entry (name, story_ptr_id)
                            VALUES
                                ('Il fumetto di Mao', 3),
                                ('Simpsons Comics Extravaganze', 4),
                                ('Batman: Une Lecture de Bon Conseil', 5),
                                ('Wolverine: Son of Canada', 6)""".trimIndent()
                    )

                    // create gcd_reprint table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_reprint` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `origin_id` int (11) NOT NULL,
                            `target_id` int (11) NOT NULL,
                            `origin_issue_id` int (11) NOT NULL,
                            `target_issue_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`origin_id`) REFERENCES `gcd_story`(`id`),
                            FOREIGN KEY (`target_id`) REFERENCES `gcd_story`(`id`),
                            FOREIGN KEY (`origin_issue_id`) REFERENCES `gcd_issue`(`id`),
                            FOREIGN KEY (`target_issue_id`) REFERENCES `gcd_issue`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_reprint's with links to bad gcd_story's or gcd_issue's
                    statement.execute(
                        """INSERT INTO gcd_reprint (origin_id, target_id, origin_issue_id, target_issue_id)
                            VALUES
                                (3, 4, 3, 4),
                                (4, 5, 4, 5),
                                (5, 6, 5, 6),
                                (6, 3, 6, 3)""".trimIndent()
                    )

                    // create gcd_story_feature_object table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_story_feature_object` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `story_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_story_feature_object's with links to bad gcd_story's
                    statement.execute(
                        """INSERT INTO gcd_story_feature_object (story_id)
                            VALUES
                                (3),
                                (4),
                                (5),
                                (6)""".trimIndent()
                    )

                    // create gcd_story_feature_logo table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_story_feature_logo` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `story_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_story_feature_logo's with links to bad gcd_story's
                    statement.execute(
                        """INSERT INTO gcd_story_feature_logo (story_id)
                            VALUES
                                (3),
                                (4),
                                (5),
                                (6)""".trimIndent()
                    )

                    // create gcd_issue_credit table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_issue_credit` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `issue_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`issue_id`) REFERENCES `gcd_issue`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_issue_credit's with links to bad gcd_issue's
                    statement.execute(
                        """INSERT INTO gcd_issue_credit (issue_id)
                            VALUES
                                (3),
                                (4),
                                (5),
                                (6)""".trimIndent()
                    )

                    // create gcd_issue_indicia_printer table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_issue_indicia_printer` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `issue_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`issue_id`) REFERENCES `gcd_issue`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_issue_indicia_printer's with links to bad gcd_issue's
                    statement.execute(
                        """INSERT INTO gcd_issue_indicia_printer (issue_id)
                            VALUES
                                (3),
                                (4),
                                (5),
                                (6)""".trimIndent()
                    )

                    // create gcd_series_bond table
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_series_bond` (
                            `id` int (11) NOT NULL AUTO_INCREMENT,
                            `origin_issue_id` int (11) NOT NULL,
                            `target_issue_id` int (11) NOT NULL,
                            `origin_id` int (11) NOT NULL,
                            `target_id` int (11) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`origin_issue_id`) REFERENCES `gcd_issue`(`id`),
                            FOREIGN KEY (`target_issue_id`) REFERENCES `gcd_issue`(`id`),
                            FOREIGN KEY (`origin_id`) REFERENCES `gcd_series`(`id`),
                            FOREIGN KEY (`target_id`) REFERENCES `gcd_series`(`id`)
                        )""".trimIndent()
                    )

                    // insert gcd_series_bond's with links to bad gcd_issue's but origin_id and target_id are to good series
                    statement.execute(
                        """INSERT INTO gcd_series_bond (origin_issue_id, target_issue_id, origin_id, target_id)
                            VALUES
                                (3, 4, 1, 2),
                                (4, 5, 2, 3),
                                (5, 6, 3, 4),
                                (6, 3, 4, 1)""".trimIndent()
                    )

                    // insert gcd_series_bond's with links to bad series, but good issues
                    statement.execute(
                        """INSERT INTO gcd_series_bond (origin_issue_id, target_issue_id, origin_id, target_id)
                            VALUES
                                (1, 2, 1, 6),
                                (1, 2, 2, 3),
                                (1, 2, 3, 1),
                                (1, 2, 4, 2)""".trimIndent()
                    )

                    // insert gcd_issue's with variant_of_id link to gcd_issue whose series or publisher is bad
                    statement.execute(
                        """INSERT INTO gcd_issue (series_id, number, variant_of_id)
                            VALUES
                                (1, 1, 3),
                                (2, 1, 4),
                                (3, 1, 5),
                                (4, 1, 6)""".trimIndent()
                    )

                    // insert series whose first_issue_id or last_issue_id is a bad issue
                    statement.execute(
                        """INSERT INTO gcd_series (name, year_began, publisher_id, country_id, language_id, first_issue_id, last_issue_id)
                            VALUES
                                ('Series 1', 1900, 1, 225, 1, 3, 4),
                                ('Series 2', 1900, 2, 225, 1, 4, 5),
                                ('Series 3', 1900, 3, 225, 1, 5, 6),
                                ('Series 4', 1900, 4, 225, 1, 6, 3)""".trimIndent()
                    )
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            // remove all tables
            getTestDbConnection().use { connection ->
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
    RAW_FOR_BAD_VIEWS,
    RAW_FOR_CULLING,
    PREPARED
}