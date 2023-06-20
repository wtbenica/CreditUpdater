package dev.benica.creditupdater.di

import dagger.Component
import dev.benica.creditupdater.db.*
import dev.benica.creditupdater.db_tasks.DBInitializer
import dev.benica.creditupdater.db_tasks.DBMigrator
import dev.benica.creditupdater.db_tasks.DBTask
import dev.benica.creditupdater.extractor.CharacterExtractor
import dev.benica.creditupdater.extractor.CreditExtractor
import javax.inject.Singleton

@Component(modules = [ConnectionProviderModule::class])
@Singleton
fun interface DatabaseComponent {
    fun inject(queryExecutor: QueryExecutor)
}

@Component(modules = [DispatchersModule::class, QueryExecutorProviderModule::class])
@Singleton
fun interface DispatchAndExecuteComponent {
    fun inject(tracker: ExtractionProgressTracker)
}

@Component(modules = [QueryExecutorProviderModule::class])
@Singleton
interface QueryExecutorComponent {
    fun inject(dbTask: DBTask)
    fun inject(charRepo: CharacterRepository)
    fun inject(creditRepository: CreditRepository)
}

@Component(modules = [DispatchersModule::class])
@Singleton
interface DispatchersComponent {
    fun inject(migrator: DBMigrator)
    fun inject(initializer: DBInitializer)
}

@Component(modules = [CharacterRepositoryModule::class])
@Singleton
fun interface CharacterRepositoryComponent {
    fun inject(extractor: CharacterExtractor)
}

@Component(modules = [CreditRepositoryModule::class])
@Singleton
fun interface CreditRepositoryComponent {
    fun inject(extractor: CreditExtractor)

}