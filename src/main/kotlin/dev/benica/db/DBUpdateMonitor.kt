package dev.benica.db

import dev.benica.Credentials.Companion.DEFAULT_BATCH_SIZE
import dev.benica.TerminalUtil
import dev.benica.converter.Extractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import toPercent
import java.sql.SQLException
import kotlin.system.measureTimeMillis

class DBUpdateMonitor(
    private val connectionSource: ConnectionSource,
    private val database: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger: KLogger
        get() = KotlinLogging.logger { }

    /**
     * Uses [extractor] to extract objects from text fields in the items from
     * [selectItemsQuery] and inserts them into the database.
     *
     * @param startingComplete the starting number of completed items
     * @param totalItems the total number of items to update, or null if
     *     unknown
     * @param batchSize the batch size to use
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    suspend fun extractAndInsertItems(
        selectItemsQuery: String,
        startingComplete: Long,
        totalItems: Int?,
        extractor: Extractor,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ) {
        var currentComplete: Long = startingComplete
        var totalTimeMillis: Long = 0
        var offset = 0

        try {
            while (true) {
                val resultSet = withContext(ioDispatcher) {
                    val conn = connectionSource.getConnection(database)
                    val statement = conn.createStatement()
                    val queryWithLimitAndOffset = "$selectItemsQuery LIMIT $batchSize OFFSET ${offset * batchSize}"
                    statement.executeQuery(queryWithLimitAndOffset)
                }

                if (!resultSet.next()) {
                    logger.info { "No more items to update" }
                    break
                }

                while (resultSet.next()) {
                    totalTimeMillis += measureTimeMillis {
                        currentComplete++

                        val itemId = extractor.extractAndInsert(resultSet)

                        printProgressInfo(
                            itemId = itemId,
                            currentComplete = currentComplete,
                            currentDurationMillis = totalTimeMillis,
                            startingComplete = startingComplete,
                            totalItems = totalItems,
                            extractor = extractor
                        )
                    }
                }
            }

            offset++
        } catch (ex: SQLException) {
            logger.error("Error updating items", ex)
            throw ex
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
        val remainingTime: Long? = getRemainingTime(totalItems, averageTime, numComplete)
        val remaining = TerminalUtil.millisToPretty(remainingTime)
        val elapsed = TerminalUtil.millisToPretty(currentDurationMillis)

        TerminalUtil.upNLines(4)
        logger.info { "Extract ${extractor.extractedItem} ${extractor.fromValue}: $itemId" }
        logger.info { "Complete: $currentComplete${totalItems?.let { "/$it" } ?: ""} $pctComplete" }
        logger.info { "Avg: ${averageTime}ms" }
        logger.info { "Elapsed: $elapsed ETR: $remaining" }
    }

    fun getRemainingTime(totalItems: Int?, averageTime: Long, numComplete: Long) =
        totalItems?.let { averageTime * (it - numComplete) }

}