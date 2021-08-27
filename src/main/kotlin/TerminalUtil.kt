class TerminalUtil {
    companion object {
        enum class CursorMovement(val value: String) {
            UP("[1A"), LINE_START("[9D"), CLEAR("[2J");

            override fun toString(): String = value
        }

        fun clearTerminal() {
            println("$ESC${CursorMovement.CLEAR}")     // clear terminal first
        }

        fun upFourLines() {
            for (aec in cursorUpFourLines) {
                print("$ESC$aec")
            }
        }

        fun upNLines(n: Int) {
            for (i in 0..n) {
                print("$ESC${CursorMovement.UP}")
            }
            print("$ESC${CursorMovement.LINE_START}")
        }

        //        var storyCount: Int? = null
        private const val ESC = "\u001B"  // escape code

        private val cursorUpFourLines =
            arrayOf(
                CursorMovement.UP,
                CursorMovement.UP,
                CursorMovement.UP,
                CursorMovement.UP,
                CursorMovement.LINE_START
            )

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