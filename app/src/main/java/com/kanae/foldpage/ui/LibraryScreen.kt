package com.kanae.foldpage.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import com.kanae.foldpage.data.Book
import com.kanae.foldpage.data.BookFormat
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
) {
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    SideEffect {
        (context.findActivity())?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    var showAbout by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Book?>(null) }
    var pendingRestart by remember { mutableStateOf<Book?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("摺頁", fontWeight = FontWeight.Bold)
                        if (state.books.isNotEmpty()) {
                            Text(
                                "共 ${state.books.size} 本書",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "關於摺頁")
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
                text = { Text("匯入書籍") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.loading && state.books.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("正在整理書庫…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.books.isEmpty() -> {
                    EmptyLibrary(onPick = { picker.launch(arrayOf("*/*")) })
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.books, key = { it.id }) { book ->
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
                            Text("正在匯入書籍…")
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
            title = { Text("移除這本書？") },
            text = {
                Text("《${book.title}》會從書庫移除。你手機上的原始檔案不受影響。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(book)
                        pendingDelete = null
                    },
                ) {
                    Text("移除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    pendingRestart?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingRestart = null },
            icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
            title = { Text("從第一頁重新開始？") },
            text = { Text("《${book.title}》目前的閱讀進度會被清除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestart(book)
                        pendingRestart = null
                    },
                ) {
                    Text("重新開始")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestart = null }) { Text("取消") }
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("摺頁 0.1.0") },
            text = {
                Text(
                    "為漫畫而生的 EPUB／TXT 閱讀器。\n\n" +
                        "直向顯示完整一頁；橫向或展開摺疊屏時，自動並排顯示左右兩頁，留白以白色填充。\n\n" +
                        "支援 EPUB（圖像頁與文字）、TXT（UTF-8／GBK 自動辨識）。",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("知道啦") }
            },
        )
    }
}

@Composable
private fun EmptyLibrary(onPick: () -> Unit) {
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
        Text("書庫還是空的", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "匯入 EPUB 漫畫或 TXT 小說，\n直向一頁、橫向兩頁，慢慢看。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("選擇檔案")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "支援 .epub 與 .txt",
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
            contentDescription = "《${book.title}》，已讀 $percent%"
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
                            contentDescription = "《${book.title}》更多選項",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("繼續閱讀") },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpen()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("從第一頁重新開始") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onRestart()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("從書庫移除") },
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
                        "$percent%",
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
            contentDescription = "《${book.title}》封面",
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
