package com.kanae.foldpage.data

import android.content.Context

class ReaderSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)

    fun load(): ReaderSettings = ReaderSettings(
        direction = runCatching {
            DirectionSetting.valueOf(prefs.getString("direction", DirectionSetting.AUTO.name)!!)
        }.getOrDefault(DirectionSetting.AUTO),
        dualMode = runCatching {
            DualMode.valueOf(prefs.getString("dual_mode", DualMode.AUTO.name)!!)
        }.getOrDefault(DualMode.AUTO),
        fontSizeSp = prefs.getInt("font_size", 18).coerceIn(14, 30),
    )

    fun save(settings: ReaderSettings) {
        prefs.edit()
            .putString("direction", settings.direction.name)
            .putString("dual_mode", settings.dualMode.name)
            .putInt("font_size", settings.fontSizeSp)
            .apply()
    }
}
