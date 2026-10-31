package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.db.parseSqlScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The linked characters and merge scripts run through [parseSqlScript], which joins every line with a
 * space and splits on semicolons: a comment would swallow the statement after it. Their
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
    @DisplayName("should parse the merge script into plain statements that drop their helper tables at the end")
    fun mergeScriptParsesIntoPlainStatements() {
        val merge = File(DBInitializer.INIT_MERGE_DUPLICATE_CHARACTERS)
            .parseSqlScript(targetSchema = "test_schema")
        assertTrue(merge.size > 10)
        merge.forEach {
            assertFalse(it.contains("{{"), "placeholder left in: $it")
            assertFalse(it.contains("--"), "comment left in: $it")
        }
        assertTrue(merge.first().startsWith("ALTER TABLE `test_schema`.m_character"))
        assertTrue(merge[1].startsWith("DROP TABLE IF EXISTS `test_schema`.merge_key_work"))
        assertTrue(merge.last().startsWith("DROP TABLE `test_schema`.merge_duplicate_work"))
    }

    @Test
    @DisplayName("should parse the prune script into one statement that only deletes characters no appearance uses")
    fun pruneScriptDeletesOnlyEmptyCharacters() {
        val prune = File(DBInitializer.INIT_PRUNE_EMPTY_CHARACTERS).parseSqlScript(targetSchema = "test_schema")
        assertEquals(1, prune.size)
        assertTrue(prune.single().startsWith("DELETE m FROM `test_schema`.m_character m WHERE NOT EXISTS"))
        assertTrue(prune.single().contains("a.character_id = m.id"))
    }

    @Test
    @DisplayName("should not drop the tables of characters and groups the gcd links to stories")
    fun keepsTheLinkTables() {
        val dropped = File(DBInitializer.INIT_DROP_UNUSED_TABLES).readText()
        assertFalse(dropped.contains("`gcd_story_character`;"))
        assertFalse(dropped.contains("`gcd_story_character_group`;"))
        assertFalse(dropped.contains("`gcd_group_character`;"))
    }

    @Test
    @DisplayName("should parse the linked groups script into plain statements that end by dropping its helper tables")
    fun groupsScriptParsesIntoPlainStatements() {
        val groups = File(DBInitializer.INIT_ADD_LINKED_GROUPS).parseSqlScript(targetSchema = "test_schema")
        assertTrue(groups.size > 20)
        groups.forEach {
            assertFalse(it.contains("{{"), "placeholder left in: $it")
            assertFalse(it.contains("--"), "comment left in: $it")
        }
        assertTrue(groups.first().startsWith("ALTER TABLE `test_schema`.m_character"))
        assertTrue(groups.last().startsWith("DROP TABLE `test_schema`.team_duplicate_work"))
    }

    @Test
    @DisplayName("should delete group links of removed stories before the stories themselves")
    fun removesGroupLinksBeforeStories() {
        val script = File("src/main/resources/sql/init_remove_items.sql").readText()
        val stories = script.indexOf("FROM gcd_story\n")
        assertTrue(script.indexOf("FROM gcd_group_character gc") in 0 until stories)
        assertTrue(script.indexOf("FROM gcd_story_character_group sg") in 0 until script.indexOf("FROM gcd_story_character gsc"))
    }

    @Test
    @DisplayName("should keep the United Kingdom's publishers as well as the United States'")
    fun keepsBritishPublishers() {
        val script = File("src/main/resources/sql/init_remove_items.sql").readText()
        assertFalse(script.contains("country_id != 225"))
        assertTrue(script.contains("country_id NOT IN (225, 75)"))
    }

    @Test
    @DisplayName("should delete links of removed stories before the stories themselves")
    fun removesLinksBeforeStories() {
        val script = File("src/main/resources/sql/init_remove_items.sql").readText()
        assertTrue(script.indexOf("FROM gcd_story_character gsc") in 0 until script.indexOf("FROM gcd_story\n"))
    }

    @Test
    @DisplayName("should parse the migrate script into plain statements that copy only the migrated stories' links")
    fun migrateScriptParsesIntoPlainStatements() {
        val migrate = File(DBMigrator.MIGRATE_LINKED)
            .parseSqlScript(targetSchema = "target_schema", sourceSchema = "source_schema")
        assertTrue(migrate.first().startsWith("SET FOREIGN_KEY_CHECKS = 0"))
        assertTrue(migrate.last().startsWith("SET FOREIGN_KEY_CHECKS = 1"))
        migrate.forEach {
            assertFalse(it.contains("{{"), "placeholder left in: $it")
            assertFalse(it.contains("--") || it.contains("#"), "comment left in: $it")
        }
        val links = migrate.first { it.startsWith("REPLACE target_schema.gcd_story_character ") }
        assertTrue(links.contains("JOIN target_schema.gcd_story gs ON gs.id = new.story_id"))
    }
}
