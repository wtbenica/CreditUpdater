package dev.benica.credit_updater.test

import CLIParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import toPercent

internal class MainKtTest {
    private lateinit var parser: CLIParser

    @BeforeEach
    fun setUp() {
        parser = CLIParser()
    }

    @Test
    fun `parse should correctly parse command line arguments`() {
        // Arrange
        val parser = CLIParser()
        val args = arrayOf("-m", "arg1", "arg2", "-q")

        // Act
        parser.parse(args)

        // Assert
        assertTrue(parser.quiet)
        assertNotNull(parser.migrate)
        assertEquals(2, parser.migrate?.size)
        assertEquals("arg1", parser.migrate?.get(0))
        assertEquals("arg2", parser.migrate?.get(1))
        assertNull(parser.prepare)
    }

    @Test
    fun `parse should set help flag when help option is provided`() {
        // Arrange
        val parser = CLIParser()
        val args = arrayOf("-h")

        // Act
        parser.parse(args)

        // Assert
        assertTrue(parser.help)
    }

    @Test
    fun `parse should set prepare option when prepare argument is provided`() {
        // Arrange
        val parser = CLIParser()
        val args = arrayOf("-p", "prepareArg")

        // Act
        parser.parse(args)

        // Assert
        assertEquals("prepareArg", parser.prepare)
    }

    @Test
    fun `parse should handle parameter exception and print error message`() {
        // Arrange
        val parser = CLIParser()
        val args = arrayOf("-m", "arg1")

        // Act
        parser.parse(args)

        // Assert
        // Add assertions to check how you handle the exception and error message
        // For example, you can assert that a log message is printed or an error flag is set
    }

    @Test
    fun `parse should handle general exceptions and print error message`() {
        // Arrange
        val parser = CLIParser()
        val args = arrayOf("-m", "arg1", "arg2")

        // Act
        parser.parse(args)

        // Assert
        // Add assertions to check how you handle the exception and error message
        // For example, you can assert that a log message is printed or an error flag is set
    }

    @Test
    fun `toPercent should format float value as percentage with two decimal places`() {
        // Arrange
        val value = 0.75f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("75.00%", result)
    }

    @Test
    fun `toPercent should handle zero value correctly`() {
        // Arrange
        val value = 0f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("0.00%", result)
    }

    @Test
    fun `toPercent should handle negative value correctly`() {
        // Arrange
        val value = -0.5f

        // Act
        val result = value.toPercent()

        // Assert
        assertEquals("-50.00%", result)
    }
}
