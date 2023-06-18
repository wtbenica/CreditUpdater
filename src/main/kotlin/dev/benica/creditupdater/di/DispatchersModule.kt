package dev.benica.creditupdater.di

import dagger.Component
import dagger.Provides
import dagger.Module
import dev.benica.creditupdater.db.ExtractionProgressTracker
import dev.benica.creditupdater.db_tasks.DBInitializer
import dev.benica.creditupdater.db_tasks.DBMigrator
import dev.benica.creditupdater.db_tasks.DBTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

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

@Component(modules = [DispatchersModule::class])
@Singleton
fun interface DispatchersComponent {
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher
}

@Component(modules = [DispatchersModule::class, DatabaseModule::class])
@Singleton
interface DatabaseWorkerComponent {
    fun inject(tracker: ExtractionProgressTracker)
    fun inject(dbTask: DBTask)
    fun inject(dbInitializer: DBInitializer)
    fun inject(dbMigrator: DBMigrator)
}