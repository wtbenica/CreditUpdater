package dev.benica.creditupdater.db

import java.sql.Connection
import java.text.Normalizer
import java.util.Locale

/**
 * The ids of the characters in `m_character`, kept in memory while characters are extracted, so a
 * story's characters don't each cost a query to look up and another to insert.
 *
 * A character is its name, alter ego and publisher. Two spellings the database would call equal
 * (it compares text without regard to case or accents, and ignores trailing spaces) are one
 * character here too, and the lowest id of those already there is the one used. A character not
 * yet there is given the next id and queued; [flush] inserts the queue.
 *
 * Ids are handed out in the order characters are first met, so extracting the same stories in the
 * same order always gives the same ids.
 *
 * @param targetSchema the schema whose `m_character` is extended
 * @param primarySchema when not null, characters already in this schema's `m_character` keep the
 *     ids they have there (the migration, which merges the new characters into that schema
 *     afterwards); they are not added to [targetSchema]
 */
class CharacterIdCache(
    private val targetSchema: String,
    private val primarySchema: String? = null,
) {
    /** A character as the database tells them apart. */
    internal data class Key(val name: String, val alterEgo: String?, val publisherId: Int)

    private class New(val id: Int, val name: String, val alterEgo: String?, val publisherId: Int)

    private val local = HashMap<Key, Int>()
    private val primary = HashMap<Key, Int>()
    private val queued = ArrayList<New>()
    private var nextId = 1

    /** Ids given to characters the table turned out to have already, and the ids it has them under. */
    private val replaced = HashMap<Int, Int>()

    /** Reads the characters already in the schemas. Call once, before [idFor]. */
    fun load(conn: Connection) {
        nextId = read(targetSchema, local, conn) + 1
        primarySchema?.let { read(it, primary, conn) }
    }

    /** Puts the characters of [schema] in [into], lowest id first; returns the highest id. */
    private fun read(schema: String, into: HashMap<Key, Int>, conn: Connection): Int {
        var highest = 0
        conn.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT id, name, alter_ego, publisher_id FROM $schema.m_character ORDER BY id"
            ).use { rows ->
                while (rows.next()) {
                    val id = rows.getInt("id")
                    into.putIfAbsent(keyOf(rows.getString("name"), rows.getString("alter_ego"), rows.getInt("publisher_id")), id)
                    highest = id
                }
            }
        }
        return highest
    }

    /** The id of the character, which is queued for insertion if there is none yet. */
    fun idFor(name: String, alterEgo: String?, publisherId: Int): Int {
        val key = keyOf(name, alterEgo, publisherId)
        primary[key]?.let { return it }
        local[key]?.let { return it }

        val id = nextId++
        local[key] = id
        queued += New(id, name.truncate(), alterEgo?.truncate(), publisherId)
        return id
    }

    /** How many characters wait to be inserted. */
    val waiting: Int
        get() = queued.size

    /**
     * The id to use for one [idFor] gave: the same, unless the table already had that character
     * under another id. (The table's idea of "the same" can be wider than ours; it is the table
     * that has the unique key.) Valid after [flush].
     */
    fun resolve(id: Int): Int = replaced[id] ?: id

    /**
     * Inserts the queued characters, with the ids they were given. One the table already has
     * (it called it equal to another) is not inserted, and its id is replaced with the existing one.
     */
    fun flush(conn: Connection) {
        if (queued.isEmpty()) return
        val sql = "INSERT IGNORE INTO $targetSchema.m_character(id, name, alter_ego, publisher_id) VALUES (?, ?, ?, ?)"
        conn.prepareStatement(sql).use { statement ->
            queued.forEach {
                statement.setInt(1, it.id)
                statement.setString(2, it.name)
                statement.setString(3, it.alterEgo)
                statement.setInt(4, it.publisherId)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        val inserted = HashSet<Int>()
        conn.prepareStatement(
            "SELECT id FROM $targetSchema.m_character WHERE id BETWEEN ? AND ?"
        ).use { statement ->
            statement.setInt(1, queued.first().id)
            statement.setInt(2, queued.last().id)
            statement.executeQuery().use { rows -> while (rows.next()) inserted += rows.getInt(1) }
        }
        queued.filter { it.id !in inserted }.forEach { missing ->
            val existing = existingId(missing, conn) ?: error(
                "m_character would not take '${missing.name}' / '${missing.alterEgo}' / ${missing.publisherId} and has no such character"
            )
            replaced[missing.id] = existing
            local[keyOf(missing.name, missing.alterEgo, missing.publisherId)] = existing
        }
        queued.clear()
    }

    /** The lowest id of the character as the table compares text, or null. */
    private fun existingId(character: New, conn: Connection): Int? =
        conn.prepareStatement(
            "SELECT MIN(id) FROM $targetSchema.m_character WHERE name = ? AND publisher_id = ? AND alter_ego <=> ?"
        ).use { statement ->
            statement.setString(1, character.name)
            statement.setInt(2, character.publisherId)
            statement.setString(3, character.alterEgo)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getObject(1)?.let { rows.getInt(1) } else null }
        }

    companion object {
        /** The longest name or alter ego `m_character` holds. */
        private const val MAX_LENGTH = 255

        private fun String.truncate(): String = if (length > MAX_LENGTH) substring(0, MAX_LENGTH) else this

        internal fun keyOf(name: String, alterEgo: String?, publisherId: Int) =
            Key(normalize(name.truncate()), alterEgo?.truncate()?.let(::normalize), publisherId)

        /**
         * Text as the database compares it, near enough: no case, no accents, no invisible
         * characters (zero-width and direction marks, which the database skips), no trailing spaces.
         * Where it still differs, [flush] sorts it out.
         */
        internal fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(MARKS, "")
                .replace(INVISIBLE, "")
                .lowercase(Locale.ROOT)
                .trimEnd(' ')

        private val MARKS = Regex("\\p{M}+")
        private val INVISIBLE = Regex("[\\p{Cf}\\p{Cc}]+")
    }
}
