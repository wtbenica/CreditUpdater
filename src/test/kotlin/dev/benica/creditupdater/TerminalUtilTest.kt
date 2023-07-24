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

package dev.benica.creditupdater
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TerminalUtilTest {

    @Test
    fun testUpNLines() {
        // Given
        val expectedOutput = "\u001B[2K\u001B[1A\u001B[1A\u001B[9D"

        // When
        val output = captureOutput {
            TerminalUtil.upNLines(2)
        }

        // Then
        assertEquals(expectedOutput, output)
    }

    @Test
    fun testMillisToPretty() {
        // Given
        val remainingTime = 123456789L
        val expectedOutput = "1d 10h 17m "

        // When
        val output = TerminalUtil.millisToPretty(remainingTime)

        // Then
        assertEquals(expectedOutput, output)
    }

    @Test
    fun testMillisToPrettyWithZeroDuration() {
        // Given
        val remainingTime: Long = 0
        val expectedOutput = "0s"

        // When
        val output = TerminalUtil.millisToPretty(remainingTime)

        // Then
        assertEquals(expectedOutput, output)
    }

    @Test
    fun testMillisToPrettyWithNullDuration() {
        // Given
        val remainingTime: Long? = null
        val expectedOutput = "0s"

        // When
        val output = TerminalUtil.millisToPretty(remainingTime)

        // Then
        assertEquals(expectedOutput, output)
    }

    private fun captureOutput(block: () -> Unit): String {
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        block()
        return outputStream.toString()
    }
}

//
//import dev.benica.creditupdater.TerminalUtil.Companion.CursorMovement
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import org.mockito.kotlin.mock
//import org.mockito.kotlin.verify
//import java.io.PrintStream
//
//class TerminalUtilTest {
//    private val printStream = mock<PrintStream>()
//
//    @BeforeEach
//    fun setUp() {
//        System.setOut(printStream)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        System.setOut(System.out)
//    }
//
//
//    @Test
//    @DisplayName("Should clear the current line before moving the cursor up")
//    fun upNLinesClearsCurrentLineBeforeMovingCursorUp() {
//        val n = 3
//        TerminalUtil.upNLines(n)
//
//        verify(printStream).print(CursorMovement.CLEAR_LINE.toString())
//        for (i in 1..n) {
//            verify(printStream).print(CursorMovement.UP.toString())
//        }
//        verify(printStream).print(CursorMovement.LINE_START.toString())
//    }
//
//}