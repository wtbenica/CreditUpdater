package dev.benica.creditupdater.db

import com.google.gson.Gson
import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.creditupdater.cli_parser.CLIParser
import dev.benica.creditupdater.db.ExtractionProgressTracker.Companion.ProgressInfo
import mu.KLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.*
import org.mockito.kotlin.*
import java.io.File
import java.sql.DriverManager

class ExtractionProgressTrackerTest {
    private lateinit var parser: CLIParser

    private val loggerMock: KLogger = mock<KLogger>()

    private lateinit var mockEpt: ExtractionProgressTracker

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)

        parser = CLIParser()

        // Create a test_progress.json file
        val file = File(TEST_PROGRESS_FILE)
        file.writer().use {
            it.write(DEFAULT_PROGRESS_MAP.filter { c -> !c.isWhitespace() })
        }

        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/$TEST_DATABASE",
            USERNAME_INITIALIZER,
            PASSWORD_INITIALIZER
        ).use { conn ->
            conn.createStatement().use { stmt ->
                // truncate
                stmt.execute("TRUNCATE TABLE gcd_story")

                // Insert 10 rows into the table
                for (i in 1..10) {
                    stmt.execute("INSERT INTO gcd_story VALUES ($i)")
                }
            }
        }

        mockEpt = spy(
            ExtractionProgressTracker(
                extractedType = "Credit",
                targetSchema = TEST_DATABASE,
                totalItems = 10,
                progressInfoMap = ProgressInfoMap(TEST_PROGRESS_FILE),
                logger = loggerMock
            )
        )

        whenever(mockEpt.getItemsCompleted()).thenReturn(ITEMS_COMPLETED)
    }

    // loadProgressInfo
    @Test
    @DisplayName("should load progress info correctly when there is an existing progress file")
    fun shouldLoadProgressInfoCorrectly() {
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, ProgressInfoMap(TEST_PROGRESS_FILE))

        val progressInfoMap = ept.progressInfoMap
        assertEquals(2, progressInfoMap.get("Credit").lastProcessedItemId)
        assertEquals(2000, progressInfoMap.get("Credit").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Credit").numCompleted)
        assertEquals(3, progressInfoMap.get("Character").lastProcessedItemId)
        assertEquals(3000, progressInfoMap.get("Character").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Character").numCompleted)

        assertEquals(2, mockEpt.getItemsCompleted())
    }

    @Test
    @DisplayName("should load progress info correctly when there is no existing progress file")
    fun shouldLoadProgressInfoCorrectlyWhenThereIsNoExistingProgressFile() {
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, ProgressInfoMap("nonexistent_progress.json"))
        val progressInfoMap = ept.progressInfoMap
        assertEquals(0, progressInfoMap.get("Credit").lastProcessedItemId)
        assertEquals(0, progressInfoMap.get("Credit").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Credit").numCompleted)
        assertEquals(0, progressInfoMap.get("Character").lastProcessedItemId)
        assertEquals(0, progressInfoMap.get("Character").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Character").numCompleted)

        val progressInfo = ept.progressInfo
        assertEquals(0, progressInfo.lastProcessedItemId)
        assertEquals(0, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)

        assertEquals(mutableMapOf<String, ProgressInfo>(), ept.progressInfoMap.mProgressInfoMap)
    }

    // updateProgressInfo
    @Test
    @DisplayName("should update progress info correctly")
    fun shouldUpdateProgressInfoCorrectly() {
        whenever(mockEpt.getItemsCompleted()).thenReturn(2)

        // verify progressInfo
        var progressInfo = mockEpt.progressInfo
        assertEquals(2, progressInfo.lastProcessedItemId)
        assertEquals(2000, progressInfo.totalTimeMillis)
        assertEquals(2, progressInfo.numCompleted)

        mockEpt.updateProgressInfo(8, 1000)

        progressInfo = mockEpt.progressInfo
        assertEquals(8, progressInfo.lastProcessedItemId)
        assertEquals(1000, progressInfo.totalTimeMillis)
        assertEquals(3, progressInfo.numCompleted)

        // verify saved file
        val file = File(TEST_PROGRESS_FILE)
        val gson = Gson()
        val expected: String = gson.toJson(
            """{
            "Credit": {
                "lastProcessedItemId": 8,
                "totalTimeMillis": 1000,
                "numCompleted": 3
            },
            "Character": {
                "lastProcessedItemId": 3,
                "totalTimeMillis": 3000,
                "numCompleted": 0
            }
        }""".filter { !it.isWhitespace() }
        )

        assertEquals(expected, gson.toJson(file.readText()))
    }

    // resetProgressInfo
    @Test
    @DisplayName("should reset progress info correctly")
    fun shouldResetProgressInfoCorrectly() {
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, ProgressInfoMap(TEST_PROGRESS_FILE))
        ept.resetProgressInfo()
        val progressInfoMap = ept.progressInfoMap
        assertEquals(0, progressInfoMap.get("Credit").lastProcessedItemId)
        assertEquals(0, progressInfoMap.get("Credit").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Credit").numCompleted)
        assertEquals(3, progressInfoMap.get("Character").lastProcessedItemId)
        assertEquals(3000, progressInfoMap.get("Character").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Character").numCompleted)

        val progressInfo = ept.progressInfo
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
                "totalTimeMillis": 3000,
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
        doReturn("Credit").whenever(mockEpt).extractedType

        mockEpt.resetProgressInfo()

        // Verify the progress info is reset correctly
        val progressInfoMap = mockEpt.progressInfoMap
        assertEquals(0, progressInfoMap.get("Credit").lastProcessedItemId)
        assertEquals(0, progressInfoMap.get("Credit").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Credit").numCompleted)
        assertEquals(3, progressInfoMap.get("Character").lastProcessedItemId)
        assertEquals(3000, progressInfoMap.get("Character").totalTimeMillis)
        assertEquals(0, progressInfoMap.get("Character").numCompleted)

        val progressInfo = mockEpt.progressInfo
        assertEquals(0, progressInfo.lastProcessedItemId)
        assertEquals(0, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)
    }

    // printProgressInfo
    @Test
    @DisplayName("should print progress info correctly")
    fun shouldPrintProgressInfoCorrectly() {
        whenever(mockEpt.getItemsCompleted()).thenReturn(2)

        // verify progressInfo
        assertEquals(2, mockEpt.progressInfo.lastProcessedItemId)
        assertEquals(2000, mockEpt.progressInfo.totalTimeMillis)
        assertEquals(2, mockEpt.progressInfo.numCompleted)

        mockEpt.printProgressInfo()

        verify(loggerMock).info("Extract Credit | StoryId: 2")
        verify(loggerMock).info("Complete: 2/10 20.00%")
        verify(loggerMock).info("Avg: 1000ms")
        verify(loggerMock).info("Elapsed: 0s ETR: 0s")
        verify(loggerMock).info("[====================>................................................................................] 20.00%")
    }

    // getProgressBar
    @Test
    @DisplayName("should get progress bar correctly A (24.6)")
    fun shouldGetProgressBarCorrectlyA() {
        val progressBar = mockEpt.getProgressBar(0.246f)
        assertEquals(
            "[========================>............................................................................] 24.60%",
            progressBar
        )
    }

    @Test
    @DisplayName("should get progress bar correctly B (100)")
    fun shouldGetProgressBarCorrectlyB() {
        val progressBar = mockEpt.getProgressBar(1.00f)
        assertEquals(
            "[====================================================================================================>] 100.00%",
            progressBar
        )
    }

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

    @Test
    fun toPercentShouldFormatFloatToTwoDecimalPlaces() {
        // Arrange
        val value = 0.75f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("75.00%", result)
    }

    @Test
    fun toPercentShouldHandleZeroCorrectly() {
        // Arrange
        val value = 0f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("0.00%", result)
    }

    @Test
    fun toPercentShouldHandleNegativeValuesCorrectly() {
        // Arrange
        val value = -0.5f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("-50.00%", result)
    }

    @Test
    fun toPercentShouldHandleSmallValuesCorrectly() {
        // Arrange
        val value = 0.0001f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("0.01%", result)
    }

    @Test
    fun toPercentShouldHandleVerySmallValuesCorrectly() {
        // Arrange
        val value = 0.00001f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("0.00%", result)
    }

    @Test
    fun toPercentShouldHandleRoundingVerySmallValuesCorrectly() {
        // Arrange
        // this isn't 0.00005 because of floating point precision, 0.00005 rounds down
        val value = 0.000051f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("0.01%", result)
    }

    companion object {
        private const val ITEMS_COMPLETED = 2
        private const val TEST_PROGRESS_FILE = "test_progress.json"
        private const val DEFAULT_PROGRESS_MAP = """{
            "Credit": {
                "lastProcessedItemId": 2,
                "totalTimeMillis": 2000,
                "numCompleted": 0
            },
            "Character": {
                "lastProcessedItemId": 3,
                "totalTimeMillis": 3000,
                "numCompleted": 0
            }
        }"""


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
                }
            }
        }
    }
}