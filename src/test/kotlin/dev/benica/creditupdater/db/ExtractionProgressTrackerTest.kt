package dev.benica.creditupdater.db

import com.google.gson.Gson
import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.creditupdater.db.ExtractionProgressTracker.Companion.ProgressInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.File
import java.sql.DriverManager

class ExtractionProgressTrackerTest {
    // getRemainingTime
    @Test
    @DisplayName("should calculate the remaining time correctly")
    fun shouldCalculateRemainingTimeCorrectly() {
        assertEquals(12, ExtractionProgressTracker.getRemainingTime(totalItems = 10, averageTime = 2, numComplete = 4))
    }

    @Test
    @DisplayName("should calculate the remaining time correctly when total items is null")
    fun shouldCalculateRemainingTimeCorrectlyWhenTotalItemsIsNull() {
        assertNull(ExtractionProgressTracker.getRemainingTime(totalItems = null, averageTime = 1000, numComplete = 4))
    }


    // loadProgressInfo
    @Test
    @DisplayName("should load progress info correctly when there is an existing progress file")
    fun shouldLoadProgressInfoCorrectly() {
        val etp = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)

        val progressInfoMap = etp.loadProgressInfo()
        assertEquals(2, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(1, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        assertEquals(2, etp.getItemsCompleted())
    }

    @Test
    @DisplayName("should load progress info correctly when there is no existing progress file")
    fun shouldLoadProgressInfoCorrectlyWhenThereIsNoExistingProgressFile() {
        val etp = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, "nonexistent_progress.json")
        val progressInfoMap = etp.progressInfoMap
        assertEquals(null, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(null, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(null, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(null, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(null, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(null, progressInfoMap["Character"]?.numCompleted)

        val progressInfo = etp.progressInfo
        assertEquals(0, progressInfo.lastProcessedItemId)
        assertEquals(0, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)

        assertEquals(mutableMapOf<String, ProgressInfo>(), etp.loadProgressInfo())
    }

    // resetProgressInfo
    @Test
    @DisplayName("should reset progress info correctly")
    fun shouldResetProgressInfoCorrectly() {
        val etp = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        etp.resetProgressInfo()
        val progressInfoMap = etp.progressInfoMap
        assertEquals(0, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(0, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        val progressInfo = etp.progressInfo
        assertEquals(0, progressInfo.lastProcessedItemId)
        assertEquals(0, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)

        // verify saved file
        val file = File(TEST_PROGRESS_FILE)
        val gson = Gson()
        val expected: String = gson.toJson(
            """{
            "Credit": {
                "lastProcessedItemId": 0,
                "totalTimeMillis": 0,
                "numCompleted": 0
            },
            "Character": {
                "lastProcessedItemId": 3,
                "totalTimeMillis": 2,
                "numCompleted": 0
            }
        }""".filter { !it.isWhitespace() }
        )

        assertEquals(expected, gson.toJson(file.readText()))
    }

    // saveProgressInfo
    @Test
    @DisplayName("should save progress info correctly")
    fun shouldSaveProgressInfoCorrectly() {
        val etp = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        // verify initial state
        val progressInfoMap = etp.progressInfoMap
        assertEquals(2, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(1, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        val progressInfo = etp.progressInfo
        assertEquals(2, progressInfo.lastProcessedItemId)
        assertEquals(1, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)

        // update progress info
        etp.updateProgressInfo(3, 2)
        assertEquals(3, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(1, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        assertEquals(3, progressInfo.lastProcessedItemId)
        assertEquals(2, progressInfo.totalTimeMillis)
        assertEquals(1, progressInfo.numCompleted)

        // save progress info
        etp.saveProgressInfo()

        val progressInfoMapPost = etp.progressInfoMap
        assertEquals(3, progressInfoMapPost["Credit"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMapPost["Credit"]?.totalTimeMillis)
        assertEquals(1, progressInfoMapPost["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMapPost["Character"]?.lastProcessedItemId)
        assertEquals(2, progressInfoMapPost["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMapPost["Character"]?.numCompleted)

        val progressInfoPost = etp.progressInfo
        assertEquals(3, progressInfoPost.lastProcessedItemId)
        assertEquals(2, progressInfoPost.totalTimeMillis)
        assertEquals(1, progressInfoPost.numCompleted)

        // verify saved file
        val file = File(TEST_PROGRESS_FILE)
        val gson = Gson()
        val expected: String = gson.toJson(
            """{
            "Credit": {
                "lastProcessedItemId": 3,
                "totalTimeMillis": 2,
                "numCompleted": 1
            },
            "Character": {
                "lastProcessedItemId": 3,
                "totalTimeMillis": 2,
                "numCompleted": 0
            }
        }""".filter { !it.isWhitespace() }
        )

        assertEquals(expected, gson.toJson(file.readText()))
    }

    // getLastProcessedItemId
    @Test
    @DisplayName("should get last processed item id correctly")
    fun shouldGetLastProcessedItemIdCorrectly() {
        assertEquals(2, ExtractionProgressTracker.getLastProcessedItemId("Credit", TEST_PROGRESS_FILE))
    }

    @Test
    @DisplayName("should get last processed item id correctly when file does not exist")
    fun shouldGetLastProcessedItemIdCorrectlyWhenFileDoesNotExist() {
        assertEquals(null, ExtractionProgressTracker.getLastProcessedItemId("Credit", "non-existent-file.json"))
    }

    // getProgressBar
    @Test
    @DisplayName("should get progress bar correctly A (24.6)")
    fun shouldGetProgressBarCorrectlyA() {
        val etp = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        val progressBar = etp.getProgressBar(24.6f)
        assertEquals(
            "[========================>............................................................................] 24.60%",
            progressBar
        )
    }

    @Test
    @DisplayName("should get progress bar correctly B (100)")
    fun shouldGetProgressBarCorrectlyB() {
        val etp = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        val progressBar = etp.getProgressBar(100f)
        assertEquals(
            "[====================================================================================================>] 100.00%",
            progressBar
        )
    }

    @BeforeEach
    fun beforeEach() {
        // Create a test_progress.json file
        val file = File(TEST_PROGRESS_FILE)
        file.writeText(
            """{
            "Credit": {
                "lastProcessedItemId": 2,
                "totalTimeMillis": 1,
                "numCompleted": 0
            },
            "Character": {
                "lastProcessedItemId": 3,
                "totalTimeMillis": 2,
                "numCompleted": 0
            }
        }""".filter { !it.isWhitespace() }
        )
    }
    companion object {
        private const val TEST_PROGRESS_FILE = "test_progress.json"

        @BeforeAll
        @JvmStatic
        fun setup() {
            // Create a database table 'gcd_story' with a single column 'id'
            DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/$TEST_DATABASE",
                USERNAME_INITIALIZER,
                PASSWORD_INITIALIZER
            ).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("CREATE TABLE IF NOT EXISTS gcd_story (id INT)")

                    // truncate
                    stmt.execute("TRUNCATE TABLE gcd_story")

                    // Insert 10 rows into the table
                    for (i in 1..10) {
                        stmt.execute("INSERT INTO gcd_story VALUES ($i)")
                    }
                }
            }

            // Create a test_progress.json file
            val file = File(TEST_PROGRESS_FILE)
            file.writeText(
                """{
                    "Credit": {
                        "lastProcessedItemId": 2,
                        "totalTimeMillis": 1,
                        "numCompleted": 0
                    },
                    "Character": {
                        "lastProcessedItemId": 3,
                        "totalTimeMillis": 2,
                        "numCompleted": 0
                    }
                }""".trimIndent()
            )
        }
    }
}