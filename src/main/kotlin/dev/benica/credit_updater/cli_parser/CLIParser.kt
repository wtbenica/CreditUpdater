package dev.benica.credit_updater.cli_parser

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

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

    val parsedCommand: String?
        get() = commander.parsedCommand

    @Throws(ParameterException::class)
    fun parse(args: Array<String>) {
        commander.parse(*args)
    }
}