package dev.benica.creditupdater.models

sealed class Character {
    abstract val name: String
    abstract val appearanceInfo: String?
}

data class Individual(override val name: String, val alterEgo: String?, override val appearanceInfo: String?) :
    Character()

data class Team(override val name: String, val members: String, override val appearanceInfo: String?) :
    Character()
