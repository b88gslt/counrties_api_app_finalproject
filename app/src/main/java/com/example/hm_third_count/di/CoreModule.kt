package com.example.hm_third_count.di

import com.example.hm_third_count.data.repository.Clock
import com.example.hm_third_count.data.repository.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
