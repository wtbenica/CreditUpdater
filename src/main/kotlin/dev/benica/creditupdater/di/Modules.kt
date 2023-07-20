package dev.benica.creditupdater.di

import dagger.Module
import dagger.Provides
import dev.benica.creditupdater.db.CharacterRepository
import dev.benica.creditupdater.db.CreditRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

interface Repository

abstract class RepoSource<T : Repository> {
    abstract fun getRepo(db: String): T
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