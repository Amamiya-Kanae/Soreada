package com.kanae.foldpage.data

import android.content.Context

enum class AppLanguage { TRADITIONAL, SIMPLIFIED, ENGLISH }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val language: AppLanguage = AppLanguage.TRADITIONAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val eyeCareEnabled: Boolean = false,
    val eyeCareStrength: Float = 0.45f,
    val grainEnabled: Boolean = false,
    val grainStrength: Float = 0.35f,
)

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        language = runCatching {
            AppLanguage.valueOf(prefs.getString("language", AppLanguage.TRADITIONAL.name)!!)
        }.getOrDefault(AppLanguage.TRADITIONAL),
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM),
        eyeCareEnabled = prefs.getBoolean("eye_care_enabled", false),
        eyeCareStrength = prefs.getFloat("eye_care_strength", 0.45f).coerceIn(0f, 1f),
        grainEnabled = prefs.getBoolean("grain_enabled", false),
        grainStrength = prefs.getFloat("grain_strength", 0.35f).coerceIn(0f, 1f),
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("language", settings.language.name)
            .putString("theme_mode", settings.themeMode.name)
            .putBoolean("eye_care_enabled", settings.eyeCareEnabled)
            .putFloat("eye_care_strength", settings.eyeCareStrength)
            .putBoolean("grain_enabled", settings.grainEnabled)
            .putFloat("grain_strength", settings.grainStrength)
            .apply()
    }
}
