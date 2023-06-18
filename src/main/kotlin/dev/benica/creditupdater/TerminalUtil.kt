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
    }
}