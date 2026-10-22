package com.kanae.foldpage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Debug-only entry point used by the build verification script.
 * It imports a file from the app's external files directory and opens it.
 * This class is not part of release builds.
 */
class TestImportActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_PATH)
            ?: File(filesDir, "test.epub").absolutePath
        val source = if (File(path).isAbsolute) File(path) else File(filesDir, path)
        Log.i("FoldPageTest", "start path=$path resolved=${source.absolutePath} exists=${source.exists()} len=${source.length()}")
        if (!source.exists()) {
            Log.e("FoldPageTest", "test file not found")
            Toast.makeText(this, "找不到測試檔：${source.absolutePath}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val app = application as SoreadaApplication
        scope.launch {
            try {
                val book = app.library.importFromFile(source, source.name)
                Log.i("FoldPageTest", "imported ${book.id} ${book.title} total=${book.total} kind=${book.contentKind}")
                withContext(Dispatchers.Main) {
                    startActivity(
                        Intent(this@TestImportActivity, MainActivity::class.java)
                            .putExtra(MainActivity.EXTRA_OPEN_BOOK_ID, book.id)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    )
                    finish()
                }
            } catch (e: Exception) {
                Log.e("FoldPageTest", "import failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TestImportActivity,
                        "匯入失敗：${e.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_PATH = "path"
    }
}
