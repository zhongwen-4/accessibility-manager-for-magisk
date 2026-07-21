package com.accessibilitymanager

import android.content.Context
import androidx.core.content.edit

internal enum class ManagerThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromPreference(value: String?): ManagerThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

internal data class ManagerPreferences(
    val themeMode: ManagerThemeMode = ManagerThemeMode.SYSTEM,
    val bottomBarFrost: Float = DEFAULT_BOTTOM_BAR_FROST,
)

internal class ManagerPreferencesStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ManagerPreferences = ManagerPreferences(
        themeMode = ManagerThemeMode.fromPreference(preferences.getString(KEY_THEME_MODE, null)),
        bottomBarFrost = sanitizeBottomBarFrost(
            preferences.getFloat(KEY_BOTTOM_BAR_FROST, DEFAULT_BOTTOM_BAR_FROST),
        ),
    )

    fun saveThemeMode(themeMode: ManagerThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, themeMode.name) }
    }

    fun saveBottomBarFrost(value: Float) {
        preferences.edit {
            putFloat(KEY_BOTTOM_BAR_FROST, sanitizeBottomBarFrost(value))
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "manager_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_BOTTOM_BAR_FROST = "bottom_bar_frost"
    }
}

internal const val DEFAULT_BOTTOM_BAR_FROST = 0.6f

internal fun sanitizeBottomBarFrost(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else DEFAULT_BOTTOM_BAR_FROST
