package com.kanae.soreada

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kanae.soreada.ui.SoreadaRoot

class MainActivity : ComponentActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)
    private var requestedBookId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SoreadaRoot(
                externalUri = externalUri,
                requestedBookId = requestedBookId,
                onExternalUriConsumed = { externalUri = null },
                onRequestedBookConsumed = { requestedBookId = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        intent.data?.let { externalUri = it }
        intent.getStringExtra(EXTRA_OPEN_BOOK_ID)?.let { requestedBookId = it }
    }

    companion object {
        const val EXTRA_OPEN_BOOK_ID = "com.kanae.soreada.OPEN_BOOK_ID"
    }
}
