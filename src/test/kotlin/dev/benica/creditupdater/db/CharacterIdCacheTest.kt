package dev.benica.creditupdater.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CharacterIdCacheTest {
    @Test
    @DisplayName("characters are numbered in the order they are first met, from 1")
    fun numbersInOrderOfFirstMeeting() {
        val cache = CharacterIdCache("s")
        assertEquals(1, cache.idFor("Batman", "Bruce Wayne", 2))
        assertEquals(2, cache.idFor("Robin", null, 2))
        assertEquals(3, cache.idFor("Joker", null, 2))
        assertEquals(3, cache.waiting)
    }

    @Test
    @DisplayName("the same character, however many times it is met, has the one id")
    fun theSameCharacterHasOneId() {
        val cache = CharacterIdCache("s")
        val batman = cache.idFor("Batman", "Bruce Wayne", 2)
        cache.idFor("Robin", null, 2)
        assertEquals(batman, cache.idFor("Batman", "Bruce Wayne", 2))
        assertEquals(2, cache.waiting)
    }

    @Test
    @DisplayName("spellings the database sees as equal are one character: case, accents, trailing spaces")
    fun spellingsTheDatabaseSeesAsEqualAreOne() {
        val cache = CharacterIdCache("s")
        val id = cache.idFor("Spider-Man", "Peter Parker", 3)
        assertEquals(id, cache.idFor("SPIDER-MAN", "peter parker", 3))
        assertEquals(id, cache.idFor("Spider-Man ", "Peter Parker  ", 3))
        assertEquals(cache.idFor("Zoé", null, 3), cache.idFor("ZOE", null, 3))
        assertEquals(2, cache.waiting)
    }

    @Test
    @DisplayName("a different alter ego, or publisher, or none, is another character")
    fun differencesThatMatter() {
        val cache = CharacterIdCache("s")
        val batman = cache.idFor("Batman", "Bruce Wayne", 2)
        assertNotEquals(batman, cache.idFor("Batman", "Dick Grayson", 2))
        assertNotEquals(batman, cache.idFor("Batman", "Bruce Wayne", 3))
        assertNotEquals(batman, cache.idFor("Batman", null, 2))
    }

    @Test
    @DisplayName("a name over 255 characters is cut as the table cuts it, so it is found again")
    fun longNamesAreCut() {
        val cache = CharacterIdCache("s")
        val long = "x".repeat(300)
        assertEquals(cache.idFor(long, null, 2), cache.idFor(long, null, 2))
        assertEquals(1, cache.waiting)
    }

    @Test
    @DisplayName("invisible characters are ignored, as the database ignores them")
    fun invisibleCharactersAreIgnored() {
        val cache = CharacterIdCache("s")
        val id = cache.idFor("Black Canary", "Dinah Lance", 54)
        assertEquals(id, cache.idFor("Black Canary", "Dinah\u200e Lance", 54))
        assertEquals(id, cache.idFor("Black\u200b Canary", "\u200eDinah Lance", 54))
        assertEquals(1, cache.waiting)
    }

    @Test
    @DisplayName("normalizing ignores case, accents and trailing spaces and keeps the rest")
    fun normalizing() {
        assertEquals("zoe", CharacterIdCache.normalize("Zoé  "))
        assertEquals("a b", CharacterIdCache.normalize("A B"))
        assertNotEquals(CharacterIdCache.normalize("a b"), CharacterIdCache.normalize("ab"))
    }
}
