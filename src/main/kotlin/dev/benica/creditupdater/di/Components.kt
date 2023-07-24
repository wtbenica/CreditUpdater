/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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