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
    internal val extractedType: String,
    targetSchema: String,
    private val totalItems: Int = 0,
    internal val progressInfoMap: ProgressInfoMap = ProgressInfoMap(),
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

    init {
        dispatchAndExecuteComponent.inject(this)
        queryExecutor = queryExecutorSource.getQueryExecutor(targetSchema)

        logger.debug { "Progress tracker initialized for $extractedType" }
        progressInfo = progressInfoMap.get(extractedType)
        logger.debug { "Progress info initialized for $extractedType" }
        CoroutineScope(ioDispatcher).launch {
            progressInfo.numCompleted = getItemsCompleted()
            logger.debug { "Progress info updated for $extractedType: $progressInfo" }
        }
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
            progressInfoMap.saveProgressInfo(progressInfo, extractedType)
        }
    }

    /** Resets the progress information for the current item being updated. */
    fun resetProgressInfo() {
        progressInfo = ProgressInfo()
        progressInfoMap.saveProgressInfo(progressInfo, extractedType)
    }

    /** Prints progress information for the current item being updated. */
    internal fun printProgressInfo() {
        val pctComplete: Float =
            totalItems.let { (progressInfo.numCompleted.toFloat() / it) }

        val averageTime: Long = progressInfo.totalTimeMillis / (progressInfo.numCompleted.takeIf { it > 0 } ?: 1)
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
        val pct = 100 * pctComplete
        val progressBar = StringBuilder()
        progressBar.append("[")
        for (i in 0..100) {
            if (i < pct.toInt()) {
                progressBar.append("=")
            } else if (i == pct.toInt()) {
                progressBar.append(">")
            } else {
                progressBar.append(".")
            }
        }
        progressBar.append("] ${(pct / 100f).toPercent()}")
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


class ProgressInfoMap(private val fileName: String = "progress.json") {
    internal val mProgressInfoMap: MutableMap<String, ExtractionProgressTracker.Companion.ProgressInfo>

    init {
        val file = File(fileName)
        if (file.exists()) {
            FileReader(file).use { reader ->
                mProgressInfoMap =
                    Gson().fromJson(
                        reader,
                        object :
                            TypeToken<MutableMap<String, ExtractionProgressTracker.Companion.ProgressInfo>>() {}.type
                    )
            }
        } else {
            mProgressInfoMap = mutableMapOf()
        }
    }

    fun get(key: String) = mProgressInfoMap.getOrDefault(key, ExtractionProgressTracker.Companion.ProgressInfo())

    /**
     * Saves [progress] to the progress file. If [progress] is null, saves the
     * current progress information.
     */
    internal fun saveProgressInfo(
        progress: ExtractionProgressTracker.Companion.ProgressInfo,
        extractedType: String
    ) {
        mProgressInfoMap[extractedType] = progress
        FileWriter(fileName).use { writer ->
            Gson().toJson(mProgressInfoMap, writer)
        }
    }
}

