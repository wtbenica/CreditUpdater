package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials
import java.sql.DriverManager
import java.util.UUID

/**
 * The schemas the database tests run in. They are made when a test first asks for one, named for
 * this run, and dropped when the run ends, so tests need nothing set up on the server beyond an
 * account that may create databases, and two runs can't trample each other.
 *
 * Schemas left behind by a run that was killed are dropped by the next run, once they are old
 * enough that no run could still be using them.
 */
object TestDatabases {
    private const val PREFIX = "gcd_test_"
    private const val STALE_AFTER_SECONDS = 6 * 60 * 60L
    private val NAME = Regex("""^${PREFIX}(\d+)_[0-9a-f]{8}(_update)?$""")

    private val names: Pair<String, String> by lazy {
        val base = "$PREFIX${System.currentTimeMillis() / 1000}_${UUID.randomUUID().toString().take(8)}"
        val pair = base to "${base}_update"
        server { statement ->
            dropStale(statement)
            statement.execute("CREATE DATABASE `${pair.first}`")
            statement.execute("CREATE DATABASE `${pair.second}`")
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            runCatching {
                server { statement ->
                    statement.execute("DROP DATABASE IF EXISTS `${pair.first}`")
                    statement.execute("DROP DATABASE IF EXISTS `${pair.second}`")
                }
            }
        })
        pair
    }

    /** The schema most tests build their fixtures in. */
    val TEST_DATABASE: String get() = names.first

    /** The schema standing in for a newer GCD dump, for the migration tests. */
    val TEST_DATABASE_UPDATE: String get() = names.second

    private fun dropStale(statement: java.sql.Statement) {
        val now = System.currentTimeMillis() / 1000
        val found = statement.executeQuery("SHOW DATABASES LIKE '${PREFIX.replace("_", "\\_")}%'").use { rows ->
            generateSequence { if (rows.next()) rows.getString(1) else null }.toList()
        }
        found.forEach { name ->
            val made = NAME.matchEntire(name)?.groupValues?.get(1)?.toLongOrNull() ?: return@forEach
            if (now - made > STALE_AFTER_SECONDS) statement.execute("DROP DATABASE IF EXISTS `$name`")
        }
    }

    private fun server(block: (java.sql.Statement) -> Unit) {
        DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/", Credentials.USERNAME_INITIALIZER, Credentials.PASSWORD_INITIALIZER
        ).use { connection -> connection.createStatement().use(block) }
    }
}
