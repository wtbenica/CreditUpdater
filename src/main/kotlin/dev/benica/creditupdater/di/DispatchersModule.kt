package dev.benica.creditupdater.di

import dagger.Component
import dagger.Provides
import dagger.Module
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
interface DispatchersComponent {
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher

    @Named("Main")
    fun provideMainDispatcher(): CoroutineDispatcher

    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher
}