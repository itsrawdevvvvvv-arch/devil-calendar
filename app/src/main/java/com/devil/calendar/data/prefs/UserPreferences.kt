package com.devil.calendar.data.prefs

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val islandEnabled = booleanPreferencesKey("island_enabled")
        val islandAllowed = stringSetPreferencesKey("island_allowed_packages")
        val islandHaptics = booleanPreferencesKey("island_haptics")
        val islandScale = stringPreferencesKey("island_scale")
        val islandTopOffset = intPreferencesKey("island_top_offset")
        val islandCorner = intPreferencesKey("island_corner")
        val aiKeyCipher = stringPreferencesKey("ai_key_cipher")
        val islandTilt = booleanPreferencesKey("island_tilt")
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
    /** Defaults to the system 12/24-hour setting until the user picks one. */
    val use24Hour: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.use24Hour] ?: DateFormat.is24HourFormat(context)
    }

    val islandEnabled: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.islandEnabled] ?: false
    }

    val islandAllowedPackages: Flow<Set<String>> = context.dataStore.data.map { p ->
        p[Keys.islandAllowed] ?: emptySet()
    }

    val islandHaptics: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.islandHaptics] ?: true
    }

    val islandTilt: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.islandTilt] ?: true
    }

    suspend fun setIslandTilt(value: Boolean) {
        context.dataStore.edit { it[Keys.islandTilt] = value }
    }

    val islandScale: Flow<String> = context.dataStore.data.map { p ->
        p[Keys.islandScale] ?: "medium"
    }

    val islandTopOffset: Flow<Int> = context.dataStore.data.map { p ->
        p[Keys.islandTopOffset] ?: 6
    }

    val islandCorner: Flow<Int> = context.dataStore.data.map { p ->
        p[Keys.islandCorner] ?: 28
    }

    /** The encrypted Sarvam key blob. Never the plain key. */
    val aiKeyCipher: Flow<String?> = context.dataStore.data.map { it[Keys.aiKeyCipher] }

    suspend fun setAiKeyCipher(value: String?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(Keys.aiKeyCipher) else prefs[Keys.aiKeyCipher] = value
        }
    }

    suspend fun setIslandAllowedPackages(value: Set<String>) {
        context.dataStore.edit { it[Keys.islandAllowed] = value }
    }

    suspend fun setIslandHaptics(value: Boolean) {
        context.dataStore.edit { it[Keys.islandHaptics] = value }
    }

    suspend fun setIslandScale(value: String) {
        context.dataStore.edit { it[Keys.islandScale] = value }
    }

    suspend fun setIslandEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.islandEnabled] = value }
    }

    suspend fun setUse24Hour(value: Boolean) =
        context.dataStore.edit { it[Keys.use24Hour] = value }

    suspend fun setOnboardingCompleted(done: Boolean) =
        context.dataStore.edit { it[Keys.onboarding] = done }
}
