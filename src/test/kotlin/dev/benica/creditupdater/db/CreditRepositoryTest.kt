package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
import java.sql.*

/** Credit Repository Test - tests the CreditRepository class */
class CreditRepositoryTest {

    /** Function to test: createOrUpdateStoryCredit */
    @Test
    @DisplayName("should insert new story credit if one does not exist")
    fun shouldInsertNewStoryCreditIfOneDoesNotExist() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Grant Morrison"
        val storyId = 1
        val roleId = 1

        // verify that the story credit does not exist
        getDbConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, extractedName)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    assert(!resultSet.next())
                }
            }
        }

        // Act
        creditRepository.createOrUpdateStoryCredit(extractedName, storyId, roleId)

        // Assert that the story credit was inserted
        getDbConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, extractedName)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    assert(resultSet.next())
                }
            }
        }
    }

    @Test
    @DisplayName("createOrUpdateStoryCredit() should update existing story credit if one exists in gcd_story_credit")
    fun shouldUpdateExistingStoryCreditInGcdStoryCredit() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Frank Quitely"
        val roleId = 2
        val storyId = 1

        // verify that the story credit exists in gcd_story_credit
        getDbConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, extractedName)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    assert(resultSet.next())
                }
            }
        }

        // Act
        creditRepository.createOrUpdateStoryCredit(extractedName, storyId, roleId)

        // Assert that the story credit was updated
        getDbConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * 
                FROM gcd_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, extractedName)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    assert(resultSet.next())
                }
            }
        }
    }

    @Test
    @DisplayName("createOrUpdateStoryCredit() should update existing story credit if one exists in m_story_credit")
    fun shouldUpdateExistingStoryCreditInMStoryCredit() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Frank Quitely"
        val roleId = 2
        val storyId = 2

        // verify that the story credit exists in gcd_story_credit
        getDbConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, extractedName)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    assert(resultSet.next())
                }
            }
        }

        // Act
        creditRepository.createOrUpdateStoryCredit(extractedName, storyId, roleId)

        // Assert that the story credit was updated
        getDbConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * 
                FROM m_story_credit sc
                WHERE sc.creator_id = (
                    SELECT gcnd.id
                    FROM gcd_creator_name_detail gcnd
                    WHERE gcnd.name = ?
                )
                AND sc.story_id = ?
                AND sc.credit_type_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, extractedName)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    assert(resultSet.next())
                }
            }
        }
    }

    // lookupGcndId
    @Test
    @DisplayName("should return the gcd_creator_name_detail id if found, null otherwise")
    fun shouldReturnTheGcdCreatorNameDetailIdIfFoundNullOtherwise() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Grant Morrison"

        // Act
        val gcndId = creditRepository.lookupGcndId(extractedName)

        // Assert
        assert(gcndId == 1)
    }

    @Test
    @DisplayName("should return null if the gcd_creator_name_detail id is not found")
    fun shouldReturnNullIfTheGcdCreatorNameDetailIdIsNotFound() {
        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE)
        val extractedName = "Bob"

        // Act
        val gcndId = creditRepository.lookupGcndId(extractedName)

        // Assert
        assert(gcndId == null)
    }

    @Test
    @DisplayName("should throw any SQLExceptions | lookupGcndId")
    fun shouldThrowAnySQLExceptionsLookupGcndId() {
        // Mock queryExecutor
        val queryExecutorMock = mock<QueryExecutor>()
        whenever(
            queryExecutorMock.executePreparedStatement(any(), any())
        ).thenAnswer { throw SQLException("Test Exception") }

        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)
        val extractedName = "Grant Morrison"

        // Assert
        assertThrows<SQLException> { creditRepository.lookupGcndId(extractedName) }
    }

    // lookupStoryCreditId
    @Test
    @DisplayName("should throw any SQLExceptions | lookupStoryCreditId")
    fun shouldThrowAnySQLExceptionsLookupStoryCreditId() {
        // Mock queryExecutor
        val queryExecutorMock = mock<QueryExecutor>()
        whenever(
            queryExecutorMock.executePreparedStatement(any(), any())
        ).thenAnswer { throw SQLException("Test Exception") }

        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)
        val extractedName = 1
        val storyId = 1
        val roleId = 1

        // Assert
        assertThrows<SQLException> { creditRepository.lookupStoryCreditId(extractedName, storyId, roleId) }
    }

    @Test
    @DisplayName("should throw any SQLExceptions | lookupStoryCreditId, second")
    fun shouldThrowAnySQLExceptionsLookupStoryCreditIdSecond() {
        // Mock queryExecutor
        val queryExecutorMock = mock<QueryExecutor>()
        whenever(
            queryExecutorMock.executePreparedStatement(any(), any())
        ).thenAnswer {  }.thenAnswer { throw SQLException("Test Exception") }

        // Arrange
        val creditRepository = CreditRepository(TEST_DATABASE, queryExecutor = queryExecutorMock)
        val extractedName = 1
        val storyId = 1
        val roleId = 1

        // Assert
        assertThrows<SQLException> { creditRepository.lookupStoryCreditId(extractedName, storyId, roleId) }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            // create publishers table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS gcd_publishers (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(255) NOT NULL
                        )
                        """.trimIndent()
                    )

                    // insert publisher
                    statement.execute(
                        """
                        INSERT INTO gcd_publishers (name)
                        VALUES ('Marvel')
                        """.trimIndent()
                    )
                    statement.execute(
                        """
                        INSERT INTO gcd_publishers (name)
                        VALUES ('DC')
                        """.trimIndent()
                    )
                }
            }

            // create series table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS gcd_series (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(255) NOT NULL,
                            publisher_id INT NOT NULL,
                            FOREIGN KEY (publisher_id) REFERENCES gcd_publishers(id)
                        )
                        """.trimIndent()
                    )

                    // insert series
                    statement.execute(
                        """INSERT INTO gcd_series (name, publisher_id)
                            VALUES ('Doom Patrol', 2),
                            ('New X-Men', 1)
                        """.trimMargin()
                    )
                }
            }

            // create issue table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS gcd_issue (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            number INT NOT NULL,
                            series_id INT NOT NULL,
                            FOREIGN KEY (series_id) REFERENCES gcd_series(id)
                        )
                        """.trimIndent()
                    )

                    // insert issue
                    statement.execute(
                        """INSERT INTO gcd_issue (number, series_id)
                            VALUES (35, 1),
                            (114, 2)
                        """.trimIndent()
                    )
                }
            }

            // create story table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS gcd_story (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            title VARCHAR(255) NOT NULL,
                            issue_id INT NOT NULL,
                            FOREIGN KEY (issue_id) REFERENCES gcd_issue(id)
                        )
                        """.trimIndent()
                    )

                    // insert story
                    statement.execute(
                        """INSERT INTO gcd_story (title, issue_id)
                            VALUES ('Crawling from the Wreckage', 1),
                            ('E for Extinction', 2)
                        """.trimIndent()
                    )
                }
            }

            // create credit_type table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS gcd_credit_type (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(255) NOT NULL
                        )
                        """.trimIndent()
                    )

                    // insert credit_type
                    statement.execute(
                        """
                        INSERT INTO gcd_credit_type (name)
                        VALUES ('script'),
                        ('pencils'),
                        ('inks'),
                        ('colors'),
                        ('letters')
                        """.trimIndent()
                    )
                }
            }

            // create gcd_creator_name_detail table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS gcd_creator_name_detail (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(255) NOT NULL
                        )
                        """.trimIndent()
                    )

                    // insert gcd_creator_name_detail
                    statement.execute(
                        """
                        INSERT INTO gcd_creator_name_detail (name)
                        VALUES ('Grant Morrison'),
                        ('Frank Quitely'),
                        ('Val Semeiks'),
                        ('Dan Green'),
                        ('Chris Sotomayor'),
                        ('Richard Starkings')
                        """.trimIndent()
                    )
                }
            }

            // create story_credit table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `gcd_story_credit` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `created` datetime(6) NOT NULL,
                            `modified` datetime(6) NOT NULL,
                            `deleted` tinyint(1) NOT NULL,
                            `is_credited` tinyint(1) NOT NULL,
                            `is_signed` tinyint(1) NOT NULL,
                            `uncertain` tinyint(1) NOT NULL,
                            `signed_as` varchar(255) NOT NULL,
                            `credited_as` varchar(255) NOT NULL,
                            `credit_name` varchar(255) NOT NULL,
                            `creator_id` int(11) NOT NULL,
                            `credit_type_id` int(11) NOT NULL,
                            `story_id` int(11) NOT NULL,
                            `signature_id` int(11) DEFAULT NULL,
                            `is_sourced` tinyint(1) NOT NULL,
                            `sourced_by` varchar(255) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail` (`id`),
                            FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type` (`id`),
                            FOREIGN KEY (`story_id`) REFERENCES `gcd_story` (`id`))""".trimIndent()
                    )

                    // insert story_credit
                    statement.execute(
                        """INSERT INTO gcd_story_credit (created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, is_sourced, sourced_by)
                            VALUES ('2006-09-24 00:00:00.000000', '2018-08-12 05:25:13.000000', 0, 1, 1, 0, 'Frank Quitely', 'Frank Quitely', 'Frank Quitely', 2, 2, 1, 0, ''),
                            ('2006-09-24 00:00:00.000000', '2018-08-12 05:25:13.000000', 0, 1, 1, 0, 'Val Semeiks', 'Val Semeiks', 'Val Semeiks', 3, 2, 2, 0, ''),
                            ('2006-09-24 00:00:00.000000', '2018-08-12 05:25:13.000000', 0, 1, 1, 0, 'Dan Green', 'Dan Green', 'Dan Green', 4, 3, 2, 0, ''),
                            ('2006-09-24 00:00:00.000000', '2018-08-12 05:25:13.000000', 0, 1, 1, 0, 'Chris Sotomayor', 'Chris Sotomayor', 'Chris Sotomayor', 5, 4, 2, 0, ''),
                            ('2006-09-24 00:00:00.000000', '2018-08-12 05:25:13.000000', 0, 1, 1, 0, 'Richard Starkings', 'Richard Starkings', 'Richard Starkings', 6, 5, 2, 0, '')""".trimIndent()
                    )
                }
            }

            // create m_story_credit table
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE IF NOT EXISTS `m_story_credit` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `created` datetime(6) NOT NULL,
                            `modified` datetime(6) NOT NULL,
                            `deleted` tinyint(1) NOT NULL,
                            `is_credited` tinyint(1) NOT NULL,
                            `is_signed` tinyint(1) NOT NULL,
                            `uncertain` tinyint(1) NOT NULL,
                            `signed_as` varchar(255) NOT NULL,
                            `credited_as` varchar(255) NOT NULL,
                            `credit_name` varchar(255) NOT NULL,
                            `creator_id` int(11) NOT NULL,
                            `credit_type_id` int(11) NOT NULL,
                            `story_id` int(11) NOT NULL,
                            `signature_id` int(11) DEFAULT NULL,
                            `is_sourced` tinyint(1) NOT NULL,
                            `sourced_by` varchar(255) NOT NULL,
                            PRIMARY KEY (`id`),
                            FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail` (`id`),
                            FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type` (`id`),
                            FOREIGN KEY (`story_id`) REFERENCES `gcd_story` (`id`))""".trimIndent()
                    )

                    // insert 1 record
                    statement.execute(
                        """INSERT INTO m_story_credit (created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, is_sourced, sourced_by)
                            VALUES ('2006-09-24 00:00:00.000000', '2018-08-12 05:25:13.000000', 0, 1, 1, 0, 'Frank Quitely', 'Frank Quitely', 'Frank Quitely', 2, 2, 2, 0, '')""".trimIndent()
                    )
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            // remove all tables
            getDbConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("DROP TABLE IF EXISTS m_story_credit")
                    statement.execute("DROP TABLE IF EXISTS gcd_story_credit")
                    statement.execute("DROP TABLE IF EXISTS gcd_creator_name_detail")
                    statement.execute("DROP TABLE IF EXISTS gcd_credit_type")
                    statement.execute("DROP TABLE IF EXISTS gcd_story")
                    statement.execute("DROP TABLE IF EXISTS gcd_issue")
                    statement.execute("DROP TABLE IF EXISTS gcd_series")
                    statement.execute("DROP TABLE IF EXISTS gcd_publishers")
                }
            }
        }

        private fun getDbConnection(): Connection = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/${Credentials.TEST_DATABASE}",
            Credentials.USERNAME_INITIALIZER,
            Credentials.PASSWORD_INITIALIZER
        )
    }
}