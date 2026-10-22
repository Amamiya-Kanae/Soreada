package com.kanae.foldpage.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanae.foldpage.FoldPageApplication

@Composable
fun FoldPageRoot(
    externalUri: Uri?,
    requestedBookId: String?,
    onExternalUriConsumed: () -> Unit,
    onRequestedBookConsumed: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as FoldPageApplication
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(app.library),
    )
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var openBookId by rememberSaveable { mutableStateOf(requestedBookId) }
    var readerSession by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(requestedBookId) {
        val id = requestedBookId ?: return@LaunchedEffect
        openBookId = id
        readerSession += 1
        onRequestedBookConsumed()
    }

    LaunchedEffect(externalUri) {
        val uri = externalUri ?: return@LaunchedEffect
        libraryViewModel.import(uri, openAfter = true)
        onExternalUriConsumed()
    }

    LaunchedEffect(Unit) {
        libraryViewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.OpenBook -> {
                    openBookId = event.bookId
                    readerSession += 1
                }
                is LibraryEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(openBookId) {
        if (openBookId == null) {
            // Give the reader a beat to flush its final page position first.
            kotlinx.coroutines.delay(250)
            libraryViewModel.refresh()
        }
    }

    val currentBookId = openBookId
    if (currentBookId == null) {
        LibraryScreen(
            state = libraryState,
            snackbarHostState = snackbarHostState,
            onOpenBook = { book ->
                libraryViewModel.markOpened(book)
                openBookId = book.id
                readerSession += 1
            },
            onImport = { uri -> libraryViewModel.import(uri) },
            onDelete = libraryViewModel::delete,
            onRestart = { book ->
                libraryViewModel.restart(book) { updated ->
                    openBookId = updated.id
                    readerSession += 1
                }
            },
        )
    } else {
        ReaderScreen(
            bookId = currentBookId,
            session = readerSession,
            onBack = { openBookId = null },
            onBookFinished = { libraryViewModel.refresh() },
        )
    }
}
