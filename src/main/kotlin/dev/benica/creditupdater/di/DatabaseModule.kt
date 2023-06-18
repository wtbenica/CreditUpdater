package dev.benica.creditupdater.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import dagger.Module
import dagger.Provides
import dev.benica.creditupdater.Credentials.Companion.MAX_CONNECTION_POOL_SIZE
import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.creditupdater.db.ConnectionSource
import dev.benica.creditupdater.db.DatabaseUtil
import dev.benica.creditupdater.db.SqlQueryExecutor
import mu.KLogger
import mu.KotlinLogging
import java.sql.Connection
import javax.inject.Singleton

@Module
class DatabaseModule {
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    @Provides
    @Singleton
    fun provideConnectionSource(): ConnectionSource {
        logger.info { "Creating connection source." }
        return object : ConnectionSource() {
            override fun getConnection(database: String): Connection {
                return HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = "jdbc:mysql://localhost:3306/$database"
                        username = USERNAME_INITIALIZER
                        password = PASSWORD_INITIALIZER
                        driverClassName = "com.mysql.cj.jdbc.Driver"
                        maximumPoolSize = MAX_CONNECTION_POOL_SIZE
                        minimumIdle = 2
                        idleTimeout = 3000
                        connectionTimeout = 5000
                    }
                ).connection
            }
        }
    }
}

@Component(modules = [DatabaseModule::class])
@Singleton
interface DatabaseComponent {
    fun inject(dbUtil: DatabaseUtil)
    fun inject(dbUtil: SqlQueryExecutor)
}

