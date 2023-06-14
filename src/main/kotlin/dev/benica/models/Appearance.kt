package dev.benica.models

/** Appearance - represents a character appearance in a story. */
data class Appearance(
    val storyId: Int,
    val characterId: Int,
    val appearanceInfo: String?,
    val notes: String?,
    val membership: String?,
    val id: Int = 0
)
