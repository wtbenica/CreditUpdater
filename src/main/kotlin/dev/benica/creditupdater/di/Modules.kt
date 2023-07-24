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