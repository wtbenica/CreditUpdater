package dev.benica.creditupdater.db

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ExtractionProgressTrackerTest {

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
}