package dev.benica.creditupdater.converter

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class CreditExtractorTest {
//    private val conn = mock(Connection::class.java)
//    private val preparedStatement = mock(PreparedStatement::class.java)
//
//
//    @Test
//    @DisplayName("Should handle SQLException when getting gcnd")
//    fun makeCreditWithSQLExceptionOnGetGcnd() {
//        val creditExtractor = CreditExtractor("test_db", conn)
//        val extractedName = "John Doe"
//        val storyId = 1
//        val roleId = 2
//
//        `when`(conn.prepareStatement(anyString())).thenThrow(SQLException::class.java)
//
//        assertThrows(SQLException::class.java) {
//            creditExtractor.makeCredit(extractedName, storyId, roleId)
//        }
//
//        verify(conn, times(1)).prepareStatement(anyString())
//    }
//
//    @Test
//    @DisplayName("Should handle general Exception when getting gcnd")
//    fun makeCreditWithGeneralExceptionOnGetGcnd() {
//        val creditExtractor = CreditExtractor("test_db", conn)
//        val extractedName = "John Doe"
//        val storyId = 1
//        val roleId = 2
//
//        `when`(conn.prepareStatement(anyString())).thenThrow(Exception::class.java)
//
//        assertThrows(Exception::class.java) {
//            creditExtractor.makeCredit(extractedName, storyId, roleId)
//        }
//
//        verify(conn, times(1)).prepareStatement(anyString())
//    }
//
//    @Test
//    @DisplayName("Should not create credits when empty names are provided")
//    fun makeCreditWithEmptyNames() {
//        val creditExtractor = CreditExtractor("test_db", conn)
//        val extractedName = ""
//        val storyId = 1
//        val roleId = 2
//
//        creditExtractor.makeCredit(extractedName, storyId, roleId)
//
//        verify(conn, never()).prepareStatement(anyString())
//        verify(preparedStatement, never()).setInt(anyInt(), anyInt())
//        verify(preparedStatement, never()).executeUpdate()
//    }
//
//    @Test
//    @DisplayName("Should handle SQLException when getting story credit")
//    fun makeCreditWithSQLExceptionOnGetStoryCredit() {
//        val creditExtractor = CreditExtractor("test_db", conn)
//        val extractedName = "John Doe"
//        val storyId = 1
//        val roleId = 2
//
//        `when`(conn.prepareStatement(anyString())).thenThrow(SQLException::class.java)
//
//        assertThrows(SQLException::class.java) {
//            creditExtractor.makeCredit(extractedName, storyId, roleId)
//        }
//
//        verify(conn, times(1)).prepareStatement(anyString())
//    }
//
//    @Test
//    @DisplayName("Should create credits for all roles when valid names are provided")
//    fun makeCreditWithValidNames() {
//        val creditExtractor = CreditExtractor("test_db", conn)
//        val extractedName = "John Doe"
//        val storyId = 1
//        val roleId = 2
//        val gcndId = 123
//
//        // Mocking the getGcnd method
//        `when`(creditExtractor.getGcnd(extractedName, conn)).thenReturn(gcndId)
//
//        // Mocking the getStoryCredit method
//        `when`(creditExtractor.getStoryCredit(gcndId, storyId, roleId)).thenReturn(null)
//
//        // Mocking the makeStoryCredit method
//        doNothing().`when`(preparedStatement).setInt(anyInt(), anyInt())
//        `when`(conn.prepareStatement(anyString())).thenReturn(preparedStatement)
//        `when`(preparedStatement.executeUpdate()).thenReturn(1)
//
//        creditExtractor.makeCredit(extractedName, storyId, roleId)
//
//        verify(creditExtractor, times(1)).getGcnd(extractedName, conn)
//        verify(creditExtractor, times(1)).getStoryCredit(gcndId, storyId, roleId)
//        verify(preparedStatement, times(1)).setInt(1, gcndId)
//        verify(preparedStatement, times(1)).setInt(2, roleId)
//        verify(preparedStatement, times(1)).setInt(3, storyId)
//        verify(preparedStatement, times(1)).executeUpdate()
//    }
//
}