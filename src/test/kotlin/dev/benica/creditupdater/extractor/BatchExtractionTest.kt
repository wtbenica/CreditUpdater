package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.db.TestDatabases.TEST_DATABASE
import dev.benica.creditupdater.db.CharacterIdCache
import dev.benica.creditupdater.db.DBState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.ResultSet

/** Extracting a chunk of stories at once, against the test database. */
@Tag("db")
class BatchExtractionTest {
    private lateinit var conn: Connection

    @BeforeEach
    fun setUp() {
        TestDatabaseSetup.setup(dbState = DBState.INIT_STEP_1_COMPLETE, schema = TEST_DATABASE, sourceSchema = null)
        conn = getTestDbConnection()
        conn.createStatement().use {
            it.execute("TRUNCATE TABLE m_character_appearance")
            it.execute("DELETE FROM m_character")
            it.execute("DELETE FROM m_story_credit")
        }
    }

    @AfterEach
    fun tearDown() = conn.close()

    private fun rows(sql: String, use: (ResultSet) -> Unit) =
        conn.createStatement().use { s -> s.executeQuery(sql).use { rs -> rs.next(); use(rs) } }

    private fun <T> query(sql: String, read: (ResultSet) -> T): List<T> =
        conn.createStatement().use { s -> s.executeQuery(sql).use { rs -> generateSequence { if (rs.next()) read(rs) else null }.toList() } }

    private fun characterStories(vararg stories: Triple<Int, String, Int>): String =
        stories.joinToString(" UNION ALL ") { "SELECT ${it.first} AS id, '${it.second}' AS characters, ${it.third} AS publisher_id" }

    /** Stories and publishers that are in the test database, which the tables the extractors fill refer to. */
    private fun storyIds(count: Int) = query("SELECT id FROM gcd_story ORDER BY id LIMIT $count") { it.getInt(1) }
    private fun publisherIds(count: Int) = query("SELECT id FROM gcd_publisher ORDER BY id LIMIT $count") { it.getInt(1) }

    @Test
    @DisplayName("characters are numbered in the order met, once each, and their appearances point at them")
    fun charactersAndAppearances() {
        val (s1, s2) = storyIds(2)
        val (p1, p2) = publisherIds(2)
        val extractor = CharacterExtractor(TEST_DATABASE)
        val sql = characterStories(
            Triple(s1, "Batman [Bruce Wayne]; Robin; robin", p1),
            Triple(s2, "batman [bruce wayne]; JOKER", p2),
        )
        var result = 0 to 0
        rows(sql) { result = extractor.extractAndInsertBatch(it, conn) }
        assertEquals(s2 to 2, result)

        val characters = query("SELECT id, name, alter_ego, publisher_id FROM m_character ORDER BY id") {
            listOf(it.getInt(1), it.getString(2), it.getString(3), it.getInt(4))
        }
        assertEquals(
            listOf(
                listOf(1, "Batman", "Bruce Wayne", p1),
                listOf(2, "Robin", null, p1),
                listOf(3, "batman", "bruce wayne", p2),
                listOf(4, "JOKER", null, p2),
            ),
            characters
        )

        val appearances = query("SELECT story_id, character_id FROM m_character_appearance ORDER BY story_id, character_id") {
            it.getInt(1) to it.getInt(2)
        }
        assertEquals(listOf(s1 to 1, s1 to 2, s2 to 3, s2 to 4), appearances)
    }

    @Test
    @DisplayName("characters already in the table keep their ids, and a second chunk goes on after the first")
    fun existingCharactersAndLaterChunks() {
        val (s1, s2) = storyIds(2)
        val (p1) = publisherIds(1)
        conn.createStatement().use {
            it.execute("INSERT INTO m_character(id, name, alter_ego, publisher_id) VALUES (40, 'Robin', NULL, $p1)")
        }
        val extractor = CharacterExtractor(TEST_DATABASE)

        rows(characterStories(Triple(s1, "Robin; Joker", p1))) { extractor.extractAndInsertBatch(it, conn) }
        rows(characterStories(Triple(s2, "Joker; Penguin", p1))) { extractor.extractAndInsertBatch(it, conn) }

        val ids = query("SELECT name, id FROM m_character ORDER BY id") { it.getString(1) to it.getInt(2) }
        assertEquals(listOf("Robin" to 40, "Joker" to 41, "Penguin" to 42), ids)
        assertEquals(
            listOf(s1 to 40, s1 to 41, s2 to 41, s2 to 42),
            query("SELECT story_id, character_id FROM m_character_appearance ORDER BY story_id, character_id") {
                it.getInt(1) to it.getInt(2)
            }
        )
    }

    @Test
    @DisplayName("a character the table calls equal to one it has, though we do not, gets the existing id")
    fun theTablesIdeaOfEqualWins() {
        // spellings this database's collation calls equal that the in-memory key does not
        val pair = listOf("Strasse" to "Straße", "Stras" to "Straß", "Fibre" to "\uFB01bre", "Aether" to "Æther")
            .firstOrNull { (a, b) ->
                CharacterIdCache.normalize(a) != CharacterIdCache.normalize(b) &&
                        query("SELECT '%s' COLLATE utf8mb4_uca1400_ai_ci = '%s'".format(a, b)) { it.getInt(1) }.single() == 1
            }
        Assumptions.assumeTrue(pair != null, "this collation has no pair the in-memory key misses")
        val (existing, spelling) = pair!!

        val (s1) = storyIds(1)
        val (p1) = publisherIds(1)
        conn.createStatement().use {
            // the collation and unique key the real m_character has (a null alter ego never conflicts
            // in a unique key, so the example has one)
            it.execute("ALTER TABLE m_character CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci")
            it.execute("ALTER TABLE m_character ADD UNIQUE KEY name_2 (name, alter_ego, publisher_id)")
            it.execute("INSERT INTO m_character(id, name, alter_ego, publisher_id) VALUES (40, '$existing Man', 'Bob', $p1)")
        }
        val extractor = CharacterExtractor(TEST_DATABASE)

        rows(characterStories(Triple(s1, "$spelling Man [Bob]; Joker", p1))) { extractor.extractAndInsertBatch(it, conn) }

        val ids = query("SELECT name, id FROM m_character ORDER BY id") { it.getString(1) to it.getInt(2) }
        assertEquals(listOf("$existing Man", "Joker"), ids.map { it.first })
        assertEquals(40, ids.first().second)
        assertEquals(
            listOf(s1 to 40, s1 to ids.last().second),
            query("SELECT story_id, character_id FROM m_character_appearance ORDER BY character_id") {
                it.getInt(1) to it.getInt(2)
            }
        )
    }

    @Test
    @DisplayName("credits: a name finds its creator, a credit already there is skipped, a name twice is one credit")
    fun credits() {
        // the fixture credits Frank Quitely (name detail 2) as penciler (type 2) of story 1, not of story 2
        val extractor = CreditExtractor(TEST_DATABASE)
        val stories = listOf(1, 2).joinToString(" UNION ALL ") {
            "SELECT $it AS id, '' AS script, 'Frank Quitely; Frank Quitely' AS pencils, '' AS inks, '' AS colors, '' AS letters, '' AS editing"
        }
        var result = 0 to 0
        rows(stories) { result = extractor.extractAndInsertBatch(it, conn) }
        assertEquals(2 to 2, result)

        val extracted = query("SELECT creator_id, credit_type_id, story_id FROM m_story_credit") {
            Triple(it.getInt(1), it.getInt(2), it.getInt(3))
        }
        assertEquals(listOf(Triple(2, 2, 2)), extracted)
    }
}
