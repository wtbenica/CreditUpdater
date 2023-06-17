package dev.benica.db

import dev.benica.TerminalUtil
import dev.benica.converter.Extractor
import kotlinx.coroutines.coroutineScope
import mu.KLogger
import mu.KotlinLogging
import toPercent
import java.sql.SQLException
import kotlin.system.measureTimeMillis

class DBUpdateMonitor(private val connectionSource: ConnectionSource, private val database: String) {
    private val logger: KLogger
        get() = KotlinLogging.logger { }

    /**
     * Selects items using [selectItemsQuery], extracts data using [extractor],
     * that inserts new objects into [database].
     *
     * @param startingComplete the starting number of completed items
     * @param totalItems the total number of items to update, or null if
     *     unknown
     */
    suspend fun extractAndInsertItems(
        selectItemsQuery: String,
        startingComplete: Long,
        totalItems: Int?,
        extractor: Extractor,
        batchSize: Int = totalItems?.let { it / 20 } ?: 100000
    ) {
        var currentComplete: Long = startingComplete
        var totalTimeMillis: Long = 0
        var offset = 0

        try {
            coroutineScope {
                while (true) {
                    val conn = connectionSource.getConnection(database)
                    val statement = conn.createStatement()

                    val queryWithLimitOffset = "$selectItemsQuery LIMIT $batchSize OFFSET ${offset * batchSize}"

                    val executeQuery = statement.executeQuery(queryWithLimitOffset)

                    if (!executeQuery.next()) {
                        logger.info { "No more items to update" }
                        break
                    }

                    executeQuery.use { resultSet ->
                        do {
                            totalTimeMillis += measureTimeMillis {
                                currentComplete++

                                val itemId: Int = extractor.extractAndInsert(resultSet)

                                printProgressInfo(
                                    itemId = itemId,
                                    currentComplete = currentComplete,
                                    currentDurationMillis = totalTimeMillis,
                                    startingComplete = startingComplete,
                                    totalItems = totalItems,
                                    extractor = extractor
                                )
                            }
                        } while (resultSet.next())
                    }

                    offset++
                }
            }
        } catch (ex: SQLException) {
            logger.error("Error updating items", ex)
        }

        logger.info { "END\n\n\n" }
    }

    /**
     * Prints progress information for the current item being updated.
     *
     * @param itemId the ID of the current item being updated
     * @param currentComplete the current number of completed items
     * @param currentDurationMillis the current duration of the update process
     *     in milliseconds
     */
    private fun printProgressInfo(
        itemId: Int,
        currentComplete: Long,
        currentDurationMillis: Long,
        startingComplete: Long,
        totalItems: Int?,
        extractor: Extractor
    ) {
        val numComplete = currentComplete - startingComplete
        val pctComplete: String =
            totalItems?.let { (currentComplete.toFloat() / it).toPercent() }
                ?: "???"

        val averageTime: Long = currentDurationMillis / numComplete
        val remainingTime: Long? = totalItems?.let { averageTime * (it - numComplete) }
        val remaining = TerminalUtil.millisToPretty(remainingTime)
        val elapsed = TerminalUtil.millisToPretty(currentDurationMillis)

        TerminalUtil.upNLines(4)
        logger.info { "Extract ${extractor.extractedItem} ${extractor.fromValue}: $itemId" }
        logger.info { "Complete: $currentComplete${totalItems?.let { "/$it" } ?: ""} $pctComplete" }
        logger.info { "Avg: ${averageTime}ms" }
        logger.info { "Elapsed: $elapsed ETR: $remaining" }
    }

}