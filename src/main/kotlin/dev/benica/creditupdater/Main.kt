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

            if (parsedArgs.interactive) {
                interactiveStartup()
            } else {
                useArgsStartup(parsedArgs)
            }
        } catch (e: Exception) {
            val msg = if (e.message?.startsWith("Was passed main parameter '") == true) {
                "Unrecognized argument: ${e.message?.substringAfter("'")?.substringBefore("'")}"
            } else {
                e.message
            }
            logger.error { msg }
            parsedArgs.usage
        }
    }
}

private suspend fun useArgsStartup(parsedArgs: CLIParser) {
    if (parsedArgs.help) {
        parsedArgs.usage
    } else {
        if (parsedArgs.prepare != null) {
            @Suppress("kotlin:S6307")
            DBInitializer(
                targetSchema = parsedArgs.prepare!!,
                startAtStep = parsedArgs.step,
                startingId = parsedArgs.startingId
            ).prepareDb()
        }

        if (parsedArgs.migrate != null) {
            DBMigrator().migrate()
        } else if (parsedArgs.prepare == null) {
            @Suppress("kotlin:S6307")
            DBInitializer(
                startAtStep = parsedArgs.step,
                startingId = parsedArgs.startingId
            ).prepareDb()
            DBMigrator().migrate()
        }
    }
}

private suspend fun interactiveStartup() {
    val startupArguments: StartupArguments = InteractiveStartup.start()
    println(startupArguments)
    when (startupArguments.databaseTask) {
        DatabaseTask.INITIALIZE -> {
            DBInitializer(
                targetSchema = startupArguments.databaseName,
                startAtStep = startupArguments.extractedType?.stepNumber ?: 0,
                startingId = startupArguments.startingStoryId
            ).prepareDb()
        }

        DatabaseTask.MIGRATE -> {
            DBMigrator().migrate()
        }
    }
}

fun initLoggers(quiet: CLIParser) {
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
