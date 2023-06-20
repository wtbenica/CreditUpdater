package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.extractor.utils.CharacterParser
import dev.benica.creditupdater.models.CharacterAppearance
import dev.benica.creditupdater.models.Individual
import dev.benica.creditupdater.models.Team
import mu.KLogger
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CharacterParserTest {
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    @Test
    @DisplayName("splitString should split the given string into a list of strings")
    fun splitStringShouldSplitTheGivenStringIntoAListOfStrings() {
        val character = "Batman [Bruce Wayne] (cameo)"
        val expectedList = listOf("Batman", "Bruce Wayne", "cameo")

        val actualList: Triple<String, String, String> = CharacterParser.splitString(character)

        Assertions.assertEquals(expectedList[0], actualList.first)
        Assertions.assertEquals(expectedList[1], actualList.second)
        Assertions.assertEquals(expectedList[2], actualList.third)
    }

    @Test
    @DisplayName("splitString should handle the case when brackets and parentheses are nested")
    fun splitStringShouldHandleTheCaseWhenBracketsAndParenthesesAreNested() {
        val character = "Batman [Bruce [Thomas] (Wayne)] (cameo)"
        val expectedList = listOf("Batman", "Bruce [Thomas] (Wayne)", "cameo")

        val actualList: Triple<String, String, String> = CharacterParser.splitString(character)

        Assertions.assertEquals(expectedList[0], actualList.first)
        Assertions.assertEquals(expectedList[1], actualList.second)
        Assertions.assertEquals(expectedList[2], actualList.third)
    }

    @Test
    @DisplayName("splitString should handle the case when the character is a team with a membership list")
    fun splitStringShouldHandleATeamWithMembershipList() {
        val character = "Justice League [Batman [Bruce Wayne]; Superman [Clark Kent], Martian Manhunter [J'onn J'onzz]]"
        val expectedList = listOf(
            "Justice League",
            "Batman [Bruce Wayne]; Superman [Clark Kent], Martian Manhunter [J'onn J'onzz]",
            ""
        )

        val actualList: Triple<String, String, String> = CharacterParser.splitString(character)

        Assertions.assertEquals(expectedList[0], actualList.first)
        Assertions.assertEquals(expectedList[1], actualList.second)
        Assertions.assertEquals(expectedList[2], actualList.third)
    }

    @Test
    @DisplayName("parseCharacters should parse the given string into a list of characters")
    fun parseCharactersShouldParseTheGivenStringIntoAListOfCharacters() {
        val characters = "Batman [Bruce Wayne] (cameo); Superman [Clark Kent] (cameo)"
        val expectedList = listOf(
            Individual("Batman", "Bruce Wayne", "cameo"),
            Individual("Superman", "Clark Kent", "cameo")
        )

        val actualList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)

        Assertions.assertEquals(expectedList[0], actualList[0])
        Assertions.assertEquals(expectedList[1], actualList[1])
    }

    @Test
    @DisplayName("parseCharacters should handle the case when one of the characters is a Team")
    fun parseCharactersShouldHandleTheCaseWhenOneOfTheCharactersIsATeam() {
        val characters =
            "Batman [Bruce Wayne] (cameo); Justice League of America [Superman [Clark Kent]; Batman [Bruce Wayne]; " +
                    "Princess Diana [Wonder Woman];] (cameo)"
        val expectedList = listOf(
            Individual(name = "Batman", alterEgo = "Bruce Wayne", appearanceInfo = "cameo"),
            Team(
                name = "Justice League of America",
                members = "Superman [Clark Kent]; Batman [Bruce Wayne]; Princess Diana [Wonder Woman];",
                appearanceInfo = "cameo"
            )
        )

        val actualList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)

        Assertions.assertEquals(expectedList[0], actualList[0])
        Assertions.assertEquals(expectedList[1], actualList[1])
    }

    @Test
    @DisplayName("parseCharacters should handle the case when one of the characters is a Team")
    fun parseCharactersShouldHandleTheCaseWhenOneOfTheCharactersIsATeamMalformed() {
        val characters =
            "Batman [Bruce Wayne] (cameo); Justice Society of America [Superman [Clark Kent; Kal-L (cameo)]; Wonder " +
                    "Woman [Diana Prince Trevor]; Red Tornado [John Smith]; Wildcat [Ted Grant]; Hourman [Rex " +
                    "Tyler]; Starman [Ted Knight]; Dr. Mid-Nite [Charles McNider]; Dr. Fate [Kent Nelson; Nabu]; " +
                    "Sandman [Wesley Dodds]; Johnny Thunder; Thunderbolt] (Earth-2)"
        val expectedList = listOf(
            Individual(name = "Batman", alterEgo = "Bruce Wayne", appearanceInfo = "cameo"),
            Team(
                name = "Justice Society of America",
                members = "Superman [Clark Kent; Kal-L (cameo)]; Wonder Woman [Diana Prince Trevor]; Red Tornado [" +
                        "John Smith]; Wildcat [Ted Grant]; Hourman [Rex Tyler]; Starman [Ted Knight]; Dr. Mid-Nite [" +
                        "Charles McNider]; Dr. Fate [Kent Nelson; Nabu]; Sandman [Wesley Dodds]; Johnny Thunder; " +
                        "Thunderbolt",
                appearanceInfo = "Earth-2"
            )
        )

        val actualList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)

        Assertions.assertEquals(expectedList[0], actualList[0])
        Assertions.assertEquals(expectedList[1], actualList[1])
    }


    @Test
    @DisplayName("parseCharacters should handle the case where there are parentheses inside the square brackets")
    fun parseCharactersShouldHandleTheCaseWhereThereAreParenthesesInsideTheSquareBrackets() {
        val characters = "Buster Brown [Major Ray (Major William Ray)] (cameo)"
        val expectedList = listOf(
            Individual(name = "Buster Brown", alterEgo = "Major Ray (Major William Ray)", appearanceInfo = "cameo")
        )

        val actualList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)

        Assertions.assertEquals(expectedList[0], actualList[0])
    }

    @Test
    @DisplayName("splitOnOuterSemicolons handles string with unmatched brackets")
    fun splitOnOuter() {
        val string =
            "Captain America [Steve Rogers]]; Wasp [Janet Van Dyne]; Cyclops; Spider-Woman [Julia Carpenter]; Wolverine [Logan]; Rogue; Absorbing Man; Titania; Volcana; Dr. Octopus; She-Hulk [Jennifer Walters]"
        val expectedResult: List<String> = listOf(
            "Captain America [Steve Rogers]]",
            "Wasp [Janet Van Dyne]",
            "Cyclops",
            "Spider-Woman [Julia Carpenter]",
            "Wolverine [Logan]",
            "Rogue",
            "Absorbing Man",
            "Titania",
            "Volcana",
            "Dr. Octopus",
            "She-Hulk [Jennifer Walters]"
        )

        val actualResult: List<String> = CharacterParser.splitOnOuterSemicolons(string)

        Assertions.assertEquals(expectedResult, actualResult)
    }

    @Test
    @DisplayName("splitOnOuterSemicolons handles semicolons inside brackets")
    fun splitOnOuterSemicolonsHandlesSemicolonsInsideBrackets() {
        val string =
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]; Despero (villain); Starro (villain);"
        val expectedResult: List<String> = listOf(
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]",
            "Despero (villain)",
            "Starro (villain)"
        )

        val actualResult: List<String> = CharacterParser.splitOnOuterSemicolons(string)

        Assertions.assertEquals(expectedResult, actualResult)
    }

    @Test
    @DisplayName("parseCharacters handles characters with semicolons inside brackets")
    fun `parseCharacters$CreditUpdater`() {
        val characters =
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]; Despero (villain); Starro (villain);"
        val expectedList = listOf(
            Team(
                name = "Justice League",
                members = "Batman; Superman; Martian Manhunter [J'onn J'onnz]",
                appearanceInfo = ""
            ),
            Individual(name = "Despero", alterEgo = null, appearanceInfo = "villain"),
            Individual(name = "Starro", alterEgo = null, appearanceInfo = "villain")
        )

        val actualList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)
        logger.debug { "${actualList[0].let { it::class.simpleName }} | $expectedList" }
        Assertions.assertEquals(expectedList[0], actualList[0])
        Assertions.assertEquals(expectedList[1], actualList[1])
        Assertions.assertEquals(expectedList[2], actualList[2])
    }

    @Test
    @DisplayName("fixMissingBrackets should handle malformed characterString")
    fun fixMissingBracketsShouldHandleMalformedCharacterString() {
        val characterString =
            """Batman [Bruce Wayne] (cameo); Justice Society of America [Superman [Clark Kent; Kal-L (cameo); Wonder 
                |Woman [Diana Prince Trevor]; Red Tornado [John Smith]; Wildcat [Ted Grant]; Hourman [Rex Tyler]; 
                |Starman [Ted Knight]; Dr. Mid-Nite [Charles McNider]; Dr. Fate [Kent Nelson; Nabu]; Sandman [Wesley 
                |Dodds]; Johnny Thunder; Thunderbolt] (Earth-2)""".trimMargin()

        val expectedString =
            """Batman [Bruce Wayne] (cameo); Justice Society of America [Superman [Clark Kent; Kal-L (cameo)]; Wonder 
                |Woman [Diana Prince Trevor]; Red Tornado [John Smith]; Wildcat [Ted Grant]; Hourman [Rex Tyler]; 
                |Starman [Ted Knight]; Dr. Mid-Nite [Charles McNider]; Dr. Fate [Kent Nelson; Nabu]; Sandman [Wesley 
                |Dodds]; Johnny Thunder; Thunderbolt] (Earth-2)""".trimMargin()

        val actualString = CharacterParser.fixMissingBrackets(characterString)

        Assertions.assertEquals(expectedString, actualString)
    }

    @Test
    fun splitString() {
    }

    @Test
    @DisplayName("parseBracketedText handles the a team")
    fun `parseBracketedText$CreditUpdater`() {
        val string = "Batman; Superman; Martian Manhunter [J'onn J'onnz]"
        val expectedResult: Pair<String?, String?> = Pair(null, "Batman; Superman; Martian Manhunter [J'onn J'onnz]")

        val actualResult: Pair<String?, String?> = CharacterParser.parseBracketedText(string)

        Assertions.assertEquals(expectedResult, actualResult)
    }

    @Test
    @DisplayName("splitOnOuterSemicolons handles string with a team")
    fun `splitOnOuterSemicolons$CreditUpdater`() {
        val string =
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]; Despero (villain); Starro (villain);"
        val expectedResult: List<String> = listOf(
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]",
            "Despero (villain)",
            "Starro (villain)"
        )

        val actualResult: List<String> = CharacterParser.splitOnOuterSemicolons(string)

        Assertions.assertEquals(expectedResult, actualResult)
    }
}