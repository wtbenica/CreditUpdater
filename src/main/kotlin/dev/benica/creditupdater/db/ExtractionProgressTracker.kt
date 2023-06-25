package dev.benica.creditupdater.db

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import dev.benica.creditupdater.TerminalUtil
import dev.benica.creditupdater.di.*
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Named

/**
 * A class that tracks the progress of an extraction.
 *
 * @param extractedType the type of data being extracted
 * @param targetSchema the schema to extract data from
 * @param totalItems the total number of items to extract
 * @param dispatchAndExecuteComponent the [DispatchAndExecuteComponent] to
 *     use, defaults to a new [DaggerDispatchAndExecuteComponent]
 *       - Note: This is only used for testing
 *
 * @param logger the [KLogger] to use, defaults to a new [KotlinLogging]
 *     logger
 *    - Note: This is only used for testing
 */
class ExtractionProgressTracker(
    private val extractedType: String,
    targetSchema: String,
    private val totalItems: Int = 0,
    fileName: String = "progress.json",
    dispatchAndExecuteComponent: DispatchAndExecuteComponent = DaggerDispatchAndExecuteComponent.create(),
    private val logger: KLogger = KotlinLogging.logger(this::class.java.simpleName)
) {
    @Volatile
    var progressInfo: ProgressInfo
        private set

    @Inject
    internal lateinit var queryExecutorSource: QueryExecutorSource

    private val queryExecutor: QueryExecutor

    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher

    internal var progressInfoMap: MutableMap<String, ProgressInfo>

    private val progressFile = File(fileName)

    init {
        dispatchAndExecuteComponent.inject(this)
        queryExecutor = queryExecutorSource.getQueryExecutor(targetSchema)

        logger.debug { "Initializing progress tracker for $extractedType" }
        progressInfoMap = loadProgressInfo()

        logger.debug { "Progress tracker initialized for $extractedType" }
        progressInfo = progressInfoMap.getOrDefault(
            key = extractedType,
            defaultValue = ProgressInfo()
        )
        logger.debug { "Progress info initialized for $extractedType" }
        CoroutineScope(ioDispatcher).launch {
            progressInfo.numCompleted = getItemsCompleted()
            logger.debug { "Progress info updated for $extractedType: $progressInfo" }
        }
    }

    internal fun loadProgressInfo(): MutableMap<String, ProgressInfo> =
        if (progressFile.exists()) {
            val gson = Gson()
            FileReader(progressFile).use { reader ->
                gson.fromJson(reader, object : TypeToken<MutableMap<String, ProgressInfo>>() {}.type)
            }
        } else {
            mutableMapOf()
        }

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

    /**
     * Saves [progress] to the progress file. If [progress] is null, saves the
     * current progress information.
     */
    internal fun saveProgressInfo(progress: ProgressInfo? = null) {
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
        val pctComplete: Float =
            totalItems.let { (progressInfo.numCompleted.toFloat() / it) }

        val averageTime: Long = progressInfo.totalTimeMillis / progressInfo.numCompleted
        val remainingTime: Long? = getRemainingTime(totalItems, averageTime, progressInfo.numCompleted)
        val remaining = TerminalUtil.millisToPretty(remainingTime)
        val elapsed = TerminalUtil.millisToPretty(progressInfo.totalTimeMillis)

        TerminalUtil.upNLines(5)
        logger.info("Extract $extractedType | StoryId: ${progressInfo.lastProcessedItemId}")
        logger.info("Complete: ${progressInfo.numCompleted}${totalItems.let { "/$it" }} ${pctComplete.toPercent()}")
        logger.info("Avg: ${averageTime}ms")
        logger.info("Elapsed: $elapsed ETR: $remaining")
        logger.info(getProgressBar(pctComplete))
    }

    internal fun getProgressBar(pctComplete: Float): String {
        val progressBar = StringBuilder()
        progressBar.append("[")
        for (i in 0..100) {
            if (i < pctComplete.toInt()) {
                progressBar.append("=")
            } else if (i == pctComplete.toInt()) {
                progressBar.append(">")
            } else {
                progressBar.append(".")
            }
        }
        progressBar.append("] ${(pctComplete / 100f).toPercent()}")
        return progressBar.toString()
    }

    internal fun getItemsCompleted(): Int =
        queryExecutor.getItemCount("gcd_story", "id <= ${progressInfo.lastProcessedItemId}")

    companion object {
        /**
         * Returns the ID of the last item that was processed for [itemType].
         *
         * @param itemType the type of item to get the last processed ID for
         */
        @JvmStatic
        fun getLastProcessedItemId(itemType: String, fileName: String = "progress.json"): Int? {
            val progressFile = File(fileName)
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

internal fun Float.toPercent(): String {
    val decimal = String.format("%.2f", this * 100)
    return "$decimal%"
}

