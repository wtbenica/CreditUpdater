package dev.benica.creditupdater

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.benica.creditupdater.db.ExtractionProgressTracker
import java.io.File

/**
 * This class is used to start the application in interactive mode.
 *
 * In this mode, the user is prompted for the required information:
 * - Initialize database (default) or Migrate database
 * - if Initialize database:
 *    - Database name
 * - if Migrate database:
 *    - Source Database name
 *    - Target Database name
 * - Username
 * - Password
 * - Starting story id (get default from file, else 0)
 */
class InteractiveStartup {
    companion object {
        /** This method starts the application in interactive mode. */
        fun start(): StartupArguments {
            val databaseName: String
            val extractedType: ExtractedType?
            val targetDatabaseName: String?

            println("Starting in interactive mode")

            // Prompt the user for the required information.
            val databaseTask = prompt("Initialize database or Migrate database? (I/M)", "I").uppercase()
            if (databaseTask == "I") {
                extractedType = when (prompt("New, Characters or Credits? (N/H/R)", "N").uppercase()) {
                    "H" -> ExtractedType.CHARACTERS
                    "R" -> ExtractedType.CREDIT
                    else -> ExtractedType.NEW
                }
                databaseName = prompt("Database name", Credentials.PRIMARY_DATABASE)
                targetDatabaseName = null
            } else {
                databaseName = prompt("Source database name", Credentials.PRIMARY_DATABASE)
                extractedType = null
                targetDatabaseName = prompt("Target database name", Credentials.INCOMING_DATABASE)
            }

            val username = prompt("Username", Credentials.USERNAME_INITIALIZER)
            val password = prompt("Password", Credentials.PASSWORD_INITIALIZER)

            val file = File("progress.json")
            val progressInfoMap = file.loadProgressInfo()
            val next = progressInfoMap.getOrDefault(extractedType?.text, null)?.lastProcessedItemId ?: 0

            val startingStoryId = prompt("Starting story id", next.toString()).toInt()

            return StartupArguments(
                when (databaseTask) {
                    "I" -> DatabaseTask.INITIALIZE
                    else -> DatabaseTask.MIGRATE
                },
                extractedType,
                databaseName,
                targetDatabaseName,
                username,
                password,
                startingStoryId
            )
        }

        private fun prompt(message: String, default: String = ""): String {
            println("$message${if (default.isNotBlank()) " [$default]" else ""}: ")
            val input = readlnOrNull()?.trim()
            return input.takeIf { it?.isNotBlank() == true } ?: default
        }
    }
}

data class StartupArguments(
    val databaseTask: DatabaseTask,
    val extractedType: ExtractedType?,
    val databaseName: String,
    val targetDatabaseName: String?,
    val username: String,
    val password: String,
    val startingStoryId: Int
)

enum class DatabaseTask {
    INITIALIZE,
    MIGRATE
}

enum class ExtractedType(val text: String, val stepNumber: Int) {
    NEW("New", 1),
    CHARACTERS("Character", 2),
    CREDIT("Credit", 3)
}

/**
 * Reads a json file and returns a progress map.
 */
fun File.loadProgressInfo(): MutableMap<String, ExtractionProgressTracker.Companion.ProgressInfo> =
    if (exists()) {
        val gson = Gson()
        reader().use { reader ->
            gson.fromJson(
                reader,
                object : TypeToken<MutableMap<String, ExtractionProgressTracker.Companion.ProgressInfo>>() {}.type
            )
        }
    } else {
        mutableMapOf()
    }
