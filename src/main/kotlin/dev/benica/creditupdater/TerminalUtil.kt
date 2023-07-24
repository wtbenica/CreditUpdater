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

/**
 * Terminal util - utility functions for the terminal, mainly for tidying
 * output.
 *
 * @constructor Create empty Terminal util
 */
class TerminalUtil {
    companion object {
        enum class CursorMovement(private val value: String) {
            UP("[1A"), LINE_START("[9D"), CLEAR("[2J"), CLEAR_LINE("[2K");

            fun moveCursor() {
                print(toString())
            }

            private val escapeCode = "\u001B"
            override fun toString(): String = "$escapeCode$value"
        }

        /**
         * Up n lines - move the cursor up n lines and to the start of the line.
         *
         * @param n the number of lines to move up
         */
        fun upNLines(n: Int) {
            CursorMovement.CLEAR_LINE.moveCursor()
            for (i in 1..n) {
                CursorMovement.UP.moveCursor()
            }
            CursorMovement.LINE_START.moveCursor()
        }

        /**
         * Converts a duration in milliseconds to a human-readable string format.
         *
         * @param remainingTime the duration in milliseconds
         * @return a string representing the duration in the format "Xd Yh Zm",
         *     where X is the number of days, Y is the number of hours, and Z is
         *     the number of minutes. If any of these values is zero, the
         *     corresponding part of the string is omitted. If the duration is less
         *     than one minute, the function returns the string "0s". If the
         *     duration is null, the function also returns "0s".
         */
        internal fun millisToPretty(remainingTime: Long?): String = remainingTime?.let {
            if (it < MILLIS_PER_MINUTE) {
                "0s"
            } else {
                var remainingTime1 = it
                val days: Long = remainingTime1 / MILLIS_PER_DAY
                remainingTime1 -= days * MILLIS_PER_DAY
                val hours: Long = remainingTime1 / MILLIS_PER_HOUR
                remainingTime1 -= hours * MILLIS_PER_HOUR
                val minutes: Long = remainingTime1 / MILLIS_PER_MINUTE
                "${if (days > 0) "${days}d " else ""}${if (hours > 0) "${hours}h " else ""}${if (minutes > 0) "${minutes}m " else ""}"
            }
        } ?: "0s"


        private const val MILLIS_PER_SECOND = 1000
        private const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
        private const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
        private const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR
    }
}