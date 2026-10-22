package com.kanae.foldpage.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
import com.kanae.foldpage.FoldPageApplication
import com.kanae.foldpage.data.DirectionSetting
import com.kanae.foldpage.data.DualMode
import com.kanae.foldpage.data.PageDirection
import com.kanae.foldpage.reader.TextPage
import com.kanae.foldpage.reader.TextTypo
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    bookId: String,
    session: Int,
    onBack: () -> Unit,
    onBookFinished: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as FoldPageApplication
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
        state.loading -> ReaderLoading()
        state.error != null -> ReaderError(
            message = state.error.orEmpty(),
            onRetry = viewModel::load,
            onBack = onBack,
        )
        else -> ReaderContent(state = state, viewModel = viewModel, onBack = onBack)
    }
}

@Composable
private fun ReaderLoading() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF3F5A87))
            Spacer(Modifier.height(16.dp))
            Text("正在載入書籍…", color = Color(0xFF5F5F66))
        }
    }
}

@Composable
private fun ReaderError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
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
                "無法開啟這本書",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFF5F5F66))
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text("返回書庫") }
                Button(onClick = onRetry) { Text("再試一次") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
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
        val spreadStart = if (pageCount <= 0) 0 else (state.page.coerceIn(0, pageCount - 1) / pps) * pps

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(pageCount, spreadStart, rtl, pps) {
                    detectTapGestures { offset ->
                        if (pageCount <= 0) return@detectTapGestures
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
                                        ComicPageView(
                                            entry = state.imagePages[pageIndex].entry,
                                            pageNumber = pageIndex + 1,
                                            loader = { entry, width, height ->
                                                viewModel.loadBitmap(entry, width, height)
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                        )
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
                        if (state.paginating) "正在排版…" else "沒有可顯示的內容",
                        color = Color(0xFF5F5F66),
                    )
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ReaderTopBar(
                    title = state.book?.title ?: "閱讀中",
                    pageLabel = pageLabel(spreadStart, pageCount, pps),
                    onBack = onBack,
                    onSettings = { showSettings = true },
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
                    onJump = viewModel::jumpTo,
                    onPrevious = { viewModel.jumpTo((spreadStart - pps).coerceAtLeast(0)) },
                    onNext = { viewModel.jumpTo((spreadStart + pps).coerceAtMost(lastSpreadStart)) },
                )
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            settings = state.settings,
            isTextBook = state.isText,
            effectiveDirection = state.direction,
            autoDirection = state.autoDirection,
            onDismiss = { showSettings = false },
            onDirection = viewModel::setDirection,
            onDualMode = viewModel::setDualMode,
            onFontSize = viewModel::setFontSize,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    pageLabel: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
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
                        contentDescription = "返回書庫",
                        tint = Color(0xFF1A1A1A),
                    )
                }
            },
            actions = {
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "閱讀設定",
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
    onJump: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
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
                        contentDescription = "上一頁",
                        tint = if (page > 0) Color(0xFF1A1A1A) else Color(0xFFBDBDBD),
                    )
                }
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
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = onNext,
                    enabled = page + pagesPerSpread < pageCount,
                ) {
                    val enabled = page + pagesPerSpread < pageCount
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "下一頁",
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
                    if (pageCount > 0) "${page + 1} / $pageCount" else "",
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
                    contentDescription = "第 $pageNumber 頁",
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize(),
                )
                is ImageLoadState.Failure -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "第 $pageNumber 頁載入失敗",
                        color = Color(0xFF6B6B72),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { attempt += 1 }) { Text("重試") }
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
    isTextBook: Boolean,
    effectiveDirection: PageDirection,
    autoDirection: PageDirection,
    onDismiss: () -> Unit,
    onDirection: (DirectionSetting) -> Unit,
    onDualMode: (DualMode) -> Unit,
    onFontSize: (Int) -> Unit,
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
            Text("閱讀設定", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            SettingsLabel("閱讀方向")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    DirectionSetting.AUTO to "自動",
                    DirectionSetting.LTR to "左至右",
                    DirectionSetting.RTL to "右至左",
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
            Text(
                if (settings.direction == DirectionSetting.AUTO) {
                    "跟隨檔案標示：目前${if (effectiveDirection == PageDirection.RTL) "由右至左" else "由左至右"}" +
                        if (autoDirection == PageDirection.RTL) "（EPUB 標示為右至左）" else ""
                } else {
                    "目前${if (effectiveDirection == PageDirection.RTL) "由右至左" else "由左至右"}翻頁"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            SettingsLabel("翻頁版面")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    DualMode.AUTO to "自動",
                    DualMode.SINGLE to "單頁",
                    DualMode.DUAL to "雙頁",
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
                "自動：直向顯示完整一頁；橫向或展開摺疊屏時左右各一頁，留白以白色填充。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isTextBook) {
                Spacer(Modifier.height(24.dp))
                SettingsLabel("字體大小")
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
                    "目前 ${fontValue.roundToInt()} sp；調整後會重新排版。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(24.dp))
                Text(
                    "這是圖像書，字體大小不影響頁面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

private fun pageLabel(page: Int, count: Int, pagesPerSpread: Int): String {
    if (count <= 0) return ""
    val start = page + 1
    val end = (page + pagesPerSpread).coerceAtMost(count)
    return if (end > start) "第 $start–$end 頁 / 共 $count 頁" else "第 $start 頁 / 共 $count 頁"
}

private fun readingProgress(state: ReaderUiState): Float {
    return if (state.isText) {
        if (state.totalChars <= 0) 0f else {
            val offset = state.textPages.getOrNull(state.page)?.absStart ?: 0
            (offset.toFloat() / state.totalChars).coerceIn(0f, 1f)
        }
    } else {
        if (state.pageCount <= 0) 0f else (state.page.toFloat() / state.pageCount).coerceIn(0f, 1f)
    }
}
