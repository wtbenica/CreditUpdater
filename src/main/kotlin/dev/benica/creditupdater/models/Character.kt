package dev.benica.creditupdater.models

sealed class Character {
    abstract val name: String
    abstract val details: String?
}

data class Individual(override val name: String, val alterEgo: String?, override val details: String?) :
    Character()

data class Team(override val name: String, val members: String, override val details: String?) :
    Character()
