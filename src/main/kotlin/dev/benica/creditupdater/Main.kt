package dev.benica.creditupdater

import ch.qos.logback.classic.Level
import dev.benica.creditupdater.cli_parser.CLIParser
import dev.benica.creditupdater.db_tasks.DBInitializer
import dev.benica.creditupdater.db_tasks.DBMigrator
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger: KLogger = KotlinLogging.logger("Main")

    runBlocking {
        val parsedArgs = CLIParser()

        try {
            parsedArgs.parse(args)
            initLoggers(parsedArgs)

            when {
                parsedArgs.help -> {
                    parsedArgs.usage
                }

                parsedArgs.prepare != null && parsedArgs.migrate != null -> {
                    @Suppress("kotlin:S6307")
                    DBInitializer(
                        targetSchema = parsedArgs.prepare!!,
                        startAtStep = parsedArgs.step,
                        startingId = parsedArgs.startingId
                    ).prepareDb()
                    DBMigrator().migrate()
                }

                parsedArgs.prepare != null -> {
                    @Suppress("kotlin:S6307")
                    DBInitializer(
                        targetSchema = parsedArgs.prepare!!,
                        startAtStep = parsedArgs.step,
                        startingId = parsedArgs.startingId
                    ).prepareDb()
                }

                parsedArgs.migrate != null -> {
                    DBMigrator().migrate()
                }

                else -> {
                    @Suppress("kotlin:S6307")
                    DBInitializer(
                        startAtStep = parsedArgs.step,
                        startingId = parsedArgs.startingId
                    ).prepareDb()
                    DBMigrator().migrate()
                }
            }
        } catch (e: com.beust.jcommander.ParameterException) {
            val msg = if (e.message?.startsWith("Was passed main parameter '") == true) {
                "Unrecognized argument: ${e.message?.substringAfter("'")?.substringBefore("'")}"
            } else {
                e.message
            }
            logger.error { msg }
            parsedArgs.usage
        } catch (e: Exception) {
            logger.error { e }
        } finally {
            parsedArgs.parsedCommand
        }
    }
}

private fun initLoggers(quiet: CLIParser) {
    val loggerLevel = when {
        quiet.verbose -> Level.ALL
        quiet.debug -> Level.DEBUG
        quiet.quiet -> Level.WARN
        else -> Level.INFO
    }

    val hikariLoggerLevel = when {
        quiet.verbose -> Level.WARN
        quiet.debug -> Level.ERROR
        quiet.quiet -> Level.OFF
        else -> Level.ERROR
    }

    LoggerFactory.getLogger("com.zaxxer.hikari").let { logger ->
        if (logger is ch.qos.logback.classic.Logger) {
            logger.level = hikariLoggerLevel
        }
    }
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).let { logger ->
        if (logger is ch.qos.logback.classic.Logger) {
            logger.level = loggerLevel
        }
    }
}

private const val MILLIS_PER_SECOND = 1000
internal const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
internal const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
internal const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR

fun Float.toPercent(): String {
    val decimal = String.format("%.2f", this * 100)
    return "$decimal%"
}

