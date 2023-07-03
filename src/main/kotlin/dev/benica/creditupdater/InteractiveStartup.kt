package dev.benica.creditupdater

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.benica.creditupdater.db.ExtractionProgressTracker
import java.io.File
import java.io.FileReader

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
            val databaseTask = prompt("(I)nitialize database or (M)igrate database?", "I").uppercase()
            if (databaseTask == "I") {
                extractedType = if (prompt("C(h)aracters or C(r)edits", "h").uppercase() == "H") {
                    ExtractedType.CHARACTERS
                } else {
                    ExtractedType.CREDIT
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
            println("$message${if (default.isNotBlank()) " [$default]" else ""}:")
            val input = readlnOrNull()?.trim()
            println("Got input: $input")
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
    CHARACTERS("Character", 2),
    CREDIT("Credit", 3)
}

fun File.loadProgressInfo(): MutableMap<String, ExtractionProgressTracker.Companion.ProgressInfo> =
    if (exists()) {
        val gson = Gson()
        FileReader(this).use { reader ->
            gson.fromJson(reader, object : TypeToken<MutableMap<String, ExtractionProgressTracker.Companion.ProgressInfo>>() {}.type)
        }
    } else {
        mutableMapOf()
    }