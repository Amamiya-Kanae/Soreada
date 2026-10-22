package com.kanae.soreada

import android.app.Application
import com.kanae.soreada.data.AppSettingsStore
import com.kanae.soreada.data.LibraryRepository
import com.kanae.soreada.data.ReaderSettingsStore

class SoreadaApplication : Application() {
    lateinit var library: LibraryRepository
        private set
    lateinit var readerSettings: ReaderSettingsStore
        private set
    lateinit var appSettings: AppSettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        library = LibraryRepository(this)
        readerSettings = ReaderSettingsStore(this)
        appSettings = AppSettingsStore(this)
    }
}
