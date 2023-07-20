package dev.benica.creditupdater.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.Credentials

class ConnectionProvider {
    companion object {
        private var connectionMap: MutableMap<String, HikariDataSource> = mutableMapOf()

        fun getConnection(targetSchema: String): HikariDataSource {
            if (!connectionMap.containsKey(targetSchema) || connectionMap[targetSchema]?.isClosed == true) {
                connectionMap[targetSchema] = createConnection(targetSchema)
            }
            @Suppress("kotlin:S6611")
            return connectionMap[targetSchema]!!
        }

        fun createConnection(targetSchema: String): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://localhost:3306/$targetSchema"
                username = Credentials.USERNAME_INITIALIZER
                password = Credentials.PASSWORD_INITIALIZER
                driverClassName = "com.mysql.cj.jdbc.Driver"
                maximumPoolSize = MAX_CONNECTION_POOL_SIZE
                minimumIdle = 5
                idleTimeout = 10000
                connectionTimeout = 5000
            }
        )

        const val MAX_CONNECTION_POOL_SIZE = 100
    }
}