package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("db")
class DBTaskCopyCharactersTest {
    private val base = "copy_chars_base"
    private val target = "copy_chars_target"
    private val conn = getTestDbConnection()

    private fun run(sql: String) = conn.createStatement().use { it.execute(sql) }

    private fun characterTable(schema: String, links: Boolean) = run(
        """CREATE TABLE $schema.m_character (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL,
           alter_ego VARCHAR(255), publisher_id INT${if (links) ", gcd_character_id INT NULL, gcd_group_id INT NULL" else ""})"""
    )

    @BeforeEach
    fun setUp() {
        listOf(base, target).forEach { run("DROP DATABASE IF EXISTS $it"); run("CREATE DATABASE $it") }
        run("CREATE TABLE $target.gcd_publisher (id INT PRIMARY KEY)")
        run("INSERT INTO $target.gcd_publisher VALUES (78)")
    }

    @AfterEach
    fun tearDown() = listOf(base, target).forEach { run("DROP DATABASE IF EXISTS $it") }

    private fun linksOf(id: Int): String = conn.createStatement().use { statement ->
        statement.executeQuery("SELECT gcd_character_id, gcd_group_id FROM $target.m_character WHERE id = $id").use {
            it.next()
            "${it.getObject(1)}/${it.getObject(2)}"
        }
    }

    @Test
    @DisplayName("should keep the ids and the links to the gcd's characters and groups")
    fun keepsIdsAndLinks() {
        characterTable(base, links = true)
        characterTable(target, links = false)
        run("INSERT INTO $base.m_character VALUES (877894, 'Constructicons', NULL, 78, NULL, 424), (5, 'Batman', 'Bruce Wayne', 78, 3, NULL), (9, 'Elsewhere', NULL, 99, NULL, 7)")

        DBTask(target).copyCharactersFrom(base)

        assertEquals("null/424", linksOf(877894))
        assertEquals("3/null", linksOf(5))
        conn.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $target.m_character").use {
                it.next()
                assertEquals(2, it.getInt(1))
            }
        }
    }

    @Test
    @DisplayName("should copy a base that has no links yet")
    fun copiesABaseWithoutLinks() {
        characterTable(base, links = false)
        characterTable(target, links = false)
        run("INSERT INTO $base.m_character VALUES (5, 'Batman', 'Bruce Wayne', 78)")

        DBTask(target).copyCharactersFrom(base)

        conn.createStatement().use { statement ->
            statement.executeQuery("SELECT id FROM $target.m_character").use {
                it.next()
                assertEquals(5, it.getInt(1))
            }
        }
    }
}
