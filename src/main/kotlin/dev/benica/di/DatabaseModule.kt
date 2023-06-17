package dev.benica.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import dagger.Module
import dagger.Provides
import dev.benica.Credentials.Companion.MAX_CONNECTION_POOL_SIZE
import dev.benica.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.db.ConnectionSource
import dev.benica.db.DatabaseUtil
import java.sql.Connection
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideConnectionSource(): ConnectionSource {
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
fun interface DatabaseComponent {
    fun inject(databaseUtil: DatabaseUtil)
}

