package com.kanae.foldpage

import android.app.Application
import com.kanae.foldpage.data.AppSettingsStore
import com.kanae.foldpage.data.LibraryRepository
import com.kanae.foldpage.data.ReaderSettingsStore

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
