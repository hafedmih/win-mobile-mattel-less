package com.famoco.kyctelcomr.core.di

import android.content.Context
import com.famoco.kyctelcomrtlib.PeripheralAccess
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PeripheralModule {

    @Provides
    @Singleton
    fun providePeripheralAccess(@ApplicationContext context: Context): PeripheralAccess {
        return PeripheralAccess(context)
    }
}