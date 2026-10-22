package com.kanae.soreada.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanae.soreada.SoreadaApplication
import com.kanae.soreada.data.AppSettings
import com.kanae.soreada.ui.components.EyeCareOverlay
import com.kanae.soreada.ui.i18n.LocalUiStrings
import com.kanae.soreada.ui.i18n.UiStrings
import com.kanae.soreada.ui.i18n.errorText
import com.kanae.soreada.ui.theme.SoreadaTheme

@Composable
fun SoreadaRoot(
    externalUri: Uri?,
    requestedBookId: String?,
    onExternalUriConsumed: () -> Unit,
    onRequestedBookConsumed: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as SoreadaApplication
    var settings by remember { mutableStateOf(app.appSettings.load()) }
    val strings = remember(settings.language) { UiStrings(settings.language) }

    val updateSettings: ((AppSettings) -> AppSettings) -> Unit = { transform ->
        val next = transform(settings)
        settings = next
        app.appSettings.save(next)
    }

    Box(Modifier.fillMaxSize()) {
        SoreadaTheme(settings.themeMode) {
            CompositionLocalProvider(LocalUiStrings provides strings) {
                RootContent(
                    settings = settings,
                    onUpdateSettings = updateSettings,
                    externalUri = externalUri,
                    requestedBookId = requestedBookId,
                    onExternalUriConsumed = onExternalUriConsumed,
                    onRequestedBookConsumed = onRequestedBookConsumed,
                )
            }
        }
        EyeCareOverlay(settings)
    }
}

@Composable
private fun RootContent(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    externalUri: Uri?,
    requestedBookId: String?,
    onExternalUriConsumed: () -> Unit,
    onRequestedBookConsumed: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as SoreadaApplication
    val strings = LocalUiStrings.current
    val currentStrings by rememberUpdatedState(strings)
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(app.library),
    )
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var openBookId by rememberSaveable { mutableStateOf(requestedBookId) }
    var readerSession by rememberSaveable { mutableIntStateOf(0) }
    var showGlobalSettings by rememberSaveable { mutableStateOf(false) }

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
                is LibraryEvent.Message -> {
                    snackbarHostState.showSnackbar(
                        currentStrings.get(event.key, *event.args.toTypedArray()),
                    )
                }
                is LibraryEvent.Failure -> {
                    snackbarHostState.showSnackbar(currentStrings.errorText(event.error))
                }
            }
        }
    }

    LaunchedEffect(openBookId) {
        if (openBookId == null) {
            kotlinx.coroutines.delay(250)
            libraryViewModel.refresh()
        }
    }

    when {
        showGlobalSettings -> {
            GlobalSettingsScreen(
                settings = settings,
                onUpdate = onUpdateSettings,
                onBack = { showGlobalSettings = false },
            )
        }
        openBookId == null -> {
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
                onOpenSettings = { showGlobalSettings = true },
            )
        }
        else -> {
            ReaderScreen(
                bookId = openBookId!!,
                session = readerSession,
                appSettings = settings,
                onUpdateSettings = onUpdateSettings,
                onBack = { openBookId = null },
                onBookFinished = { libraryViewModel.refresh() },
            )
        }
    }
}
