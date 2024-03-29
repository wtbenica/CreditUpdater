package dev.benica.creditupdater.models

/** Appearance - represents a character appearance in a story. */
data class Appearance(
    val storyId: Int,
    val characterId: Int,
    val details: String?,
    val notes: String?,
    val membership: String?,
    val id: Int = 0
)
