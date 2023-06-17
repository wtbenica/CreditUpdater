package dev.benica.creditupdater.db

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito.mock

class DBUpdateMonitorTest {

    @Test
    @DisplayName("should calculate the remaining time correctly")
    fun shouldCalculateRemainingTimeCorrectly() {
        val conn = mock(ConnectionSource::class.java)

        val monitor = DBUpdateMonitor(conn, "test")
        assertEquals(12, monitor.getRemainingTime(totalItems = 10, averageTime = 2, numComplete = 4))
    }

    @Test
    @DisplayName("should calculate the remaining time correctly when total items is null")
    fun shouldCalculateRemainingTimeCorrectlyWhenTotalItemsIsNull() {
        val conn = mock(ConnectionSource::class.java)

        val monitor = DBUpdateMonitor(conn, "test")
        assertNull(monitor.getRemainingTime(totalItems = null, averageTime = 1000, numComplete = 4))
    }
}