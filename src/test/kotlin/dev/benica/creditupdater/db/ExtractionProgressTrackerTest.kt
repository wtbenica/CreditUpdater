package dev.benica.creditupdater.db

import com.google.gson.Gson
import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.creditupdater.cli_parser.CLIParser
import dev.benica.creditupdater.db.ExtractionProgressTracker.Companion.ProgressInfo
import dev.benica.creditupdater.di.QueryExecutorSource
import mu.KLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.*
import java.io.File
import java.sql.DriverManager

class ExtractionProgressTrackerTest {
    private lateinit var parser: CLIParser

    @Mock
    private lateinit var queryExecutorSourceMock: QueryExecutorSource

    @Mock
    private lateinit var queryExecutorMock: QueryExecutor

    @Mock
    private lateinit var progressFileMock: File

    private val loggerMock: KLogger = mock<KLogger>()

    @Spy
    @InjectMocks
    private var mockEpt: ExtractionProgressTracker =
        ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE, logger = loggerMock)

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)

        parser = CLIParser()

        whenever(queryExecutorSourceMock.getQueryExecutor(any())).thenReturn(queryExecutorMock)

        // Create a test_progress.json file
        val file = File(TEST_PROGRESS_FILE)
        file.writer().use {
            it.write(DEFAULT_PROGRESS_FILE.filter { c -> !c.isWhitespace() })
        }
        //file.writeText(DEFAULT_PROGRESS_FILE.filter { !it.isWhitespace() })

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
    }

    // loadProgressInfo
    @Test
    @DisplayName("should load progress info correctly when there is an existing progress file")
    fun shouldLoadProgressInfoCorrectly() {
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)

        val progressInfoMap = ept.loadProgressInfo()
        assertEquals(2, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(2000, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(3000, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        assertEquals(2, ept.getItemsCompleted())
    }

    @Test
    @DisplayName("should load progress info correctly when there is no existing progress file")
    fun shouldLoadProgressInfoCorrectlyWhenThereIsNoExistingProgressFile() {
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, "nonexistent_progress.json")
        val progressInfoMap = ept.progressInfoMap
        assertEquals(null, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(null, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(null, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(null, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(null, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(null, progressInfoMap["Character"]?.numCompleted)

        val progressInfo = ept.progressInfo
        assertEquals(0, progressInfo.lastProcessedItemId)
        assertEquals(0, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)

        assertEquals(mutableMapOf<String, ProgressInfo>(), ept.loadProgressInfo())
    }

    // updateProgressInfo
    @Test
    @DisplayName("should update progress info correctly")
    fun shouldUpdateProgressInfoCorrectly() {
        whenever(mockEpt.loadProgressInfo()).thenReturn(mockProgressInfoMap)
        whenever(mockEpt.getItemsCompleted()).thenReturn(2)

        // verify progressInfoMap
        assertEquals(2, mockEpt.progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(2000, mockEpt.progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(2, mockEpt.progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, mockEpt.progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(3000, mockEpt.progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, mockEpt.progressInfoMap["Character"]?.numCompleted)

        mockEpt.updateProgressInfo(8, 1000)

        val progressInfoMap = mockEpt.progressInfoMap
        assertEquals(8, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(1000, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(3, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(3000, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        val progressInfo = mockEpt.progressInfo
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
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        ept.resetProgressInfo()
        val progressInfoMap = ept.progressInfoMap
        assertEquals(0, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(0, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(3000, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

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

        val mockItemsCompleted = 2

        whenever(mockEpt.loadProgressInfo()).thenReturn(mockProgressInfoMap)
        whenever(mockEpt.getItemsCompleted()).thenReturn(mockItemsCompleted)
        whenever(queryExecutorSourceMock.getQueryExecutor(any())).thenReturn(queryExecutorMock)

        mockEpt.resetProgressInfo()

        // Verify the progress info is reset correctly
        val progressInfoMap = mockEpt.progressInfoMap
        assertEquals(0, progressInfoMap["Credit"]?.lastProcessedItemId)
        assertEquals(0, progressInfoMap["Credit"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Credit"]?.numCompleted)
        assertEquals(3, progressInfoMap["Character"]?.lastProcessedItemId)
        assertEquals(3000, progressInfoMap["Character"]?.totalTimeMillis)
        assertEquals(0, progressInfoMap["Character"]?.numCompleted)

        val progressInfo = mockEpt.progressInfo
        assertEquals(0, progressInfo.lastProcessedItemId)
        assertEquals(0, progressInfo.totalTimeMillis)
        assertEquals(0, progressInfo.numCompleted)
    }

    // printProgressInfo
    @Test
    @DisplayName("should print progress info correctly")
    fun shouldPrintProgressInfoCorrectly() {
        val eptMock = spy(
            ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE, logger = loggerMock)
        )
        whenever(eptMock.progressFile).thenReturn(progressFileMock)
        whenever(eptMock.loadProgressInfo()).thenReturn(mockProgressInfoMap)
        whenever(eptMock.getItemsCompleted()).thenReturn(2)

        doNothing().whenever(loggerMock).debug(any<() -> Any?>())

        // verify progressInfo
        assertEquals(2, eptMock.progressInfo.lastProcessedItemId)
        assertEquals(2000, eptMock.progressInfo.totalTimeMillis)
        assertEquals(2, eptMock.progressInfo.numCompleted)

        eptMock.printProgressInfo()

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
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        val progressBar = ept.getProgressBar(0.246f)
        assertEquals(
            "[========================>............................................................................] 24.60%",
            progressBar
        )
    }

    @Test
    @DisplayName("should get progress bar correctly B (100)")
    fun shouldGetProgressBarCorrectlyB() {
        val ept = ExtractionProgressTracker("Credit", TEST_DATABASE, 10, TEST_PROGRESS_FILE)
        val progressBar = ept.getProgressBar(1.00f)
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
        private const val TEST_PROGRESS_FILE = "test_progress.json"
        private const val DEFAULT_PROGRESS_FILE = """{
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

        val mockProgressInfoMap = mutableMapOf(
            "Credit" to ProgressInfo(
                lastProcessedItemId = 2, totalTimeMillis = 1, numCompleted = 0
            ),
            "Character" to ProgressInfo(
                lastProcessedItemId = 3, totalTimeMillis = 2, numCompleted = 0
            )
        )


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