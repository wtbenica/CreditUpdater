package dev.benica.creditupdater.db

import java.sql.Connection

/**
 * The creators' names and the credits already in the database, kept in memory while credits are
 * extracted, so a name in a story doesn't cost queries to look up and to check.
 *
 * A name is found as the database would find it: without regard to case or accents, and ignoring
 * trailing spaces; of several name details with the same name the lowest id is used.
 *
 * @param targetSchema the schema with the `gcd_creator_name_detail`, `gcd_story_credit` and
 *     `m_story_credit` tables
 */
class CreditLookup(private val targetSchema: String) {
    /** A credit as the database tells them apart. */
    data class Credit(val creatorId: Int, val creditTypeId: Int, val storyId: Int)

    private val creators = HashMap<String, Int>()

    /** Reads the creators' names. Call once, before [creatorId]. */
    fun load(conn: Connection) {
        conn.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT id, name FROM $targetSchema.gcd_creator_name_detail ORDER BY id"
            ).use { rows ->
                while (rows.next()) {
                    creators.putIfAbsent(CharacterIdCache.normalize(rows.getString("name")), rows.getInt("id"))
                }
            }
        }
    }

    /** The id of the name detail with [name], or null. */
    fun creatorId(name: String): Int? = creators[CharacterIdCache.normalize(name)]

    /** The credits of the stories from [firstStory] to [lastStory], GCD's own and the extracted. */
    fun creditsBetween(firstStory: Int, lastStory: Int, conn: Connection): Set<Credit> {
        val found = HashSet<Credit>()
        listOf("gcd_story_credit", "m_story_credit").forEach { table ->
            conn.prepareStatement(
                "SELECT creator_id, credit_type_id, story_id FROM $targetSchema.$table WHERE story_id BETWEEN ? AND ?"
            ).use { statement ->
                statement.setInt(1, firstStory)
                statement.setInt(2, lastStory)
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        found += Credit(rows.getInt(1), rows.getInt(2), rows.getInt(3))
                    }
                }
            }
        }
        return found
    }
}
