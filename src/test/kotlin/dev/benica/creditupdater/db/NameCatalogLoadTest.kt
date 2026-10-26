package dev.benica.creditupdater.db

import dev.benica.creditupdater.db.TestDatabases.TEST_DATABASE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("db")
class NameCatalogLoadTest {
    @Test
    @DisplayName("should know the names of the gcd's characters and groups, deleted records aside")
    fun readsCharactersAndGroups() {
        ConnectionProvider.getConnection(TEST_DATABASE).connection.use { conn ->
            conn.createStatement().use {
                it.execute("DROP TABLE IF EXISTS $TEST_DATABASE.gcd_character")
                it.execute("DROP TABLE IF EXISTS $TEST_DATABASE.gcd_character_name_detail")
                it.execute("DROP TABLE IF EXISTS $TEST_DATABASE.gcd_group")
                it.execute("DROP TABLE IF EXISTS $TEST_DATABASE.gcd_group_name_detail")
                it.execute("CREATE TABLE $TEST_DATABASE.gcd_character (id INT PRIMARY KEY, name VARCHAR(255), deleted TINYINT)")
                it.execute("CREATE TABLE $TEST_DATABASE.gcd_character_name_detail (id INT PRIMARY KEY, name VARCHAR(255), deleted TINYINT)")
                it.execute("CREATE TABLE $TEST_DATABASE.gcd_group (id INT PRIMARY KEY, name VARCHAR(255), deleted TINYINT)")
                it.execute("CREATE TABLE $TEST_DATABASE.gcd_group_name_detail (id INT PRIMARY KEY, name VARCHAR(255), deleted TINYINT)")
                it.execute("INSERT INTO $TEST_DATABASE.gcd_character VALUES (1, 'Superman', 0), (2, 'Gone Hero', 1)")
                it.execute("INSERT INTO $TEST_DATABASE.gcd_character_name_detail VALUES (1, 'Man of Steel', 0)")
                it.execute("INSERT INTO $TEST_DATABASE.gcd_group VALUES (1, 'Justice League', 0), (2, 'Gone Team', 1)")
                it.execute("INSERT INTO $TEST_DATABASE.gcd_group_name_detail VALUES (1, 'JLA', 0)")
            }

            val catalog = NameCatalog.load(conn, TEST_DATABASE)

            assertEquals(NameCatalog.Kind.CHARACTER, catalog.kindOf("Man of Steel"))
            assertEquals(NameCatalog.Kind.GROUP, catalog.kindOf("jla"))
            assertEquals(NameCatalog.Kind.GROUP, catalog.kindOf("Justice League"))
            assertEquals(NameCatalog.Kind.UNKNOWN, catalog.kindOf("Gone Hero"))
            assertEquals(NameCatalog.Kind.UNKNOWN, catalog.kindOf("Gone Team"))
        }
    }

    @Test
    @DisplayName("should be empty, not fail, for a database with no such tables")
    fun emptyWithoutTables() {
        ConnectionProvider.getConnection(TEST_DATABASE).connection.use { conn ->
            conn.createStatement().use {
                it.execute("DROP TABLE IF EXISTS $TEST_DATABASE.gcd_character")
            }
            assertEquals(NameCatalog.Kind.UNKNOWN, NameCatalog.load(conn, TEST_DATABASE).kindOf("Superman"))
        }
    }
}
