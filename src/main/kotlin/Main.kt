import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import dev.benica.credit_updater.converter.logger
import dev.benica.credit_updater.doers.Migrator
import dev.benica.credit_updater.doers.PrimaryDatabaseInitializer
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        println(args.size.toString())

        val app = CLIParser()

        try {
            app.parse(args)

            when {
                app.help -> {
                    app.usage
                    return@runBlocking
                }

                app.quiet -> {
                    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")
                }

                app.prepare != null -> {
                    PrimaryDatabaseInitializer(app.prepare).update()
                }

                app.migrate != null -> {
                    Migrator().migrate()
                }

                else -> {
                    PrimaryDatabaseInitializer().update()
                }
            }
        } catch (e: com.beust.jcommander.ParameterException) {
            logger.error("Error parsing command line arguments", e)
            app.usage
        } catch (e: Exception) {
            logger.error("Error parsing command line arguments", e)
        } finally {
            app.parsedCommand
        }

        if (args.isEmpty())
            PrimaryDatabaseInitializer().update()
        else if (args.size == 1 && args[0] == "migrate") {
            Migrator().migrate()
        }
    }
}

class CLIParser {
    private val commander: JCommander = JCommander.newBuilder()
        .addObject(this)
        .programName("CreditUpdater")
        .build()

    @Parameter(names = ["-h", "--help"], description = "print this message")
    var help: Boolean = false

    @Parameter(names = ["-q", "--quiet"], description = "Suppress non-error messages")
    var quiet: Boolean = false

    @Parameter(
        names = ["-p", "--prepare"],
        description = "Prepare new primary database",
        arity = 1,
    )
    var prepare: String? = null

    @Parameter(names = ["-m", "--migrate"], description = "Migrate primary database", arity = 2)
    var migrate: List<String>? = null

    val usage
        get() = commander.usage()

    val parsedCommand
        get() = commander.parsedCommand

    fun parse(args: Array<String>) {
        val commander = JCommander.newBuilder()
            .addObject(this)
            .build()
        commander.programName = "CreditUpdater"

        try {
            commander.parse(*args)
        } catch (e: com.beust.jcommander.ParameterException) {
            println(e.message)
            commander.usage()
        } catch (e: Exception) {
            println(e.message)
        } finally {
            commander.parsedCommand
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

