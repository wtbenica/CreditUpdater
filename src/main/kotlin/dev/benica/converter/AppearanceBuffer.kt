package dev.benica.converter

import dev.benica.Credentials
import dev.benica.db.Repository
import dev.benica.models.Appearance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.time.Instant


/**
 * A buffer for temporarily storing new character appearances before saving
 * them to the database. This buffer is designed to improve performance by
 * allowing multiple appearances to be saved at once.
 */
class AppearanceBuffer(database: String, conn: Connection) {
    private val _appearances: MutableSet<Appearance> = mutableSetOf()
    private val appearances: Set<Appearance>
        get() = _appearances
    private var lastUpdated: Long = Instant.MAX.epochSecond
    private val repository = Repository(database, conn)

    /**
     * Adds a new appearance to the buffer. If the buffer has reached its
     * limit, the current set of appearances will be saved to the database.
     *
     * @param appearance the appearance to add to the buffer
     */
    @Synchronized
    fun addToBuffer(appearance: Appearance?) {
        if (appearance != null) {
            // Add the appearance to the set and update the timestamp
            _appearances.add(appearance)
            lastUpdated = Instant.now().epochSecond

            // If the set has reached the collector limit, save the current set of appearances
            if (_appearances.size >= Credentials.COLLECTOR_LIMIT) {
                save()
            }
        }
    }

    /**
     * Saves the current set of appearances in the buffer to the database.
     */
    @Synchronized
    fun save() {
        // a copy of 'appearances'
        val appearances = this.appearances.toSet()
        _appearances.clear()
        CoroutineScope(Dispatchers.IO).launch {
            //            logger.info { "Saving character appearances" }
            repository.insertCharacterAppearances(appearances)
        }
    }
}
