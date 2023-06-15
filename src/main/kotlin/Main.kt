import com.zaxxer.hikari.HikariConfig
import dev.benica.cli_parser.CLIParser
import dev.benica.doers.DatabaseMigrator
import dev.benica.doers.DatabaseInitializer
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import java.util.logging.Level
import java.util.logging.Logger

private val logger: KLogger
    get() = KotlinLogging.logger { }

fun main(args: Array<String>) {
    Logger.getLogger("com.zaxxer.hikari.PoolBase").level = Level.OFF
    Logger.getLogger("com.zaxxer.hikari.pool.HikariPool").level = Level.OFF
    Logger.getLogger("com.zaxxer.hikari.HikariDataSource").level = Level.OFF
    Logger.getLogger("com.zaxxer.hikari.HikariConfig").level = Level.OFF
    Logger.getLogger("com.zaxxer.hikari.util.DriverDataSource").level = Level.OFF

    runBlocking {
        val app = CLIParser()

        try {
            app.parse(args)

            when {
                app.help -> {
                    app.usage
                    return@runBlocking
                }

                app.quiet -> {
                    // TODO: Set logger level to ERROR
                }

                app.prepare != null -> {
                    DatabaseInitializer(app.prepare).prepareDatabase()
                }

                app.migrate != null -> {
                    DatabaseMigrator().migrate()
                }

                else -> {
                    DatabaseInitializer().prepareDatabase()
                }
            }
        } catch (e: com.beust.jcommander.ParameterException) {
            val msg = if (e.message?.startsWith("Was passed main parameter '") == true) {
                "Unrecognized argument: ${e.message?.substringAfter("'")?.substringBefore("'")}"
            } else {
                e.message
            }
            logger.error { msg }
            app.usage
        } catch (e: Exception) {
            logger.error { e }
        } finally {
            app.parsedCommand
        }
    }
}

internal const val MILLIS_PER_SECOND = 1000
internal const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
internal const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
internal const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR


fun Float.toPercent(): String {
    val decimal = String.format("%.2f", this * 100)
    return "$decimal%"
}

