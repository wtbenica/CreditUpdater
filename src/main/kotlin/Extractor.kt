import java.sql.Connection
import java.sql.ResultSet

fun String.prepareName(): String = this
    .replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    .replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    .replace(Regex("\\s*\\?\\s*"), "")
    .replace(Regex("^\\s*"), "")
    .cleanup()

fun String.cleanup(): String = this
    .replace(Regex("^[\\s]*"), "")
    .replace(Regex("[\\s]*$"), "")

abstract class Extractor(
    protected val database: String,
    protected val conn: Connection
) {

    abstract suspend fun extract(
        extractFrom: ResultSet,
        destDatabase: String? = null
    ): Int

    abstract fun finish()
}
