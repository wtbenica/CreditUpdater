package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.db.NameCatalog
import dev.benica.creditupdater.extractor.utils.CharacterParser
import dev.benica.creditupdater.models.Individual
import dev.benica.creditupdater.models.Team
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CharacterParserCatalogTest {
    private val catalog = NameCatalog(
        characterNames = listOf("Superman", "Fenris", "Ace Morgan"),
        groupNames = listOf("Challengers of the Unknown", "Fenris", "Justice League", "JLA"),
    )

    @Test
    @DisplayName("a name the gcd knows as a character is an individual, however many names are bracketed")
    fun characterWithSeveralNames() {
        val parsed = CharacterParser.parseCharacters("Superman [Clark Kent; Kal-El] (cameo)", catalog)

        assertEquals(listOf(Individual("Superman", "Clark Kent", "cameo; also as Kal-El")), parsed)
    }

    @Test
    @DisplayName("without the catalog the same text is the team it used to be")
    fun withoutTheCatalog() {
        val parsed = CharacterParser.parseCharacters("Superman [Clark Kent; Kal-El]")

        assertEquals(listOf(Team("Superman", "Clark Kent; Kal-El", "")), parsed)
    }

    @Test
    @DisplayName("a name the gcd knows as a group is a team, even with members that have no alter egos")
    fun groupWithPlainMembers() {
        val parsed = CharacterParser.parseCharacters(
            "Challengers of the Unknown [Ace Morgan; Prof Haley; Red Ryan; Rocky Davis]", catalog
        )

        assertEquals(
            listOf(Team("Challengers of the Unknown", "Ace Morgan; Prof Haley; Red Ryan; Rocky Davis", "")),
            parsed
        )
    }

    @Test
    @DisplayName("a group with one bracketed member, or none, is still a team")
    fun groupWithOneOrNoMembers() {
        val parsed = CharacterParser.parseCharacters("Justice League [Flash]; JLA", catalog)

        assertEquals(listOf(Team("Justice League", "Flash", ""), Team("JLA", "", "")), parsed)
    }

    @Test
    @DisplayName("a name that is both, or neither, goes by the brackets")
    fun bothOrNeither() {
        val parsed = CharacterParser.parseCharacters(
            "Fenris [Girl Fenris; Male Fenris]; Nobody Known [One]; Nobody Else [A; B]", catalog
        )

        assertEquals(
            listOf(
                Team("Fenris", "Girl Fenris; Male Fenris", ""),
                Individual("Nobody Known", "One", ""),
                Team("Nobody Else", "A; B", ""),
            ),
            parsed
        )
    }

    @Test
    @DisplayName("names are matched without case or punctuation")
    fun matchedLoosely() {
        assertEquals(NameCatalog.Kind.GROUP, catalog.kindOf("justice-league"))
        assertEquals(NameCatalog.Kind.CHARACTER, catalog.kindOf("SUPERMAN"))
        assertEquals(NameCatalog.Kind.UNKNOWN, catalog.kindOf("Batman"))
    }
}
