import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("${args.size}")

    if (args.isEmpty())
        PrimaryDatabaseInitializer().update()
    else if (args.size == 1 && args[0] == "migrate") {
        Migrator().migrate()
    }
}

internal const val MILLIS_PER_SECOND = 1000
internal const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
internal const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
internal const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR


fun Float.toPercent(): String {
    val decimal = (this * 10000).toInt().toFloat() / 100
    return "$decimal%"
}

