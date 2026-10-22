package com.kanae.foldpage.ui

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanae.foldpage.R
import com.kanae.foldpage.SoreadaApplication
import com.kanae.foldpage.data.AppSettings
import com.kanae.foldpage.data.Bookmark
import com.kanae.foldpage.data.DirectionSetting
import com.kanae.foldpage.data.DualMode
import com.kanae.foldpage.data.PageDirection
import com.kanae.foldpage.reader.TextPage
import com.kanae.foldpage.reader.TextTypo
import com.kanae.foldpage.ui.components.PaperGrainOverlay
import com.kanae.foldpage.ui.components.ZoomablePage
import com.kanae.foldpage.ui.i18n.LocalUiStrings
import com.kanae.foldpage.ui.i18n.UiKey
import com.kanae.foldpage.ui.i18n.UiStrings
import com.kanae.foldpage.ui.i18n.errorText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    bookId: String,
    session: Int,
    appSettings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    onBookFinished: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as SoreadaApplication
    val owner = remember(bookId, session) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val viewModel: ReaderViewModel = viewModel(
        viewModelStoreOwner = owner,
        key = "reader-$bookId-$session",
        factory = ReaderViewModelFactory(app.library, bookId, app.readerSettings),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalUiStrings.current

    val view = LocalView.current
    val window = LocalContext.current.findActivity()?.window
    SideEffect {
        window?.let {
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    BackHandler { onBack() }

    DisposableEffect(owner) {
        onDispose {
            viewModel.close()
            owner.viewModelStore.clear()
            onBookFinished()
        }
    }

    when {
        state.loading -> ReaderLoading(strings)
        state.error != null -> ReaderError(
            failure = state.error!!,
            strings = strings,
            onRetry = viewModel::load,
            onBack = onBack,
        )
        else -> ReaderContent(
            state = state,
            viewModel = viewModel,
            strings = strings,
            appSettings = appSettings,
            onUpdateSettings = onUpdateSettings,
            onBack = onBack,
        )
    }
}

@Composable
private fun ReaderLoading(strings: UiStrings) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF3F5A87))
            Spacer(Modifier.height(16.dp))
            Text(strings.get(UiKey.LOADING_BOOK), color = Color(0xFF5F5F66))
        }
    }
}

@Composable
private fun ReaderError(
    failure: ReaderFailure,
    strings: UiStrings,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                strings.get(UiKey.CANNOT_OPEN),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                strings.errorText(failure.error),
                color = Color(0xFF5F5F66),
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) {
                    Text(strings.get(UiKey.BACK_TO_LIBRARY))
                }
                Button(onClick = onRetry) {
                    Text(strings.get(UiKey.RETRY))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    strings: UiStrings,
    appSettings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val view = LocalView.current
    val window = context.findActivity()?.window
    val controller = remember(window, view) {
        window?.let { WindowCompat.getInsetsController(it, view) }
    }

    var chromeVisible by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var zoomedPage by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        if (showSettings) showSettings = false else onBack()
    }

    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(controller, chromeVisible) {
        if (controller == null) return@LaunchedEffect
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(chromeVisible, showSettings) {
        if (chromeVisible && !showSettings) {
            delay(6000)
            chromeVisible = false
        }
    }

    val rtl = state.direction == PageDirection.RTL

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val dual = when (state.settings.dualMode) {
            DualMode.AUTO -> widthDp >= 600.dp && heightDp >= 480.dp && widthDp > heightDp
            DualMode.DUAL -> true
            DualMode.SINGLE -> false
        }
        val pps = if (dual) 2 else 1
        val pageCount = state.pageCount

        if (state.isText) {
            val horizontalPadding = with(density) { TextTypo.HORIZONTAL_PADDING_DP.dp.toPx() }
            val verticalPadding = with(density) { TextTypo.VERTICAL_PADDING_DP.dp.toPx() }
            val slotWidth = with(density) { (maxWidth / pps).toPx() }
            val pageWidth = (slotWidth - horizontalPadding * 2f).roundToInt()
            val pageHeight = (with(density) { maxHeight.toPx() } - verticalPadding * 2f).roundToInt()
            val bodyPx = with(density) { state.settings.fontSizeSp.sp.toPx() }
            LaunchedEffect(pageWidth, pageHeight, bodyPx) {
                viewModel.paginate(pageWidth, pageHeight, bodyPx)
            }
        }

        val totalSpreads = if (pageCount <= 0) 0 else (pageCount + pps - 1) / pps
        val lastSpreadStart = if (pageCount <= 0) 0 else ((pageCount - 1) / pps) * pps
        val spreadStart = if (pageCount <= 0) {
            0
        } else {
            (state.page.coerceIn(0, pageCount - 1) / pps) * pps
        }
        val firstTextPage = state.textPages.getOrNull(spreadStart)
        val lastTextPage = state.textPages.getOrNull(
            (spreadStart + pps - 1).coerceAtMost((pageCount - 1).coerceAtLeast(0)),
        )
        val bookmarkRange = if (state.isText) {
            if (firstTextPage != null && lastTextPage != null) {
                firstTextPage.absStart until lastTextPage.absEnd
            } else {
                IntRange.EMPTY
            }
        } else {
            spreadStart until ((spreadStart + pps).coerceAtMost(pageCount))
        }
        val currentBookmark = state.bookmarks.firstOrNull { it.position in bookmarkRange }
        val currentPosition = if (state.isText) firstTextPage?.absStart ?: 0 else spreadStart

        LaunchedEffect(spreadStart, pps) {
            zoomedPage = null
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(pageCount, spreadStart, rtl, pps, zoomedPage) {
                    detectTapGestures { offset ->
                        if (pageCount <= 0) return@detectTapGestures
                        if (zoomedPage != null) {
                            chromeVisible = !chromeVisible
                            return@detectTapGestures
                        }
                        val fraction = offset.x / size.width
                        val next = (spreadStart + pps).coerceAtMost(lastSpreadStart)
                        val previous = (spreadStart - pps).coerceAtLeast(0)
                        when {
                            !rtl && fraction > 0.70f -> viewModel.jumpTo(next)
                            !rtl && fraction < 0.30f -> viewModel.jumpTo(previous)
                            rtl && fraction < 0.30f -> viewModel.jumpTo(next)
                            rtl && fraction > 0.70f -> viewModel.jumpTo(previous)
                            else -> chromeVisible = !chromeVisible
                        }
                    }
                },
        ) {
            if (totalSpreads > 0) {
                val pagerState = rememberPagerState(
                    initialPage = (state.page / pps).coerceIn(0, totalSpreads - 1),
                    pageCount = { totalSpreads },
                )
                val currentPps by rememberUpdatedState(pps)
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.collect { spread ->
                        viewModel.onPagerSettled(spread * currentPps, currentPps)
                    }
                }
                LaunchedEffect(state.page, pps, totalSpreads) {
                    val target = (state.page / pps).coerceIn(0, (totalSpreads - 1).coerceAtLeast(0))
                    if (pagerState.currentPage != target) {
                        pagerState.animateScrollToPage(target)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    reverseLayout = rtl,
                    key = { it },
                    beyondViewportPageCount = 1,
                    userScrollEnabled = zoomedPage == null,
                    modifier = Modifier.fillMaxSize(),
                ) { spread ->
                    val firstPage = spread * pps
                    val slots = if (rtl && pps == 2) {
                        listOf(firstPage + 1, firstPage)
                    } else {
                        List(pps) { firstPage + it }
                    }
                    Row(Modifier.fillMaxSize()) {
                        slots.forEach { pageIndex ->
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                if (pageIndex in 0 until pageCount) {
                                    if (state.isText) {
                                        TextPageView(
                                            page = state.textPages[pageIndex],
                                            fontSizeSp = state.settings.fontSizeSp,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        val entry = state.imagePages[pageIndex].entry
                                        ZoomablePage(
                                            resetKey = "$entry:$pps:${state.page}",
                                            onZoomChanged = { zoomed ->
                                                if (zoomed) {
                                                    zoomedPage = pageIndex
                                                } else if (zoomedPage == pageIndex) {
                                                    zoomedPage = null
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            ComicPageView(
                                                entry = entry,
                                                pageNumber = pageIndex + 1,
                                                loader = { item, width, height ->
                                                    viewModel.loadBitmap(item, width, height)
                                                },
                                                strings = strings,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Color(0xFF3F5A87))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (state.paginating) {
                            strings.get(UiKey.RENDERING)
                        } else {
                            strings.get(UiKey.NO_CONTENT)
                        },
                        color = Color(0xFF5F5F66),
                    )
                }
            }

            if (appSettings.grainEnabled && appSettings.grainStrength > 0f) {
                PaperGrainOverlay(appSettings.grainStrength)
            }

            AnimatedVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ReaderTopBar(
                    title = state.book?.title ?: strings.get(UiKey.APP_NAME),
                    pageLabel = pageLabel(spreadStart, pageCount, pps, strings),
                    isBookmarked = currentBookmark != null,
                    onToggleBookmark = {
                        if (currentBookmark != null) {
                            viewModel.removeBookmark(currentBookmark.position)
                        } else {
                            viewModel.addBookmark(currentPosition)
                        }
                    },
                    onBack = onBack,
                    onSettings = { showSettings = true },
                    strings = strings,
                )
            }

            AnimatedVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                ReaderBottomBar(
                    page = spreadStart,
                    pageCount = pageCount,
                    pagesPerSpread = pps,
                    progress = readingProgress(state),
                    bookmarks = state.bookmarks,
                    total = if (state.isText) state.totalChars else pageCount,
                    bookmarkLabel = { bookmark ->
                        val page = if (state.isText) {
                            state.textPages.indexOfLast { it.absStart <= bookmark.position }
                                .coerceAtLeast(0) + 1
                        } else {
                            bookmark.position + 1
                        }
                        strings.get(UiKey.BOOKMARK_AT, page)
                    },
                    onBookmarkJump = { bookmark ->
                        viewModel.jumpToBookmark(bookmark.position)
                        chromeVisible = true
                    },
                    onJump = viewModel::jumpTo,
                    onPrevious = { viewModel.jumpTo((spreadStart - pps).coerceAtLeast(0)) },
                    onNext = { viewModel.jumpTo((spreadStart + pps).coerceAtMost(lastSpreadStart)) },
                    strings = strings,
                )
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            settings = state.settings,
            appSettings = appSettings,
            isTextBook = state.isText,
            effectiveDirection = state.direction,
            autoDirection = state.autoDirection,
            onDismiss = { showSettings = false },
            onDirection = viewModel::setDirection,
            onDualMode = viewModel::setDualMode,
            onFontSize = viewModel::setFontSize,
            onUpdateSettings = onUpdateSettings,
            strings = strings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    pageLabel: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    strings: UiStrings,
) {
    Surface(color = Color.White, shadowElevation = 3.dp) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pageLabel.isNotEmpty()) {
                        Text(
                            pageLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5F5F66),
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.get(UiKey.BACK_TO_LIBRARY),
                        tint = Color(0xFF1A1A1A),
                    )
                }
            },
            actions = {
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        painter = painterResource(
                            if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border,
                        ),
                        contentDescription = strings.get(
                            if (isBookmarked) UiKey.REMOVE_BOOKMARK else UiKey.ADD_BOOKMARK,
                        ),
                        tint = if (isBookmarked) Color(0xFF3F5A87) else Color(0xFF1A1A1A),
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = strings.get(UiKey.READING_SETTINGS),
                        tint = Color(0xFF1A1A1A),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color(0xFF1A1A1A),
                navigationIconContentColor = Color(0xFF1A1A1A),
                actionIconContentColor = Color(0xFF1A1A1A),
            ),
        )
    }
}

@Composable
private fun ReaderBottomBar(
    page: Int,
    pageCount: Int,
    pagesPerSpread: Int,
    progress: Float,
    bookmarks: List<Bookmark>,
    total: Int,
    bookmarkLabel: (Bookmark) -> String,
    onBookmarkJump: (Bookmark) -> Unit,
    onJump: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    strings: UiStrings,
) {
    var sliderValue by remember { mutableFloatStateOf(page.toFloat()) }
    LaunchedEffect(page) { sliderValue = page.toFloat() }
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = page > 0) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = strings.get(UiKey.PREVIOUS_PAGE),
                        tint = if (page > 0) Color(0xFF1A1A1A) else Color(0xFFBDBDBD),
                    )
                }
                Column(Modifier.weight(1f)) {
                    BookmarkTrack(
                        bookmarks = bookmarks,
                        total = total,
                        label = bookmarkLabel,
                        onJump = onBookmarkJump,
                    )
                    if (pageCount > 1) {
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { onJump(sliderValue.roundToInt()) },
                            valueRange = 0f..(pageCount - 1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3F5A87),
                                activeTrackColor = Color(0xFF3F5A87),
                                inactiveTrackColor = Color(0xFFD8DAE5),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                IconButton(
                    onClick = onNext,
                    enabled = page + pagesPerSpread < pageCount,
                ) {
                    val enabled = page + pagesPerSpread < pageCount
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = strings.get(UiKey.NEXT_PAGE),
                        tint = if (enabled) Color(0xFF1A1A1A) else Color(0xFFBDBDBD),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (pageCount > 0) {
                        strings.get(UiKey.PAGE_SHORT, page + 1, pageCount)
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5F5F66),
                )
                Text(
                    "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5F5F66),
                )
            }
        }
    }
}

@Composable
private fun BookmarkTrack(
    bookmarks: List<Bookmark>,
    total: Int,
    label: (Bookmark) -> String,
    onJump: (Bookmark) -> Unit,
) {
    if (bookmarks.isEmpty() || total <= 0) {
        Spacer(Modifier.height(4.dp))
        return
    }
    val primary = Color(0xFF3F5A87)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
            .padding(horizontal = 20.dp),
    ) {
        bookmarks.forEach { bookmark ->
            val fraction = (bookmark.position.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val description = label(bookmark)
            Box(
                Modifier
                    .offset(x = (maxWidth - 22.dp) * fraction)
                    .align(Alignment.BottomStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onJump(bookmark) }
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(primary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun TextPageView(
    page: TextPage,
    fontSizeSp: Int,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val headingSp = fontSizeSp * TextTypo.HEADING_SCALE
    Column(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.White)
            .padding(
                horizontal = TextTypo.HORIZONTAL_PADDING_DP.dp,
                vertical = TextTypo.VERTICAL_PADDING_DP.dp,
            ),
    ) {
        if (page.showTitle && !page.chapterTitle.isNullOrBlank()) {
            Text(
                page.chapterTitle,
                color = Color(0xFF141414),
                fontSize = headingSp.sp,
                lineHeight = (headingSp * TextTypo.HEADING_LINE_HEIGHT_MULT).sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = TextTypo.HEADING_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                Modifier.height(
                    with(density) { (fontSizeSp * TextTypo.HEADING_GAP_EM).sp.toDp() },
                ),
            )
        }
        Text(
            text = page.text.trim('\n'),
            color = Color(0xFF1A1A1A),
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * TextTypo.BODY_LINE_HEIGHT_MULT).sp,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data object Failure : ImageLoadState
    data class Success(val bitmap: Bitmap) : ImageLoadState
}

@Composable
private fun ComicPageView(
    entry: String,
    pageNumber: Int,
    loader: suspend (String, Int, Int) -> Bitmap?,
    strings: UiStrings,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.background(Color.White)) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        var attempt by remember(entry) { mutableIntStateOf(0) }
        val loadState by produceState<ImageLoadState>(
            initialValue = ImageLoadState.Loading,
            entry,
            width,
            height,
            attempt,
        ) {
            value = ImageLoadState.Loading
            val bitmap = loader(entry, width, height)
            value = if (bitmap != null) ImageLoadState.Success(bitmap) else ImageLoadState.Failure
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = loadState) {
                is ImageLoadState.Success -> Image(
                    bitmap = current.bitmap.asImageBitmap(),
                    contentDescription = strings.get(UiKey.PAGE_NUMBER, pageNumber),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize(),
                )
                is ImageLoadState.Failure -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        strings.get(UiKey.PAGE_LOAD_FAILED, pageNumber),
                        color = Color(0xFF6B6B72),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { attempt += 1 }) {
                        Text(strings.get(UiKey.RETRY_PAGE))
                    }
                }
                ImageLoadState.Loading -> CircularProgressIndicator(
                    color = Color(0xFF3F5A87),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    settings: com.kanae.foldpage.data.ReaderSettings,
    appSettings: AppSettings,
    isTextBook: Boolean,
    effectiveDirection: PageDirection,
    autoDirection: PageDirection,
    onDismiss: () -> Unit,
    onDirection: (DirectionSetting) -> Unit,
    onDualMode: (DualMode) -> Unit,
    onFontSize: (Int) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    strings: UiStrings,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
        ) {
            Text(strings.get(UiKey.READING_SETTINGS), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            SettingsLabel(strings.get(UiKey.READING_DIRECTION))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    DirectionSetting.AUTO to strings.get(UiKey.AUTO),
                    DirectionSetting.LTR to strings.get(UiKey.LEFT_TO_RIGHT),
                    DirectionSetting.RTL to strings.get(UiKey.RIGHT_TO_LEFT),
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.direction == option.first,
                        onClick = { onDirection(option.first) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(option.second, maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val directionText = strings.get(
                if (effectiveDirection == PageDirection.RTL) {
                    UiKey.DIRECTION_RTL_TEXT
                } else {
                    UiKey.DIRECTION_LTR_TEXT
                },
            )
            Text(
                if (settings.direction == DirectionSetting.AUTO) {
                    strings.get(UiKey.DIRECTION_AUTO_HINT, directionText)
                } else {
                    strings.get(UiKey.DIRECTION_MANUAL_HINT, directionText)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            SettingsLabel(strings.get(UiKey.PAGE_LAYOUT))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    DualMode.AUTO to strings.get(UiKey.AUTO),
                    DualMode.SINGLE to strings.get(UiKey.SINGLE_PAGE),
                    DualMode.DUAL to strings.get(UiKey.DOUBLE_PAGE),
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.dualMode == option.first,
                        onClick = { onDualMode(option.first) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(option.second, maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                strings.get(UiKey.LAYOUT_HINT),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isTextBook) {
                Spacer(Modifier.height(24.dp))
                SettingsLabel(strings.get(UiKey.FONT_SIZE))
                var fontValue by remember(settings.fontSizeSp) {
                    mutableFloatStateOf(settings.fontSizeSp.toFloat())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = fontValue,
                        onValueChange = { fontValue = it },
                        onValueChangeFinished = { onFontSize(fontValue.roundToInt()) },
                        valueRange = 14f..30f,
                        steps = 15,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    Text("A", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    strings.get(UiKey.FONT_CURRENT, fontValue.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(24.dp))
                Text(
                    strings.get(UiKey.IMAGE_BOOK_NOTE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsLabel(strings.get(UiKey.DISPLAY_EFFECTS))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(strings.get(UiKey.EYE_CARE), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        strings.get(UiKey.EYE_CARE_HINT),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = appSettings.eyeCareEnabled,
                    onCheckedChange = { checked -> onUpdateSettings { current -> current.copy(eyeCareEnabled = checked) } },
                )
            }
            if (appSettings.eyeCareEnabled) {
                Slider(
                    value = appSettings.eyeCareStrength,
                    onValueChange = { value -> onUpdateSettings { current -> current.copy(eyeCareStrength = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    strings.get(UiKey.STRENGTH, (appSettings.eyeCareStrength * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(strings.get(UiKey.PAPER_GRAIN), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        strings.get(UiKey.PAPER_GRAIN_HINT),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = appSettings.grainEnabled,
                    onCheckedChange = { checked -> onUpdateSettings { current -> current.copy(grainEnabled = checked) } },
                )
            }
            if (appSettings.grainEnabled) {
                Slider(
                    value = appSettings.grainStrength,
                    onValueChange = { value -> onUpdateSettings { current -> current.copy(grainStrength = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    strings.get(UiKey.STRENGTH, (appSettings.grainStrength * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                strings.get(if (isTextBook) UiKey.READER_GESTURE_HINT else UiKey.ZOOM_HINT),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(10.dp))
}

private fun pageLabel(page: Int, count: Int, pagesPerSpread: Int, strings: UiStrings): String {
    if (count <= 0) return ""
    val start = page + 1
    val end = (page + pagesPerSpread).coerceAtMost(count)
    return if (end > start) {
        strings.get(UiKey.PAGES_OF, start, end, count)
    } else {
        strings.get(UiKey.PAGE_OF, start, count)
    }
}

private fun readingProgress(state: ReaderUiState): Float {
    return if (state.isText) {
        if (state.totalChars <= 0) {
            0f
        } else {
            val offset = state.textPages.getOrNull(state.page)?.absStart ?: 0
            (offset.toFloat() / state.totalChars).coerceIn(0f, 1f)
        }
    } else {
        if (state.pageCount <= 0) 0f else (state.page.toFloat() / state.pageCount).coerceIn(0f, 1f)
    }
}
