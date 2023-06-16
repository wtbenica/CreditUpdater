package dev.benica.cli_parser

import com.beust.jcommander.ParameterException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CLIParserTest {
    private lateinit var parser: CLIParser

    @BeforeEach
    fun setUp() {
        this.parser = CLIParser()
    }

    @Test
    @DisplayName("should parse no arguments")
    fun shouldParseNoArguments() {
        val args = arrayOf<String>()
        assertFalse(parser.help)
        assertFalse(parser.quiet)
        assertNull(parser.prepare)
        assertNull(parser.migrate)
        parser.parse(args)
        assertFalse(parser.help)
        assertFalse(parser.quiet)
        assertNull(parser.prepare)
        assertNull(parser.migrate)
    }

    @Test
    @DisplayName("should set quiet flag when quiet option is provided")
    fun shouldSetQuietFlagWhenQuietOptionIsProvided() {
        val args = arrayOf("-q")
        parser.parse(args)
        assertTrue(parser.quiet)
    }

    @Test
    @DisplayName("should set prepare option when prepare argument is provided")
    fun parseShouldSetPrepareOptionWhenPrepareArgumentIsProvided() {
        val args = arrayOf("-p", "arg1")
        parser.parse(args)
        assertNotNull(parser.prepare)
        assertEquals("arg1", parser.prepare)
        assertNull(parser.migrate)
    }

    @Test
    @DisplayName("should fail when prepare argument is provided with no arguments")
    fun shouldFailWhenPrepareArgumentIsProvidedWithNoArguments() {
        val args = arrayOf("-p")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertNull(parser.prepare)
        val expectedOutput = "Expected a value after parameter -p"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    @DisplayName("should set migrate option when migrate argument is provided")
    fun shouldSetMigrateOptionWhenMigrateArgumentIsProvided() {
        val args = arrayOf("-m", "arg1", "arg2")
        assertNull(parser.migrate)
        parser.parse(args)
        assertNotNull(parser.migrate)
        assertEquals(2, parser.migrate?.size)
        assertEquals("arg1", parser.migrate?.get(0))
        assertEquals("arg2", parser.migrate?.get(1))
        assertNull(parser.prepare)
    }

    @Test
    @DisplayName("should fail when migrate argument is provided with only one argument")
    fun shouldFailWhenMigrateArgumentIsProvidedWithNoArguments() {
        val args = arrayOf("-m")
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        val expectedOutput = "Expected a value after parameter -m"
        val e = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertNull(parser.migrate)
        assertEquals(expectedOutput, e.message)
    }

    @Test
    @DisplayName("should fail when migrate argument is provided with only one argument")
    fun shouldFailWhenMigrateArgumentIsProvidedWithOnlyOneArgument() {
        val args = arrayOf("-m", "arg1")
        val expectedOutput = "Expected 2 values after -m"
        val e = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertNull(parser.migrate)
        assertNull(parser.prepare)
        assertEquals(expectedOutput, e.message)
    }

    @Test
    @DisplayName("should set help flag when help option is provided")
    fun shouldSetHelpFlagWhenHelpOptionIsProvided() {
        val args = arrayOf("-h")
        parser.parse(args)
        assertTrue(parser.help)
    }

    @Test
    @DisplayName("should print usage when help option is provided")
    fun shouldPrintUsageWhenHelpOptionIsProvided() {
        this.parser.parse(arrayOf("-h"))


        val expectedOutput = Regex(
            """Usage: CreditUpdater [options]
 Options:
 -h, --help
 print this message
 Default: false
 -m, --migrate
 Migrate primary database
 -p, --prepare
 Prepare new primary database
 -q, --quiet
 Only warnings and errors will be logged.
 Default: false
 -s, --step
 Start at the indicated step, skipping completed steps.""".trimIndent()
        )
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val originalOut = System.out
        System.setOut(printStream)
        this.parser.usage
        System.setOut(originalOut)
        val actualOutput = outputStream.toString().trim()
        assertTrue(expectedOutput.matches(actualOutput))
    }

    @Test
    @DisplayName("should throw parameter exception on unknown option")
    fun shouldThrowParameterExceptionOnUnknownOption() {
        val args = arrayOf("-x")
        assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
    }

    @Test
    @DisplayName("should fail on unrecognized arguments")
    fun shouldFailOnUnrecognizedArguments() {
        val args = arrayOf("-x")
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        val expectedOutput = "Was passed main parameter '-x' but no main parameter was defined in your arg class"
        val e = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertFalse(parser.help)
        assertFalse(parser.quiet)
        assertNull(parser.prepare)
        assertNull(parser.migrate)
        assertEquals(expectedOutput, e.message)
    }

    @Test
    fun parseShouldSetQuietFlagWhenQuietOptionIsProvidedLong() {
        val args = arrayOf("--quiet")
        parser.parse(args)
        assertTrue(parser.quiet)
    }

    @Test
    fun parseShouldSetPrepareOptionWhenPrepareArgumentIsProvidedLong() {
        val args = arrayOf("--prepare", "arg1")
        parser.parse(args)
        assertNotNull(parser.prepare)
        assertEquals("arg1", parser.prepare)
        assertNull(parser.migrate)
    }

    @Test
    fun parseShouldSetMigrateOptionWhenMigrateArgumentIsProvidedLong() {
        val args = arrayOf("--migrate", "arg1", "arg2")
        parser.parse(args)
        assertNotNull(parser.migrate)
        assertEquals(2, parser.migrate?.size)
        assertEquals("arg1", parser.migrate?.get(0))
        assertEquals("arg2", parser.migrate?.get(1))
        assertNull(parser.prepare)
    }

    @Test
    fun parseShouldPrintUsageWhenHelpOptionIsProvided() {
        val args = arrayOf("-h")
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        val expectedOutput = "Hello, world!"
        parser.parse(args)
        assertEquals(expectedOutput, outputStream.toString().trim())
    }
}