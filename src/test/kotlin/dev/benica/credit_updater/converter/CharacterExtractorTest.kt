package dev.benica.credit_updater.converter

import dev.benica.credit_updater.converter.CharacterExtractor.*
import kotlinx.coroutines.runBlocking
import mu.KLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.sql.Connection
import java.sql.ResultSet

class CharacterExtractorTest {
    private val database: String = "gcdb"
    private val conn: Connection = mock(Connection::class.java)
    private val logger: KLogger = mock(KLogger::class.java)
    private val characterExtractor: CharacterExtractor = CharacterExtractor(database, conn)

    @Test
    @DisplayName("Should handle empty character list in the ResultSet")
    fun handleEmptyCharacterListInResultSet() {
        val resultSet: ResultSet = mock(ResultSet::class.java)
        val destDatabase: String? = null
        val expectedStoryId = 1234

        `when`(resultSet.next()).thenReturn(false)
        `when`(resultSet.getInt("id")).thenReturn(expectedStoryId)
        `when`(resultSet.getString("characters")).thenReturn("Aquaman [Arthur Curry]; Batman [Bruce Wayne] (cameo); Lois Lane")
        `when`(resultSet.getInt("publisherId")).thenReturn(5)

        runBlocking {
            val result = characterExtractor.extract(resultSet, destDatabase)

            assertEquals(expectedStoryId, result)
        }
    }

    @Test
    @DisplayName("Should return the storyId after extracting characters from the ResultSet")
    fun returnStoryIdAfterExtractingCharacters() {
        val resultSet: ResultSet = mock(ResultSet::class.java)
        val destDatabase: String? = null
        val expectedStoryId = 1234

        `when`(resultSet.next()).thenReturn(true, false)
        `when`(resultSet.getInt("id")).thenReturn(expectedStoryId)
        `when`(resultSet.getString("characters")).thenReturn("Aquaman [Arthur Curry]; Batman [Bruce Wayne] (cameo); Lois Lane")
        `when`(resultSet.getInt("publisherId")).thenReturn(5)

        runBlocking {
            val actualStoryId: Int = characterExtractor.extract(resultSet, destDatabase)

            assertEquals(expectedStoryId, actualStoryId)
        }
    }
//    @Test
//    @DisplayName("Should save the buffer when the buffer limit is reached")
//    fun saveBufferWhenBufferLimitIsReached() {
//        val resultSet: ResultSet = mock(ResultSet::class.java)
//        val destDatabase: String? = null
//        val bufferLimit = 10
//        val appearances = mutableSetOf<CharacterExtractor.Appearance>()
//        val characterExtractor = spy(CharacterExtractor(database, conn))
//        characterExtractor.bufferLimit = bufferLimit
//
//        // Mocking the extractAppearanceInfo method
//        doAnswer { invocation ->
//            val openParen = invocation.getArgument<Int?>(0)
//            val closeParen = invocation.getArgument<Int?>(1)
//            val character = invocation.getArgument<String>(2)
//            val appearance = CharacterExtractor.Appearance(
//                character.substring(0, openParen!!),
//                character.substring(openParen + 1, closeParen!!),
//                character.substring(closeParen + 1)
//            )
//            appearances.add(appearance)
//            Triple(appearance.name, appearance.alterEgo, appearance.appearanceInfo)
//        }.`when`(characterExtractor).extractAppearanceInfo(anyInt(), anyInt(), anyString())
//
//        // Mocking the insertCharacterAppearances method
//        doNothing().`when`(characterExtractor).insertCharacterAppearances(
//            anySet(), anyString(), any(Connection::class.java)
//        )
//
//        // Mocking the upsertCharacter method
//        doReturn(1).`when`(characterExtractor).upsertCharacter(
//            anyString(), anyString(), anyInt()
//        )
//
//        // Mocking the lookupCharacter method
//        doReturn(null).`when`(characterExtractor).lookupCharacter(
//            anyString(), anyString(), anyInt(), anyString(), any(Connection::class.java), anyBoolean()
//        )
//
//        // Mocking the getPublisherId method
//        doReturn(1).`when`(characterExtractor).getPublisherId(anyInt(), any(Connection::class.java))
//
//        // Extracting the resultSet
//        val extractedCount = characterExtractor.extract(resultSet, destDatabase)
//
//        // Verifying the extracted count
//        assertEquals(appearances.size, extractedCount)
//
//        // Verifying the buffer is empty
//        assertEquals(0, characterExtractor.newAppearanceInsertionBuffer.appearances.size)
//
//        // Verifying the insertCharacterAppearances method is called
//        verify(characterExtractor, times(1)).insertCharacterAppearances(
//            eq(appearances), eq(database), eq(conn)
//        )
//    }

    @Test
    @DisplayName("parenthesesIndexes should return the indexes of the parentheses in the given string")
    fun parenthesesIndexesShouldReturnTheIndexesOfTheParenthesesInTheGivenString() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman [Bruce Wayne] (cameo)"
        val expectedIndexes = Pair(21, 27)

        val actualIndexes = characterExtractor.parenthesesIndexes(character)

        assertEquals(expectedIndexes, actualIndexes)
    }

    @Test
    @DisplayName("parenthesesIndexes should handle the case when the given string does not contain parentheses")
    fun parenthesesIndexesShouldHandleTheCaseWhenTheGivenStringDoesNotContainParentheses() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman"
        val expectedIndexes = Pair(null, null)

        val actualIndexes = characterExtractor.parenthesesIndexes(character)

        assertEquals(expectedIndexes, actualIndexes)
    }

    @Test
    @DisplayName("parenthesesIndexes should handle the case when the given string contains only one parentheses")
    fun parenthesesIndexesShouldHandleTheCaseWhenTheGivenStringContainsOnlyOneParentheses() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman [Bruce Wayne] (cameo"
        val expectedIndexes = Pair(21, null)

        val actualIndexes = characterExtractor.parenthesesIndexes(character)

        assertEquals(expectedIndexes, actualIndexes)
    }

    @Test
    @DisplayName("parenthesesIndexes should handle the case when the given string contains multiple sets of parentheses")
    fun parenthesesIndexesShouldHandleTheCaseWhenTheGivenStringContainsMultipleSetsOfParentheses() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman [Bruce Wayne] (cameo) (cameo)"
        val expectedIndexes = Pair(21, 27)

        val actualIndexes = characterExtractor.parenthesesIndexes(character)

        assertEquals(expectedIndexes, actualIndexes)
    }

    @Test
    @DisplayName("bracketIndexes should return the indexes of the brackets in the given string")
    fun bracketIndexesShouldReturnTheIndexesOfTheBracketsInTheGivenString() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman [Bruce Wayne] (cameo)"
        val expectedIndexes = Pair(7, 19)

        val actualIndexes = characterExtractor.bracketIndexes(character)

        assertEquals(expectedIndexes, actualIndexes)
    }

    @Test
    @DisplayName("splitString should split the given string into a list of strings")
    fun splitStringShouldSplitTheGivenStringIntoAListOfStrings() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman [Bruce Wayne] (cameo)"
        val expectedList = listOf("Batman", "Bruce Wayne", "cameo")

        val actualList: Triple<String, String, String> = characterExtractor.splitString(character)

        assertEquals(expectedList[0], actualList.first)
        assertEquals(expectedList[1], actualList.second)
        assertEquals(expectedList[2], actualList.third)
    }

    @Test
    @DisplayName("splitString should handle the case when brackets and parentheses are nested")
    fun splitStringShouldHandleTheCaseWhenBracketsAndParenthesesAreNested() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Batman [Bruce [Thomas] (Wayne)] (cameo)"
        val expectedList = listOf("Batman", "Bruce [Thomas] (Wayne)", "cameo")

        val actualList: Triple<String, String, String> = characterExtractor.splitString(character)

        assertEquals(expectedList[0], actualList.first)
        assertEquals(expectedList[1], actualList.second)
        assertEquals(expectedList[2], actualList.third)
    }

    @Test
    @DisplayName("splitString should handle the case when the character is a team with a membership list")
    fun splitStringShouldHandleATeamWithMembershipList() {
        val characterExtractor = CharacterExtractor(database, conn)
        val character = "Justice League [Batman [Bruce Wayne]; Superman [Clark Kent], Martian Manhunter [J'onn J'onzz]]"
        val expectedList = listOf(
            "Justice League",
            "Batman [Bruce Wayne]; Superman [Clark Kent], Martian Manhunter [J'onn J'onzz]",
            ""
        )

        val actualList: Triple<String, String, String> = characterExtractor.splitString(character)

        assertEquals(expectedList[0], actualList.first)
        assertEquals(expectedList[1], actualList.second)
        assertEquals(expectedList[2], actualList.third)
    }

    @Test
    @DisplayName("parseCharacters should parse the given string into a list of characters")
    fun parseCharactersShouldParseTheGivenStringIntoAListOfCharacters() {
        val characterExtractor = CharacterExtractor(database, conn)
        val characters = "Batman [Bruce Wayne] (cameo); Superman [Clark Kent] (cameo)"
        val expectedList = listOf(
            Individual("Batman", "Bruce Wayne", "cameo"),
            Individual("Superman", "Clark Kent", "cameo")
        )

        val actualList: List<CharacterAppearance> = characterExtractor.parseCharacters(characters)

        assertEquals(expectedList[0], actualList[0])
        assertEquals(expectedList[1], actualList[1])
    }

    @Test
    @DisplayName("parseCharacters should handle the case when one of the characters is a Team")
    fun parseCharactersShouldHandleTheCaseWhenOneOfTheCharactersIsATeam() {
        val characterExtractor = CharacterExtractor(database, conn)
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

        val actualList: List<CharacterAppearance> = characterExtractor.parseCharacters(characters)

        assertEquals(expectedList[0], actualList[0])
        assertEquals(expectedList[1], actualList[1])
    }

    @Test
    @DisplayName("parseCharacters should handle the case when one of the characters is a Team")
    fun parseCharactersShouldHandleTheCaseWhenOneOfTheCharactersIsATeamMalformed() {
        val characterExtractor = CharacterExtractor(database, conn)
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

        val actualList: List<CharacterAppearance> = characterExtractor.parseCharacters(characters)

        assertEquals(expectedList[0], actualList[0])
        assertEquals(expectedList[1], actualList[1])
    }


    @Test
    @DisplayName("parseCharacters should handle the case where there are parentheses inside the square brackets")
    fun parseCharactersShouldHandleTheCaseWhereThereAreParenthesesInsideTheSquareBrackets() {
        val characterExtractor = CharacterExtractor(database, conn)
        val characters = "Buster Brown [Major Ray (Major William Ray)] (cameo)"
        val expectedList = listOf(
            Individual(name = "Buster Brown", alterEgo = "Major Ray (Major William Ray)", appearanceInfo = "cameo")
        )

        val actualList: List<CharacterAppearance> = characterExtractor.parseCharacters(characters)

        assertEquals(expectedList[0], actualList[0])
    }

    @Test
    @DisplayName("splitOnOuterSemicolons handles string with unmatched brackets")
    fun splitOnOuter() {
        val characterExtractor = CharacterExtractor(database, conn)
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

        val actualResult: List<String> = characterExtractor.splitOnOuterSemicolons(string)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    @DisplayName("splitOnOuterSemicolons handles semicolons inside brackets")
    fun splitOnOuterSemicolonsHandlesSemicolonsInsideBrackets() {
        val characterExtractor = CharacterExtractor(database, conn)
        val string =
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]; Despero (villain); Starro (villain);"
        val expectedResult: List<String> = listOf(
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]",
            "Despero (villain)",
            "Starro (villain)"
        )

        val actualResult: List<String> = characterExtractor.splitOnOuterSemicolons(string)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    @DisplayName("parseCharacters handles characters with semicolons inside brackets")
    fun `parseCharacters$CreditUpdater`() {
        val characterExtractor = CharacterExtractor(database, conn)
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

        val actualList: List<CharacterAppearance> = characterExtractor.parseCharacters(characters)
        println("${actualList.get(0).let { it::class.simpleName }} | $expectedList")
        assertEquals(expectedList[0], actualList[0])
        assertEquals(expectedList[1], actualList[1])
        assertEquals(expectedList[2], actualList[2])
    }

    @Test
    @DisplayName("fixMissingBrackets should handle malformed characterString")
    fun fixMissingBracketsShouldHandleMalformedCharacterString() {
        val characterExtractor = CharacterExtractor(database, conn)
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

        val actualString = characterExtractor.fixMissingBrackets(characterString)

        assertEquals(expectedString, actualString)
    }

    @Test
    fun splitString() {
    }

    @Test
    @DisplayName("parseBracketedText handles the a team")
    fun `parseBracketedText$CreditUpdater`() {
        val characterExtractor = CharacterExtractor(database, conn)
        val string = "Batman; Superman; Martian Manhunter [J'onn J'onnz]"
        val expectedResult: Pair<String?, String?> = Pair(null, "Batman; Superman; Martian Manhunter [J'onn J'onnz]")

        val actualResult: Pair<String?, String?> = characterExtractor.parseBracketedText(string)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `bracketIndexes$CreditUpdater`() {
    }

    @Test
    fun `parenthesesIndexes$CreditUpdater`() {
    }

    @Test
    @DisplayName("splitOnOuterSemicolons handles string with a team")
    fun `splitOnOuterSemicolons$CreditUpdater`() {
        val characterExtractor = CharacterExtractor(database, conn)
        val string =
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]; Despero (villain); Starro (villain);"
        val expectedResult: List<String> = listOf(
            "Justice League [Batman; Superman; Martian Manhunter [J'onn J'onnz]]",
            "Despero (villain)",
            "Starro (villain)"
        )

        val actualResult: List<String> = characterExtractor.splitOnOuterSemicolons(string)

        assertEquals(expectedResult, actualResult)
    }
}