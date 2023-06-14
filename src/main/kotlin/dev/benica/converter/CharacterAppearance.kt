package dev.benica.converter

sealed class CharacterAppearance {
    abstract val name: String
    abstract val appearanceInfo: String?
}

data class Individual(override val name: String, val alterEgo: String?, override val appearanceInfo: String?) :
    CharacterAppearance()

data class Team(override val name: String, val members: String, override val appearanceInfo: String?) :
    CharacterAppearance()
