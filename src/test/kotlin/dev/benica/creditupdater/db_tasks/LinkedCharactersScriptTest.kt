package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.db.parseSqlScript
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The linked characters script runs through [parseSqlScript], which joins every line with a
 * space and splits on semicolons: a comment would swallow the statement after it. The script's
 * behavior is tested against a database in the backend repository (tools/tests).
 */
class LinkedCharactersScriptTest {
    private val statements =
        File(DBInitializer.INIT_ADD_LINKED_CHARACTERS).parseSqlScript(targetSchema = "test_schema")

    @Test
    @DisplayName("should parse into statements with the schema filled in and no comments")
    fun parsesIntoPlainStatements() {
        assertTrue(statements.size > 10)
        statements.forEach {
            assertFalse(it.contains("{{"), "placeholder left in: $it")
            assertFalse(it.contains("--"), "comment left in: $it")
            assertFalse(it.contains("#"), "comment left in: $it")
        }
    }

    @Test
    @DisplayName("should add its columns first and remove its helper tables last")
    fun startsAndEndsAsExpected() {
        assertTrue(statements.first().startsWith("ALTER TABLE `test_schema`.m_character"))
        assertTrue(statements.last().startsWith("DROP TABLE `test_schema`.extracted_key_work"))
    }

    @Test
    @DisplayName("should not drop the table of characters the gcd links to stories")
    fun keepsTheLinkTable() {
        val dropped = File(DBInitializer.INIT_DROP_UNUSED_TABLES).readText()
        assertFalse(dropped.contains("`gcd_story_character`;"))
    }

    @Test
    @DisplayName("should delete links of removed stories before the stories themselves")
    fun removesLinksBeforeStories() {
        val script = File("src/main/resources/sql/init_remove_items.sql").readText()
        assertTrue(script.indexOf("FROM gcd_story_character gsc") in 0 until script.indexOf("FROM gcd_story\n"))
    }
}
