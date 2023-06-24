package dev.benica.creditupdater

import dev.benica.creditupdater.cli_parser.CLIParser
import dev.benica.creditupdater.db.toPercent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MainKtTest {
    private lateinit var parser: CLIParser

    @BeforeEach
    fun setUp() {
        parser = CLIParser()
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
}
