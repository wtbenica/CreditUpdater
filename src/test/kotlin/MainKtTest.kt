import com.beust.jcommander.ParameterException
import dev.benica.credit_updater.cli_parser.CLIParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

internal class MainKtTest {
    private lateinit var parser: CLIParser

    @BeforeEach
    fun setUp() {
        parser = CLIParser()
    }

    @Test
    fun parseShouldNotSetAnyFlagsForNoArguments() {
        // Arrange
        val args = arrayOf<String>()

        // Act
        parser.parse(args)

        // Assert
        assertFalse(parser.help)
        assertFalse(parser.quiet)
        assertNull(parser.prepare)
        assertNull(parser.migrate)
    }

    @Test
    fun parseShouldSetQuietFlagWhenQuietOptionIsProvided() {
        // Arrange
        val args = arrayOf("-q")

        // Act
        parser.parse(args)

        // Assert
        assertTrue(parser.quiet)
    }

    @Test
    fun parseShouldSetQuietFlagWhenQuietOptionIsProvidedLong() {
        // Arrange
        val args = arrayOf("--quiet")

        // Act
        parser.parse(args)

        // Assert
        assertTrue(parser.quiet)
    }

    @Test
    fun parseShouldSetPrepareOptionWhenPrepareArgumentIsProvided() {
        // Arrange
        val args = arrayOf("-p", "arg1")

        // Act
        parser.parse(args)

        // Assert
        assertNotNull(parser.prepare)
        assertEquals("arg1", parser.prepare)
        assertNull(parser.migrate)
    }

    @Test
    fun parseShouldSetPrepareOptionWhenPrepareArgumentIsProvidedLong() {
        // Arrange
        val args = arrayOf("--prepare", "arg1")

        // Act
        parser.parse(args)

        // Assert
        assertNotNull(parser.prepare)
        assertEquals("arg1", parser.prepare)
        assertNull(parser.migrate)
    }

    @Test
    fun parseShouldFailWhenPrepareArgumentIsProvidedWithNoArguments() {
        // Arrange
        val args = arrayOf("-p")

        // Act
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }

        // Assert
        assertNull(parser.prepare)

        val expectedOutput = "Expected a value after parameter -p"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    fun parseShouldSetMigrateOptionWhenMigrateArgumentIsProvided() {
        // Arrange
        val args = arrayOf("-m", "arg1", "arg2")

        // Act
        parser.parse(args)

        // Assert
        assertNotNull(parser.migrate)
        assertEquals(2, parser.migrate?.size)
        assertEquals("arg1", parser.migrate?.get(0))
        assertEquals("arg2", parser.migrate?.get(1))
        assertNull(parser.prepare)
    }

    @Test
    fun parseShouldSetMigrateOptionWhenMigrateArgumentIsProvidedLong() {
        // Arrange
        val args = arrayOf("--migrate", "arg1", "arg2")

        // Act
        parser.parse(args)

        // Assert
        assertNotNull(parser.migrate)
        assertEquals(2, parser.migrate?.size)
        assertEquals("arg1", parser.migrate?.get(0))
        assertEquals("arg2", parser.migrate?.get(1))
        assertNull(parser.prepare)
    }

    @Test
    fun parseShouldFailWhenMigrateArgumentIsProvidedWithNoArguments() {
        // Arrange
        val args = arrayOf("-m")

        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val expectedOutput = "Expected a value after parameter -m"

        // Act
        val e = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }

        // Assert
        assertNull(parser.migrate)

        assertEquals(expectedOutput, e.message)
    }

    @Test
    fun parseShouldFailWhenMigrateArgumentIsProvidedWithOnlyOneArgument() {
        // Arrange
        val args = arrayOf("-m", "arg1")

        val expectedOutput = "Expected 2 values after -m"

        // Act
        val e = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }

        // Assert
        assertNull(parser.migrate)
        assertNull(parser.prepare)
        assertEquals(expectedOutput, e.message)
    }

    @Test
    fun parseShouldSetHelpFlagWhenHelpOptionIsProvided() {
        // Arrange
        val args = arrayOf("-h")

        // Act
        parser.parse(args)

        // Assert
        assertTrue(parser.help)
    }

    @Test
    fun parseShouldThrowParameterExceptionOnUnknownOption() {
        // Arrange
        val args = arrayOf("-x")

        // Assert
        assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
    }

    @Test
    fun parseShouldFailOnUnrecognizedArguments() {
        // Arrange
        val args = arrayOf("-x")

        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val expectedOutput = "Was passed main parameter '-x' but no main parameter was defined in your arg class"

        // Act
        val e = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }

        // Assert
        assertFalse(parser.help)
        assertFalse(parser.quiet)
        assertNull(parser.prepare)
        assertNull(parser.migrate)
        assertEquals(expectedOutput, e.message)
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
