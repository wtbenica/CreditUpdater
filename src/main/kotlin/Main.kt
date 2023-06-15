import dev.benica.cli_parser.CLIParser
import dev.benica.doers.DatabaseInitializer
import dev.benica.doers.DatabaseMigrator
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: KLogger
    get() = KotlinLogging.logger { }

fun main(args: Array<String>) {
    LoggerFactory.getLogger("com.zaxxer.hikari").let { logger ->
        if (logger is ch.qos.logback.classic.Logger) {
            logger.level = ch.qos.logback.classic.Level.OFF
        }
    }
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).let { logger ->
        if (logger is ch.qos.logback.classic.Logger) {
            logger.level = ch.qos.logback.classic.Level.ALL
        }
    }

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
                    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).let { logger ->
                        if (logger is ch.qos.logback.classic.Logger) {
                            logger.level = ch.qos.logback.classic.Level.OFF
                        }
                    }
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

