package dev.benica.creditupdater.extractor.utils

import dev.benica.creditupdater.extractor.cleanup
import dev.benica.creditupdater.models.Character
import dev.benica.creditupdater.models.Individual
import dev.benica.creditupdater.models.Team

class CharacterParser {
    companion object {
        /**
         * Parses a character string into a list of [Character] objects.
         *
         * @param characters the character string to parse. a character string is a
         *     semicolon-separated list of character names, with optional bracketed
         *     text containing alter egos or team memberships, and optional
         *     parenthetical text containing appearance notes.
         * @return a list of [Character] objects.
         */
        fun parseCharacters(characters: String): List<Character> {
            val fixedInput = fixMissingBrackets(characters)
            val characterList = mutableListOf<Character>()
            val characterStrings = splitOnOuterSemicolons(fixedInput)
            characterStrings.forEach { characterString ->
                val (name, bracketedText, appearanceInfo) = splitString(characterString)

                val (alterEgo: String?, membership: String?) =
                    parseBracketedText(bracketedText)

                val isTeam = membership != null

                if (name.isNotEmpty()) {
                    characterList.add(
                        if (isTeam) {
                            Team(name = name, members = bracketedText, appearanceInfo = appearanceInfo)
                        } else {
                            Individual(name = name, alterEgo = alterEgo, appearanceInfo = appearanceInfo)
                        }
                    )
                }
            }
            return characterList
        }

        /**
         * Attempts to fix malformed character strings by adding missing
         * brackets. It expects a string in the format: "name/team name
         * [alter ego/membership] (appearance notes)"
         *
         * @param input the character string to fix.
         * @return the fixed character string
         */
        internal fun fixMissingBrackets(input: String): String {
            val stack = mutableListOf<Char>()
            val fixedString = StringBuilder()
            var depth = 0
            var semicolonSeen = false

            for (char in input) {
                when (char) {
                    '[' -> {
                        stack.add(char)
                        depth++
                    }

                    ']' -> {
                        if (stack.isNotEmpty() && stack.last() == '[') {
                            depth--
                            semicolonSeen = false
                            stack.removeAt(stack.size - 1)
                        }
                    }

                    ';' -> {
                        if (depth == 2 && !semicolonSeen) {
                            semicolonSeen = true
                        } else if (depth == 2) {
                            fixedString.append(']') // Add missing closing bracket
                            depth--
                            semicolonSeen = false
                            stack.removeAt(stack.size - 1)
                        }
                    }
                }
                fixedString.append(char)
            }

            while (stack.isNotEmpty() && stack.last() == '[') {
                fixedString.append(']')
                stack.removeAt(stack.size - 1)
            }

            return fixedString.toString()
        }

        /**
         * Splits a string into substrings based on semicolons that are not inside
         * brackets or parentheses.
         *
         * @param input the input string to split
         * @return a mutable list of substrings
         */
        internal fun splitOnOuterSemicolons(input: String): List<String> {
            var depth = 0
            var start = 0
            val characters = mutableListOf<String>()
            for (i in input.indices) {
                when (input[i]) {
                    '(', '[' -> depth++
                    ')', ']' -> depth--
                    ';' -> if (depth <= 0) {
                        val element = input.substring(start, i).trim()
                        if (element.isNotEmpty()) {
                            characters.add(element)
                        }
                        start = i + 1
                    }
                }
            }
            val element = input.substring(start).trim()
            if (element.isNotEmpty()) {
                characters.add(element)
            }
            return characters
        }

        /**
         * Parses a character string into name/team name, alter ego/membership, and
         * appearance notes. It expects a string in the format: "name/team name
         * [alter ego/membership] (appearance notes)"
         *
         * @param input the character string to parse.
         * @return a triple containing the name/team name, alter ego/membership,
         *     and appearance notes.
         */
        internal fun splitString(input: String): Triple<String, String, String> {
            // a regex to split "name [team name [or membership]] (appearance notes)" to "name", "team name [or membership]", and "appearance notes"

            val regex = Regex("^([^\\[(]*)(?:\\[(.*)(?=]))?(?:[^(]+)?(?:\\((.*)(?=\\)))?")

            val matchResult = regex.find(input)

            val textBeforeBrackets = matchResult?.groupValues?.get(1)?.trim() ?: ""
            val textInsideBrackets = matchResult?.groupValues?.get(2)?.trim() ?: ""
            val textInsideParentheses = matchResult?.groupValues?.get(3)?.trim() ?: ""
            return Triple(textBeforeBrackets, textInsideBrackets, textInsideParentheses)
        }

        /**
         * Parses bracketed text and returns a triple of strings containing the
         * alter ego and notes, or a list of team members.
         *
         * @param bracketedText The bracketed text to parse.
         * @return A triple of strings containing the alter ego, notes, and team
         *     members.
         */
        internal fun parseBracketedText(bracketedText: String?): Pair<String?, String?> {
            val splitText = bracketedText?.let { splitOnOuterSemicolons(it) }
            var alterEgo: String? = null
            var membership: String? = null

            // Check if the split text is not null
            if (splitText != null) {
                // If the split text has more than two elements, assume the bracketed text is a list of team members
                if (splitText.size > 1) {
                    membership = bracketedText
                } else {
                    // If the split text has at least one element, assume the first element is the alter ego
                    if (splitText.isNotEmpty()) {
                        alterEgo = splitText[0].cleanup()
                    }
                }
            }
            return Pair(alterEgo, membership)
        }
    }
}