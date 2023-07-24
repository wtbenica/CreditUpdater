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

import ch.qos.logback.classic.Level
import dev.benica.creditupdater.cli_parser.CLIParser
import dev.benica.creditupdater.db_tasks.DBInitializer
import dev.benica.creditupdater.db_tasks.DBMigrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

internal suspend fun useArgsStartup(parsedArgs: CLIParser) {
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
            DBMigrator(
                sourceSchema = parsedArgs.migrate!![0],
                targetSchema = parsedArgs.migrate!![1],
                startAtStep = parsedArgs.step,
                startingId = parsedArgs.startingId
            ).migrate()
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

internal suspend fun interactiveStartup() {
    val startupArguments: StartupArguments = InteractiveStartup.start()
    println(startupArguments)
    when (startupArguments.databaseTask) {
        DatabaseTask.INITIALIZE -> {
            @Suppress("kotlin:S6310")
            withContext(Dispatchers.IO) {
                DBInitializer(
                    targetSchema = startupArguments.databaseName,
                    startAtStep = startupArguments.extractedType?.stepNumber ?: 0,
                    startingId = startupArguments.startingStoryId
                ).prepareDb()
            }
        }

        DatabaseTask.MIGRATE -> {
            DBMigrator(
                sourceSchema = startupArguments.databaseName,
                targetSchema = startupArguments.targetDatabaseName ?: Credentials.INCOMING_DATABASE,
                startingId = startupArguments.startingStoryId
            ).migrate()
        }
    }
}

internal fun initLoggers(quiet: CLIParser) {
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
