package dev.benica.creditupdater.db

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import dev.benica.creditupdater.TerminalUtil
import dev.benica.creditupdater.di.DaggerDatabaseWorkerComponent
import dev.benica.creditupdater.di.DatabaseWorkerComponent
import dev.benica.creditupdater.toPercent
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Named

class ExtractionProgressTracker(
    private val extractedType: String,
    private val database: String,
    private val totalItems: Int = 0,
    databaseComponent: DatabaseWorkerComponent = DaggerDatabaseWorkerComponent.create(),
) {
    @Inject
    internal lateinit var connectionSource: ConnectionSource

    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher

    init {
        databaseComponent.inject(this)
    }

    @Volatile
    var progressInfo: ProgressInfo
        private set

    /**
     * Updates the current progress and saves it to the progress file.
     *
     * @param lastProcessedItemId the ID of the last item that was processed
     * @param totalTimeMillis the total time it took to process the last item
     */
    fun updateProgressInfo(
        lastProcessedItemId: Int,
        totalTimeMillis: Long,
    ) {
        synchronized(this) {
            progressInfo.lastProcessedItemId = lastProcessedItemId
            progressInfo.totalTimeMillis = totalTimeMillis
            progressInfo.numCompleted += 1
            printProgressInfo()
            saveProgressInfo()
        }
    }

    /** Resets the progress information for the current item being updated. */
    fun resetProgressInfo() {
        saveProgressInfo(ProgressInfo())
    }

    private var progressInfoMap: MutableMap<String, ProgressInfo> = mutableMapOf()


    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    init {
        logger.debug { "Initializing progress tracker for $extractedType" }
        if (progressFile.exists()) {
            val gson = Gson()
            FileReader(progressFile).use { reader ->
                progressInfoMap =
                    gson.fromJson(reader, object : TypeToken<MutableMap<String, ProgressInfo>>() {}.type)
            }
        }
        logger.debug { "Progress tracker initialized for $extractedType" }
        progressInfo = progressInfoMap.getOrDefault(
            key = extractedType,
            defaultValue = ProgressInfo()
        )
        logger.debug { "Progress info initialized for $extractedType" }
        CoroutineScope(ioDispatcher).launch {
            getItemsCompleted().let { progressInfo.numCompleted = it }
            logger.debug { "Progress info updated for $extractedType: $progressInfo" }
        }
    }

    /**
     * Saves [progress] to the progress file. If [progress] is null, saves the
     * current progress information.
     */
    private fun saveProgressInfo(progress: ProgressInfo? = null) {
        if (progress != null) {
            progressInfo.lastProcessedItemId = progress.lastProcessedItemId
            progressInfo.totalTimeMillis = progress.totalTimeMillis
            progressInfo.numCompleted = progress.numCompleted
        }
        progressInfoMap[extractedType] = progressInfo
        val gson = Gson()
        FileWriter(progressFile).use { writer ->
            gson.toJson(progressInfoMap, writer)
        }
    }

    /** Prints progress information for the current item being updated. */
    private fun printProgressInfo() {
        val pctComplete: String =
            totalItems.let { (progressInfo.numCompleted.toFloat() / it).toPercent() }

        val averageTime: Long = progressInfo.totalTimeMillis / progressInfo.numCompleted
        val remainingTime: Long? = getRemainingTime(totalItems, averageTime, progressInfo.numCompleted)
        val remaining = TerminalUtil.millisToPretty(remainingTime)
        val elapsed = TerminalUtil.millisToPretty(progressInfo.totalTimeMillis)

        TerminalUtil.upNLines(4)
        logger.info("Extract $extractedType | StoryId: ${progressInfo.lastProcessedItemId}")
        logger.info("Complete: ${progressInfo.numCompleted}${totalItems.let { "/$it" }} $pctComplete")
        logger.info("Avg: ${averageTime}ms")
        logger.info("Elapsed: $elapsed ETR: $remaining")
    }

    private suspend fun getItemsCompleted(): Int =
        withContext(ioDispatcher) {
            connectionSource.getConnection(database).use { conn ->
                conn.createStatement().use { statement ->
                    statement.executeQuery(
                        """SELECT COUNT(*) 
                                |FROM $database.gcd_story 
                                |WHERE id <= ${progressInfo.lastProcessedItemId}""".trimMargin()
                    ).use { resultSet ->
                        resultSet.next()
                        resultSet.getInt(1)
                    }
                }
            }
        }

    companion object {
        private val progressFile = File("progress.json")

        /**
         * Returns the ID of the last item that was processed for [itemType].
         *
         * @param itemType the type of item to get the last processed ID for
         */
        @JvmStatic
        fun getLastProcessedItemId(itemType: String): Int? {
            return if (progressFile.exists()) {
                val gson = Gson()
                val progressInfo: MutableMap<String, ProgressInfo>
                FileReader(progressFile).use { reader ->
                    progressInfo =
                        gson.fromJson(reader, object : TypeToken<MutableMap<String, ProgressInfo>>() {}.type)
                }
                progressInfo.getOrDefault(itemType, null)?.lastProcessedItemId ?: 0
            } else {
                null
            }
        }

        /**
         * Returns the remaining time for the update process to complete.
         *
         * @param totalItems the total number of items to update
         * @param averageTime the average time it takes to update an item
         * @param numComplete the number of items that have been updated
         */
        internal fun getRemainingTime(totalItems: Int?, averageTime: Long, numComplete: Int) =
            totalItems?.let { averageTime * (it - numComplete) }

        data class ProgressInfo(
            @SerializedName("lastProcessedItemId") var lastProcessedItemId: Int = 0,
            @SerializedName("totalTimeMillis") var totalTimeMillis: Long = 0,
            @SerializedName("numCompleted") var numCompleted: Int = 0,
        )
    }
}