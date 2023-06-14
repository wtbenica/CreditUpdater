package dev.benica.di

import dagger.Component
import dagger.Module
import dagger.Provides
import dev.benica.Credentials.Companion.PASSWORD
import dev.benica.Credentials.Companion.USERNAME
import dev.benica.db.ConnectionSource
import dev.benica.db.DatabaseUtil
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.inject.Singleton

@Module
class DatabaseModule() {
    @Provides
    @Singleton
    fun provideConnectionSource(): ConnectionSource {
        val connectionProps = Properties()
        connectionProps["user"] = USERNAME
        connectionProps["password"] = PASSWORD
        return object : ConnectionSource() {
            override fun getConnection(database: String): Connection {
                return DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/$database",
                    connectionProps
                )
            }
        }
    }
}

@Component(modules = [DatabaseModule::class])
@Singleton
fun interface DatabaseComponent {
    fun inject(databaseUtil: DatabaseUtil)
}