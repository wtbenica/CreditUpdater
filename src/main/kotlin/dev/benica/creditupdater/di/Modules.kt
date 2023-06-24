package dev.benica.creditupdater.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import dev.benica.creditupdater.Credentials.Companion.PASSWORD_INITIALIZER
import dev.benica.creditupdater.Credentials.Companion.USERNAME_INITIALIZER
import dev.benica.creditupdater.db.CharacterRepository
import dev.benica.creditupdater.db.CreditRepository
import dev.benica.creditupdater.db.QueryExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

abstract class ConnectionSource {
    abstract fun getConnection(targetSchema: String): HikariDataSource
}

interface Repository

abstract class RepoSource<T : Repository> {
    abstract fun getRepo(db: String): T
}

abstract class QueryExecutorSource {
    abstract fun getQueryExecutor(targetSchema: String): QueryExecutor
}

@Module
class ConnectionProviderModule {
    private var connectionMap: MutableMap<String, HikariDataSource> = mutableMapOf()

    @Provides
    @Singleton
    fun provideConnectionSource(): ConnectionSource = object : ConnectionSource() {
        override fun getConnection(targetSchema: String): HikariDataSource {
            if (!connectionMap.containsKey(targetSchema) || connectionMap[targetSchema]?.isClosed == true) {
                connectionMap[targetSchema] = createConnection(targetSchema)
            }
            @Suppress("kotlin:S6611")
            return connectionMap[targetSchema]!!
        }
    }

    fun createConnection(targetSchema: String): HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://localhost:3306/$targetSchema"
            username = USERNAME_INITIALIZER
            password = PASSWORD_INITIALIZER
            driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = MAX_CONNECTION_POOL_SIZE
            minimumIdle = 2
            idleTimeout = 3000
            connectionTimeout = 5000
        }
    )

    companion object {
        const val MAX_CONNECTION_POOL_SIZE = 2000
    }
}

@Module
class QueryExecutorProviderModule {
    @Provides
    @Singleton
    fun provideQueryExecutor(): QueryExecutorSource = object : QueryExecutorSource() {
        override fun getQueryExecutor(targetSchema: String): QueryExecutor = QueryExecutor(targetSchema)
    }
}

@Module
class CharacterRepositoryModule {
    @Provides
    @Singleton
    fun provideCharacterRepositorySource() = object : RepoSource<CharacterRepository>() {
        override fun getRepo(db: String): CharacterRepository {
            return CharacterRepository(db)
        }
    }
}

@Module
class CreditRepositoryModule {
    @Provides
    @Singleton
    fun provideCreditRepositorySource() = object : RepoSource<CreditRepository>() {
        override fun getRepo(db: String): CreditRepository {
            return CreditRepository(db)
        }
    }
}

@Module
class DispatchersModule {
    @Provides
    @Singleton
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @Named("Main")
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}