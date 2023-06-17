package dev.benica.converter

import dev.benica.converter.*
import dev.benica.converter.CharacterExtractor.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.sql.Connection
import java.sql.ResultSet

class CharacterExtractorTest {
    private val database: String = "gcdb"
    private val conn: Connection = mock(Connection::class.java)
    private val characterExtractor: CharacterExtractor = CharacterExtractor(database, conn)

    @Test
    @DisplayName("Should handle empty character list in the ResultSet")
    fun handleEmptyCharacterListInResultSet() {
        val resultSet: ResultSet = mock(ResultSet::class.java)
        val expectedStoryId = 1234

        `when`(resultSet.next()).thenReturn(false)
        `when`(resultSet.getInt("id")).thenReturn(expectedStoryId)
        `when`(resultSet.getString("characters")).thenReturn("Aquaman [Arthur Curry]; Batman [Bruce Wayne] (cameo); Lois Lane")
        `when`(resultSet.getInt("publisherId")).thenReturn(5)

        runBlocking {
            val result = characterExtractor.extractAndInsert(resultSet)

            assertEquals(expectedStoryId, result)
        }
    }

    @Test
    @DisplayName("Should return the storyId after extracting characters from the ResultSet")
    fun returnStoryIdAfterExtractingCharacters() {
        val resultSet: ResultSet = mock(ResultSet::class.java)
        val expectedStoryId = 1234

        `when`(resultSet.next()).thenReturn(true, false)
        `when`(resultSet.getInt("id")).thenReturn(expectedStoryId)
        `when`(resultSet.getString("characters")).thenReturn("Aquaman [Arthur Curry]; Batman [Bruce Wayne] (cameo); Lois Lane")
        `when`(resultSet.getInt("publisherId")).thenReturn(5)

        runBlocking {
            val actualStoryId: Int = characterExtractor.extractAndInsert(resultSet)

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
}