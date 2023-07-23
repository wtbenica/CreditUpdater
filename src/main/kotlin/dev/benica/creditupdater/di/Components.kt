package dev.benica.creditupdater.di

import dagger.Component
import dev.benica.creditupdater.db.ExtractionProgressTracker
import dev.benica.creditupdater.db_tasks.DBInitializer
import dev.benica.creditupdater.db_tasks.DBMigrator
import dev.benica.creditupdater.extractor.CharacterExtractor
import dev.benica.creditupdater.extractor.CreditExtractor
import javax.inject.Singleton

@Component(modules = [DispatchersModule::class])
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