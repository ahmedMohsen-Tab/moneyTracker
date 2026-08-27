/**
 * Hilt module that exposes [UserPreferences] as a `@Singleton`.
 *
 * The DataStore instance itself is created via the
 * `Context.dataStore` extension defined inside
 * [com.moneytracker.data.local.preferences.UserPreferences] so it stays
 * scoped to the application context (no leaks).
 */
package com.moneytracker.di

import android.content.Context
import com.moneytracker.data.local.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences = UserPreferences(context)
}
