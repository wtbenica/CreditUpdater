package dev.benica.creditupdater

import ch.qos.logback.classic.Level
import dev.benica.creditupdater.cli_parser.CLIParser
import dev.benica.creditupdater.db_tasks.DBInitializer
import dev.benica.creditupdater.db_tasks.DBMigrator
import dev.benica.creditupdater.di.DaggerDispatchersComponent
import dev.benica.creditupdater.di.DispatchersComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val dispatchersComponent: DispatchersComponent = DaggerDispatchersComponent.create()
    val dispatcher: CoroutineDispatcher = dispatchersComponent.provideIODispatcher()

    val logger: KLogger = KotlinLogging.logger("Main")

    runBlocking {
        val parsedArgs = CLIParser()

        try {
            parsedArgs.parse(args)
            initLoggers(parsedArgs.quiet)

            when {
                parsedArgs.help -> {
                    parsedArgs.usage
                }

                parsedArgs.prepare != null && parsedArgs.migrate != null -> {
                    withContext(dispatcher) {
                        DBInitializer(parsedArgs.prepare!!, parsedArgs.step).prepareDb()
                        DBMigrator().migrate()
                    }
                }

                parsedArgs.prepare != null -> {
                    withContext(dispatcher) {
                        DBInitializer(parsedArgs.prepare!!, parsedArgs.step).prepareDb()
                    }
                }

                parsedArgs.migrate != null -> {
                    withContext(dispatcher) {
                        DBMigrator().migrate()
                    }
                }

                else -> {
                    withContext(dispatcher) {
                        DBInitializer(startAtStep = parsedArgs.step).prepareDb()
                    }
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

private fun initLoggers(quiet: Boolean) {
    LoggerFactory.getLogger("com.zaxxer.hikari").let { logger ->
        if (logger is ch.qos.logback.classic.Logger) {
            logger.level = Level.OFF
        }
    }
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).let { logger ->
        if (logger is ch.qos.logback.classic.Logger) {
            logger.level = if (quiet) Level.WARN else Level.ALL
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

