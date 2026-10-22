package com.kanae.foldpage.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kanae.foldpage.data.Book
import com.kanae.foldpage.data.BookFormat
import com.kanae.foldpage.ui.i18n.LocalUiStrings
import com.kanae.foldpage.ui.i18n.UiKey
import com.kanae.foldpage.ui.theme.LocalAppDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onOpenBook: (Book) -> Unit,
    onImport: (Uri) -> Unit,
    onDelete: (Book) -> Unit,
    onRestart: (Book) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalUiStrings.current
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = LocalAppDarkTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Book?>(null) }
    var pendingRestart by remember { mutableStateOf<Book?>(null) }
    val focusRequester = remember { FocusRequester() }

    SideEffect {
        context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus() else keyboard?.hide()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }

    val trimmedQuery = query.trim()
    val visibleBooks = remember(state.books, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            state.books
        } else {
            state.books.filter { book ->
                book.title.contains(trimmedQuery, ignoreCase = true) ||
                    book.author?.contains(trimmedQuery, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.get(UiKey.APP_NAME), fontWeight = FontWeight.Bold)
                        if (state.books.isNotEmpty()) {
                            Text(
                                if (state.books.size == 1) {
                                    strings.get(UiKey.BOOKS_COUNT_ONE)
                                } else {
                                    strings.get(UiKey.BOOKS_COUNT, state.books.size)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (searchActive) {
                        IconButton(onClick = {
                            searchActive = false
                            query = ""
                            keyboard?.hide()
                        }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = strings.get(UiKey.CLOSE_SEARCH),
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            searchActive = true
                        }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = strings.get(UiKey.SEARCH_DESC),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = strings.get(UiKey.SETTINGS_DESC),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!state.importing) picker.launch(arrayOf("*/*")) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(strings.get(UiKey.IMPORT_BOOKS)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (searchActive) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = { Text(strings.get(UiKey.SEARCH_HINT)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = strings.get(UiKey.CLOSE_SEARCH),
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.books.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                strings.get(UiKey.LOADING_LIBRARY),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    state.books.isEmpty() -> {
                        EmptyLibrary(
                            onPick = { picker.launch(arrayOf("*/*")) },
                        )
                    }
                    visibleBooks.isEmpty() -> {
                        NoResults()
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 112.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(visibleBooks, key = { it.id }) { book ->
                                BookCard(
                                    book = book,
                                    onOpen = { onOpenBook(book) },
                                    onRestart = { pendingRestart = book },
                                    onDelete = { pendingDelete = book },
                                )
                            }
                        }
                    }
                }

                if (state.importing) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(16.dp))
                                Text(strings.get(UiKey.IMPORTING))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text(strings.get(UiKey.REMOVE_TITLE)) },
            text = { Text(strings.get(UiKey.REMOVE_MESSAGE, book.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(book)
                        pendingDelete = null
                    },
                ) {
                    Text(strings.get(UiKey.REMOVE), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.get(UiKey.CANCEL))
                }
            },
        )
    }

    pendingRestart?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingRestart = null },
            icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
            title = { Text(strings.get(UiKey.RESTART_TITLE)) },
            text = { Text(strings.get(UiKey.RESTART_MESSAGE, book.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestart(book)
                        pendingRestart = null
                    },
                ) {
                    Text(strings.get(UiKey.RESTART))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestart = null }) {
                    Text(strings.get(UiKey.CANCEL))
                }
            },
        )
    }
}

@Composable
private fun NoResults() {
    val strings = LocalUiStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            strings.get(UiKey.NO_RESULTS),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            strings.get(UiKey.NO_RESULTS_HINT),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyLibrary(onPick: () -> Unit) {
    val strings = LocalUiStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(150.dp, 120.dp)) {
            val pageWidth = size.width * 0.36f
            val pageHeight = size.height * 0.72f
            val top = size.height * 0.14f
            val gap = size.width * 0.015f
            drawRoundRect(
                color = primaryContainer,
                topLeft = Offset(size.width * 0.50f + gap, top),
                size = Size(pageWidth, pageHeight),
                cornerRadius = CornerRadius(14f, 14f),
            )
            drawRoundRect(
                color = primary,
                topLeft = Offset(size.width * 0.50f - pageWidth - gap, top),
                size = Size(pageWidth, pageHeight),
                cornerRadius = CornerRadius(14f, 14f),
            )
            drawLine(
                color = tertiary,
                start = Offset(size.width * 0.50f - pageWidth + 16f, top + pageHeight * 0.30f),
                end = Offset(size.width * 0.50f - 16f, top + pageHeight * 0.30f),
                strokeWidth = 5f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(size.width * 0.50f + 16f, top + pageHeight * 0.30f),
                end = Offset(size.width * 0.50f + pageWidth - 16f, top + pageHeight * 0.30f),
                strokeWidth = 5f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(size.width * 0.50f + 16f, top + pageHeight * 0.42f),
                end = Offset(size.width * 0.50f + pageWidth - 30f, top + pageHeight * 0.42f),
                strokeWidth = 5f,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            strings.get(UiKey.EMPTY_TITLE),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            strings.get(UiKey.EMPTY_SUBTITLE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.get(UiKey.CHOOSE_FILE))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            strings.get(UiKey.SUPPORTED_FORMATS),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookCard(
    book: Book,
    onOpen: () -> Unit,
    onRestart: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalUiStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coverFile = remember(book.coverFileName, context) {
        book.coverFileName?.let { File(context.filesDir, "foldpage/covers/$it") }
    }
    val percent = (book.progress * 100).roundToInt()

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.semantics {
            contentDescription = "${book.title}，${strings.get(UiKey.READ_PERCENT, percent)}"
        },
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f),
            ) {
                BookCover(book = book, coverFile = coverFile)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                ) {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.32f), CircleShape),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = strings.get(UiKey.MORE_OPTIONS, book.title),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.get(UiKey.CONTINUE_READING)) },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpen()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(strings.get(UiKey.RESTART_READING)) },
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onRestart()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(strings.get(UiKey.REMOVE_FROM_LIBRARY)) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.45f),
                )
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author?.takeIf { it.isNotBlank() } ?: " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (book.format == BookFormat.EPUB) "EPUB" else "TXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        strings.get(UiKey.READ_PERCENT, percent),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookCover(book: Book, coverFile: File?) {
    val strings = LocalUiStrings.current
    var coverBitmap by remember(coverFile?.absolutePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(coverFile?.absolutePath) {
        coverBitmap = withContext(Dispatchers.IO) {
            coverFile?.takeIf { it.exists() }?.let {
                runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull()
            }
        }
    }
    val loaded = coverBitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = strings.get(UiKey.COVER_OF, book.title),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (book.format == BookFormat.EPUB) "EPUB" else "TXT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}
