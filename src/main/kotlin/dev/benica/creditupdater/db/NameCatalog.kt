package dev.benica.creditupdater.db

import mu.KotlinLogging
import java.sql.Connection
import java.sql.SQLException

/**
 * The names the gcd gives its characters and its groups, to tell what a name in a story's text is:
 * a character, whose brackets hold the names they go by, or a group, whose brackets hold its members.
 * A name is compared without case or punctuation, as the database scripts do.
 */
class NameCatalog(characterNames: Collection<String>, groupNames: Collection<String>) {
    enum class Kind { CHARACTER, GROUP, BOTH, UNKNOWN }

    private val characters = characterNames.mapTo(HashSet()) { key(it) }
    private val groups = groupNames.mapTo(HashSet()) { key(it) }

    fun kindOf(name: String): Kind {
        val key = key(name)
        return when {
            key in characters && key in groups -> Kind.BOTH
            key in characters -> Kind.CHARACTER
            key in groups -> Kind.GROUP
            else -> Kind.UNKNOWN
        }
    }

    companion object {
        /** Knows nothing, so every name is left to the text's own clues. */
        val EMPTY = NameCatalog(emptyList(), emptyList())

        internal fun key(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

        /** The catalog of [schema]; empty if it has no such tables (a database that isn't a gcd dump). */
        fun load(conn: Connection, schema: String): NameCatalog = try {
            NameCatalog(
                names(conn, schema, "gcd_character", "gcd_character_name_detail"),
                names(conn, schema, "gcd_group", "gcd_group_name_detail"),
            )
        } catch (e: SQLException) {
            KotlinLogging.logger(NameCatalog::class.java.simpleName)
                .warn { "No gcd character and group names in $schema, so none are told apart: ${e.message}" }
            EMPTY
        }

        private fun names(conn: Connection, schema: String, table: String, detailTable: String): List<String> =
            conn.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT name FROM $schema.$table WHERE deleted = 0 " +
                            "UNION SELECT name FROM $schema.$detailTable WHERE deleted = 0"
                ).use { rows ->
                    buildList { while (rows.next()) add(rows.getString(1)) }
                }
            }
    }
}
