package com.kanae.foldpage.data

import android.content.Context

class ReaderSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)

    fun load(): ReaderSettings {
        // 0.2.0 changes the default to left-to-right. Migrate the old AUTO
        // default once, while keeping AUTO available as a manual choice.
        if (!prefs.getBoolean("ltr_default_migrated", false)) {
            val old = prefs.getString("direction", null)
            val editor = prefs.edit().putBoolean("ltr_default_migrated", true)
            if (old == null || old == DirectionSetting.AUTO.name) {
                editor.putString("direction", DirectionSetting.LTR.name)
            }
            editor.apply()
        }
        return ReaderSettings(
        direction = runCatching {
            DirectionSetting.valueOf(prefs.getString("direction", DirectionSetting.LTR.name)!!)
        }.getOrDefault(DirectionSetting.LTR),
        dualMode = runCatching {
            DualMode.valueOf(prefs.getString("dual_mode", DualMode.AUTO.name)!!)
        }.getOrDefault(DualMode.AUTO),
        fontSizeSp = prefs.getInt("font_size", 18).coerceIn(14, 30),
    )
    }

    fun save(settings: ReaderSettings) {
        prefs.edit()
            .putString("direction", settings.direction.name)
            .putString("dual_mode", settings.dualMode.name)
            .putInt("font_size", settings.fontSizeSp)
            .apply()
    }
}
