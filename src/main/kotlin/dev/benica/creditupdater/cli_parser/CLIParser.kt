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

package dev.benica.creditupdater.cli_parser

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

class CLIParser {
    private val commander: JCommander = JCommander.newBuilder()
        .addObject(this)
        .programName("CreditUpdater")
        .build()

    @Parameter(
        names = ["-h", "--help"],
        description = "print this message",
    )
    var help: Boolean = false

    @Parameter(
        names = ["-q", "--quiet"],
        description = "Sets logger level to WARN",
    )
    var quiet: Boolean = false

    @Parameter(
        names = ["-v", "--verbose"],
        description = "Prints all logs",
    )
    var verbose: Boolean = false

    @Parameter(
        names = ["-d", "--debug"],
        description = "Sets logger level to DEBUG",
    )
    var debug: Boolean = false

    @Parameter(
        names = ["-p", "--prepare"],
        description = "Prepare new primary database",
        arity = 1,
    )
    var prepare: String? = null

    @Parameter(
        names = ["-s", "--step"],
        description = "Start at the indicated step, skipping completed steps.",
        arity = 1,
    )
    private var _mStep: Int? = null
    val step: Int
        get() = _mStep ?: 1

    @Parameter(
        names = ["-m", "--migrate"],
        description = "Migrate primary database",
        arity = 2,
    )
    var migrate: List<String>? = null

    @Parameter(
        names = ["-n", "--init"],
        description = "Starting story id",
        validateWith = [StartIdValidator::class],
        arity = 1
    )
    var startingId: Int? = null

    @Parameter(
        names = ["-i", "--interactive"],
        description = "Start in interactive mode",
    )
    var interactive: Boolean = false

    /** Prints the usage information for the command. */
    val usage: Unit
        get() = commander.usage()

    /**
     * Whether any flags (other than interactive) were passed.
     */
    private val anyFlags: Boolean
        get() = prepare != null || migrate != null || startingId != null || help

    @Throws(ParameterException::class)
    fun parse(args: Array<String>) {
        commander.parse(*args)
        validate()
    }

    private fun validate() {
        if (_mStep != null && prepare != null && _mStep !in 2..4) {
            _mStep = 1
            throw ParameterException("Parameter 'step' must be between 2 and 4 (inclusive)")
        }
        if (_mStep != null && prepare == null && migrate == null) {
            _mStep = 1
            throw ParameterException("Parameter 'step' should only be used with --prepare or --migrate")
        }

        // if interactive, nothing else can be set
        if (interactive && anyFlags) {
            throw ParameterException("Parameter 'interactive' cannot be used with other parameters")
        }
    }

    class StartIdValidator : IParameterValidator {
        override fun validate(name: String?, value: String?) {
            if (value != null && (value.toIntOrNull() == null || value.toInt() < 0)) {
                throw ParameterException("Parameter $name should be a non-negative integer")
            }
        }
    }
}
