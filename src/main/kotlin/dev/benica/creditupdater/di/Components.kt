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
interface DatabaseComponent {
    fun inject(queryExecutor: QueryExecutor)
    fun inject(characterRepository: CharacterRepository)
    fun inject(creditRepository: CreditRepository)
    fun inject(dbTask: DBTask)
}

@Component(modules = [DispatchersModule::class, ConnectionProviderModule::class])
@Singleton
interface DispatchAndExecuteComponent {
    fun inject(tracker: ExtractionProgressTracker)
    fun inject(dbInitializer: DBInitializer)
    fun inject(dbMigrator: DBMigrator)
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