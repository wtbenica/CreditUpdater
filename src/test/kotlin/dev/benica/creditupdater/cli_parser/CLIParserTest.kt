/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.creditupdater.cli_parser

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

    // no args
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

    // bad args
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
    @DisplayName("should throw parameter exception on unknown option")
    fun shouldThrowParameterExceptionOnUnknownOption() {
        val args = arrayOf("-x")
        assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
    }


    // -h, --help
    @Test
    @DisplayName("should parse help flag")
    fun shouldParseHelpFlag() {
        val args = arrayOf("-h")
        assertFalse(parser.help)
        parser.parse(args)
        assertTrue(parser.help)
    }

    @Test
    @DisplayName("should parse help flag with long name")
    fun shouldParseHelpFlagWithLongName() {
        val args = arrayOf("--help")
        assertFalse(parser.help)
        parser.parse(args)
        assertTrue(parser.help)
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

        @Suppress("RegExpRepeatedSpace", "RegExpDuplicateCharacterInClass")
        val expectedOutput = """Usage: CreditUpdater [options]
  Options:
    -d, --debug
      Sets logger level to DEBUG
      Default: false
    -h, --help
      print this message
      Default: false
    -n, --init
      Starting story id
    -i, --interactive
      Start in interactive mode
      Default: false
    -m, --migrate
      Migrate primary database
    -p, --prepare
      Prepare new primary database
    -q, --quiet
      Sets logger level to WARN
      Default: false
    -s, --step
      Start at the indicated step, skipping completed steps.
    -v, --verbose
      Prints all logs
      Default: false"""

        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val originalOut = System.out
        System.setOut(printStream)
        this.parser.usage
        System.setOut(originalOut)
        val actualOutput = outputStream.toString().trim()
        assertEquals(expectedOutput, actualOutput)
    }

    // -q, --quiet
    @Test
    @DisplayName("should set quiet flag when quiet option is provided")
    fun shouldSetQuietFlagWhenQuietOptionIsProvided() {
        val args = arrayOf("-q")
        parser.parse(args)
        assertTrue(parser.quiet)
    }

    @Test
    @DisplayName("should set quiet flag when quiet option is provided with long name")
    fun shouldSetQuietFlagWhenQuietOptionIsProvidedWithLongName() {
        val args = arrayOf("--quiet")
        parser.parse(args)
        assertTrue(parser.quiet)
    }

    @Test
    fun parseShouldSetQuietFlagWhenQuietOptionIsProvidedLong() {
        val args = arrayOf("--quiet")
        parser.parse(args)
        assertTrue(parser.quiet)
    }

    // -v, --verbose
    @Test
    @DisplayName("should set verbose flag when verbose option is provided")
    fun shouldSetVerboseFlagWhenVerboseOptionIsProvided() {
        val args = arrayOf("-v")
        parser.parse(args)
        assertTrue(parser.verbose)
    }

    @Test
    @DisplayName("should set verbose flag when verbose option is provided with long name")
    fun shouldSetVerboseFlagWhenVerboseOptionIsProvidedWithLongName() {
        val args = arrayOf("--verbose")
        parser.parse(args)
        assertTrue(parser.verbose)
    }

    // -d, --debug
    @Test
    @DisplayName("should set debug flag when debug option is provided")
    fun shouldSetDebugFlagWhenDebugOptionIsProvided() {
        val args = arrayOf("-d")
        parser.parse(args)
        assertTrue(parser.debug)
    }

    @Test
    @DisplayName("should set debug flag when debug option is provided with long name")
    fun shouldSetDebugFlagWhenDebugOptionIsProvidedWithLongName() {
        val args = arrayOf("--debug")
        parser.parse(args)
        assertTrue(parser.debug)
    }

    // -p, --prepare
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
    @DisplayName("should set prepare option when prepare argument is provided with long name")
    fun parseShouldSetPrepareOptionWhenPrepareArgumentIsProvidedWithLongName() {
        val args = arrayOf("--prepare", "arg1")
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
    fun parseShouldSetPrepareOptionWhenPrepareArgumentIsProvidedLong() {
        val args = arrayOf("--prepare", "arg1")
        parser.parse(args)
        assertNotNull(parser.prepare)
        assertEquals("arg1", parser.prepare)
        assertNull(parser.migrate)
    }

    // -s, --step
    @Test
    @DisplayName("setp should fail when step is provided but not prepare or migrate")
    fun shouldSetStepOptionWhenStepArgumentIsProvided() {
        val args = arrayOf("-s", "2")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertEquals(1, parser.step)
        val expectedOutput = "Parameter 'step' should only be used with --prepare or --migrate"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    @DisplayName("should fail when step argument is provided with no arguments")
    fun shouldFailWhenStepArgumentIsProvidedWithNoArguments() {
        val args = arrayOf("-s")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertEquals(1, parser.step)
        val expectedOutput = "Expected a value after parameter -s"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    @DisplayName("should fail when step argument is provided with non-numeric argument")
    fun shouldFailWhenStepArgumentIsProvidedWithNonNumericArgument() {
        val args = arrayOf("-s", "a")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertEquals(1, parser.step)
        val expectedOutput = "\"-s\": couldn't convert \"a\" to an integer"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    @DisplayName("should fail when --prepare and --step but step is out of range 2..4")
    fun shouldFailWhenPrepareAndStepButStepIsOutOfRange() {
        val args = arrayOf("-p", "inf_lb_test", "-s", "5")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        val expectedOutput = "Parameter 'step' must be between 2 and 4 (inclusive)"
        assertEquals(expectedOutput, exception.message)
        assertEquals(1, parser.step)
    }

    // -m, --migrate
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
    fun parseShouldSetMigrateOptionWhenMigrateArgumentIsProvidedLong() {
        val args = arrayOf("--migrate", "arg1", "arg2")
        parser.parse(args)
        assertNotNull(parser.migrate)
        assertEquals(2, parser.migrate?.size)
        assertEquals("arg1", parser.migrate?.get(0))
        assertEquals("arg2", parser.migrate?.get(1))
        assertNull(parser.prepare)
    }

    // -n, --init
    @Test
    @DisplayName("should set init option when init argument is provided")
    fun shouldSetInitOptionWhenInitArgumentIsProvided() {
        val args = arrayOf("-n", "123")
        parser.parse(args)
        assertNotNull(parser.startingId)
        assertEquals(123, parser.startingId)
    }

    @Test
    @DisplayName("should fail when init argument is provided with no arguments")
    fun shouldFailWhenInitArgumentIsProvidedWithNoArguments() {
        val args = arrayOf("-n")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertNull(parser.startingId)
        val expectedOutput = "Expected a value after parameter -n"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    @DisplayName("should fail when init argument is provided with non-numeric argument")
    fun shouldFailWhenInitArgumentIsProvidedWithNonNumericArgument() {
        val args = arrayOf("-n", "a")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        assertNull(parser.startingId)
        val expectedOutput = "Parameter -n should be a non-negative integer"
        assertEquals(expectedOutput, exception.message)
    }

    // -i, --interactive
    @Test
    @DisplayName("should set interactive option when interactive argument is provided")
    fun shouldSetInteractiveOptionWhenInteractiveArgumentIsProvided() {
        val args = arrayOf("-i")
        parser.parse(args)
        assertTrue(parser.interactive)
    }

    @Test
    @DisplayName("should set interactive option when interactive argument is provided long")
    fun shouldSetInteractiveOptionWhenInteractiveArgumentIsProvidedLong() {
        val args = arrayOf("--interactive")
        parser.parse(args)
        assertTrue(parser.interactive)
    }

    @Test
    @DisplayName("should throw ParameterException if interactive argument is provided with arguments")
    fun shouldThrowParameterExceptionIfInteractiveArgumentIsProvidedWithArguments() {
        val args = arrayOf("-i", "arg1")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        val expectedOutput = "Was passed main parameter 'arg1' but no main parameter was defined in your arg class"
        assertEquals(expectedOutput, exception.message)
    }

    @Test
    @DisplayName("should throw ParameterException if interactive flag is used with any other flags")
    fun shouldThrowParameterExceptionIfInteractiveFlagIsUsedWithAnyOtherFlags() {
        val args = arrayOf("-i", "-p", "inf_lb_test")
        val exception = assertThrows(ParameterException::class.java) {
            parser.parse(args)
        }
        val expectedOutput = "Parameter 'interactive' cannot be used with other parameters"
        assertEquals(expectedOutput, exception.message)
    }
}