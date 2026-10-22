package com.devil.calendar.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devil_prefs")

/** Profile fields shown on Home, the widget and Profile. */
data class UserProfile(
    val name: String = "",
    val goal: String = "",
    val avatarPath: String? = null,
) {
    val displayName: String get() = name.ifBlank { "You" }
    val initial: String get() = (name.trim().firstOrNull() ?: 'Y').uppercaseChar().toString()
    val displayGoal: String get() = goal.ifBlank { "Set your goal in profile" }
}

data class AppSettings(
    val onboardingCompleted: Boolean = false,
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val name = stringPreferencesKey("profile_name")
        val goal = stringPreferencesKey("profile_goal")
        val avatar = stringPreferencesKey("profile_avatar_path")
        val onboarding = booleanPreferencesKey("onboarding_completed")
    }

    val profile: Flow<UserProfile> = context.dataStore.data.map { p ->
        UserProfile(
            name = p[Keys.name].orEmpty(),
            goal = p[Keys.goal].orEmpty(),
            avatarPath = p[Keys.avatar],
        )
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(onboardingCompleted = p[Keys.onboarding] ?: false)
    }

    suspend fun setName(value: String) = context.dataStore.edit { it[Keys.name] = value.take(20) }
    suspend fun setGoal(value: String) = context.dataStore.edit { it[Keys.goal] = value.take(60) }
    suspend fun setAvatarPath(path: String?) = context.dataStore.edit { p ->
        if (path == null) p.remove(Keys.avatar) else p[Keys.avatar] = path
    }
    suspend fun setOnboardingCompleted(done: Boolean) =
        context.dataStore.edit { it[Keys.onboarding] = done }
}
