package com.metrolist.music.reader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.metrolist.music.R
import com.metrolist.music.constants.ReaderBackgroundKey
import com.metrolist.music.constants.ReaderFullscreenKey
import com.metrolist.music.constants.ReaderKeepScreenOnKey
import com.metrolist.music.constants.ReaderReadingModeKey
import com.metrolist.music.constants.ReaderScaleModeKey
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReaderLibraryScreen(navController: NavController, viewModel: ReaderViewModel = hiltViewModel()) {
    val books by viewModel.books.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importUris(uris)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Reader") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Text("Local manga and stories", style = MaterialTheme.typography.headlineSmall)
            Text("Import CBZ, ZIP, or image files. Everything stays on this device.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))

            Button(onClick = { launcher.launch(arrayOf("application/zip", "application/x-cbz", "image/*")) }) {
                Icon(painterResource(R.drawable.library_music_outlined), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import files")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { navController.navigate("reader/extensions") }) {
                Text("Extensions")
            }
            Spacer(Modifier.height(16.dp))
            if (books.isEmpty()) {
                Text("Your reader library is empty.", modifier = Modifier.padding(top = 24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(books, key = { it.id }) { book ->
                        Card(Modifier.fillMaxWidth().clickable { navController.navigate("reader/${book.id}") }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.library_music_outlined), contentDescription = null, modifier = Modifier.size(40.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(book.title, style = MaterialTheme.typography.titleMedium)
                                    Text("Continue from page ${book.currentPage + 1}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.deleteBook(book) }) {
                                    Icon(painterResource(R.drawable.delete), contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(bookId: String, navController: NavController, viewModel: ReaderViewModel = hiltViewModel()) {
    val book by viewModel.book(bookId).collectAsState(initial = null)
    val pages by viewModel.pages(bookId).collectAsState(initial = emptyList())
    var readingMode by rememberEnumPreference(ReaderReadingModeKey, ReaderReadingMode.RIGHT_TO_LEFT)
    var scaleMode by rememberEnumPreference(ReaderScaleModeKey, ReaderScaleMode.FIT_SCREEN)
    var pageBackground by rememberEnumPreference(ReaderBackgroundKey, ReaderBackground.BLACK)
    var fullscreen by rememberPreference(ReaderFullscreenKey, false)
    var keepScreenOn by rememberPreference(ReaderKeepScreenOnKey, false)
    var showControls by remember(bookId) { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var currentPage by remember(bookId) { mutableIntStateOf(0) }
    var pageInitialized by remember(bookId) { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity, fullscreen, keepScreenOn) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val hadKeepScreenOn = window?.let { it.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0 } ?: false
        if (fullscreen) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        if (keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (hadKeepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(book?.id, pages.size) {
        val loadedBook = book
        if (!pageInitialized && loadedBook != null && pages.isNotEmpty()) {
            currentPage = loadedBook.currentPage.coerceIn(0, pages.lastIndex)
            pageInitialized = true
        }
    }

    val initialPage = currentPage.coerceIn(0, maxOf(pages.lastIndex, 0))
    val pagerState = key(bookId, readingMode) {
        rememberPagerState(initialPage = initialPage, pageCount = { pages.size.coerceAtLeast(1) })
    }
    val listState = key(bookId, readingMode) {
        rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pageInitialized, readingMode, pages.size, pagerState, listState) {
        if (!pageInitialized || pages.isEmpty()) return@LaunchedEffect
        val startPage = currentPage.coerceIn(0, pages.lastIndex)
        if (readingMode.isContinuous) listState.scrollToItem(startPage)
        else pagerState.scrollToPage(startPage)

        val visiblePage = if (readingMode.isContinuous) {
            snapshotFlow { listState.firstVisibleItemIndex }
        } else {
            snapshotFlow { pagerState.currentPage }
        }
        visiblePage.distinctUntilChanged().collect { pageIndex ->
            val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
            currentPage = safeIndex
            viewModel.updateProgress(bookId, safeIndex)
        }
    }

    fun goToPage(targetPage: Int) {
        if (pages.isEmpty()) return
        val page = targetPage.coerceIn(0, pages.lastIndex)
        coroutineScope.launch {
            if (readingMode.isContinuous) listState.animateScrollToItem(page)
            else pagerState.animateScrollToPage(page)
        }
    }

    val pageCount = pages.size
    val displayedPage = if (pageCount == 0) 0 else currentPage.coerceIn(0, pageCount - 1)
    Scaffold(
        containerColor = pageBackground.color,
        topBar = {
            if (showControls) {
                TopAppBar(
                    title = {
                        Column {
                            Text(book?.title ?: "Reader", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (pageCount > 0) {
                                Text("${displayedPage + 1} / $pageCount · ${readingMode.title}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showSettings = true }) { Text("Reader settings") }
                    },
                )
            }
        },
    ) { padding ->
        when {
            book == null -> ReaderMessage("Loading reader…", Modifier.fillMaxSize().padding(padding))
            pages.isEmpty() -> ReaderMessage("No readable pages found.", Modifier.fillMaxSize().padding(padding))
            !pageInitialized -> ReaderMessage("Restoring reading position…", Modifier.fillMaxSize().padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(pageBackground.color),
                ) {
                    if (readingMode.isContinuous) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(readingMode.pageGapDp.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = readingMode.pageGapDp.dp),
                        ) {
                            itemsIndexed(pages, key = { _, page -> page.filePath }) { index, page ->
                                ReaderPageImage(
                                    page = page,
                                    pageNumber = index + 1,
                                    mode = readingMode,
                                    scaleMode = scaleMode,
                                    modifier = Modifier.fillMaxWidth().background(pageBackground.color),
                                    onNext = { goToPage(index + 1) },
                                    onPrevious = { goToPage(index - 1) },
                                    onToggleControls = { showControls = !showControls },
                                )
                            }
                        }
                    } else if (readingMode.isVerticalPager) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = 0.dp,
                        ) { index ->
                            ReaderPageImage(
                                page = pages[index],
                                pageNumber = index + 1,
                                mode = readingMode,
                                scaleMode = scaleMode,
                                modifier = Modifier.fillMaxSize().background(pageBackground.color),
                                onNext = { goToPage(index + 1) },
                                onPrevious = { goToPage(index - 1) },
                                onToggleControls = { showControls = !showControls },
                            )
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = readingMode.isRightToLeft,
                            pageSpacing = 0.dp,
                        ) { index ->
                            ReaderPageImage(
                                page = pages[index],
                                pageNumber = index + 1,
                                mode = readingMode,
                                scaleMode = scaleMode,
                                modifier = Modifier.fillMaxSize().background(pageBackground.color),
                                onNext = { goToPage(index + 1) },
                                onPrevious = { goToPage(index - 1) },
                                onToggleControls = { showControls = !showControls },
                            )
                        }
                    }
                }
                if (showControls) {
                    ReaderNavigationControls(
                        currentPage = displayedPage,
                        pageCount = pageCount,
                        onPrevious = { goToPage(displayedPage - 1) },
                        onNext = { goToPage(displayedPage + 1) },
                        onSeek = ::goToPage,
                    )
                }
            }
        }
    }

    if (showSettings) {
        ReaderSettingsDialog(
            readingMode = readingMode,
            onReadingModeChange = { readingMode = it },
            scaleMode = scaleMode,
            onScaleModeChange = { scaleMode = it },
            background = pageBackground,
            onBackgroundChange = { pageBackground = it },
            fullscreen = fullscreen,
            onFullscreenChange = { fullscreen = it },
            keepScreenOn = keepScreenOn,
            onKeepScreenOnChange = { keepScreenOn = it },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun ReaderPageImage(
    page: com.metrolist.music.reader.db.ReaderPageEntity,
    pageNumber: Int,
    mode: ReaderReadingMode,
    scaleMode: ReaderScaleMode,
    modifier: Modifier,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleControls: () -> Unit,
) {
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnToggle by rememberUpdatedState(onToggleControls)
    val pageImagePainter = rememberAsyncImagePainter(model = page.filePath)
    val pageImageState by pageImagePainter.state.collectAsState()
    Box(modifier.pointerInput(mode) {
        detectTapGestures { tap ->
            if (mode.isContinuous) {
                currentOnToggle()
            } else {
                val centerX = size.width * 0.35f..size.width * 0.65f
                val centerY = size.height * 0.35f..size.height * 0.65f
                if (tap.x in centerX && tap.y in centerY) {
                    currentOnToggle()
                } else {
                    val next = when (mode) {
                        ReaderReadingMode.RIGHT_TO_LEFT -> tap.x > size.width / 2f
                        ReaderReadingMode.LEFT_TO_RIGHT -> tap.x < size.width / 2f
                        ReaderReadingMode.VERTICAL -> tap.y > size.height / 2f
                        else -> false
                    }
                    if (next) currentOnNext() else currentOnPrevious()
                }
            }
        }
    }) {
        Image(
            painter = pageImagePainter,
            contentDescription = "Page $pageNumber",
            contentScale = if (mode.isContinuous) androidx.compose.ui.layout.ContentScale.FillWidth else scaleMode.contentScale(),
            modifier = Modifier.fillMaxSize(),
        )
        if (pageImageState is AsyncImagePainter.State.Error) {
            Text(
                "Couldn't load this page. Check the connection or source image URL.",
                Modifier.align(Alignment.Center).padding(20.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ReaderNavigationControls(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(enabled = currentPage > 0, onClick = onPrevious) { Text("Previous") }
            Text("${currentPage + 1} / $pageCount", style = MaterialTheme.typography.labelLarge)
            TextButton(enabled = currentPage < pageCount - 1, onClick = onNext) { Text("Next") }
        }
        if (pageCount > 1) {
            var sliderPosition by remember(currentPage, pageCount) { mutableFloatStateOf(currentPage.toFloat()) }
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = { onSeek(sliderPosition.roundToInt()) },
                valueRange = 0f..(pageCount - 1).toFloat(),
            )
        }
    }
}

@Composable
private fun ReaderSettingsDialog(
    readingMode: ReaderReadingMode,
    onReadingModeChange: (ReaderReadingMode) -> Unit,
    scaleMode: ReaderScaleMode,
    onScaleModeChange: (ReaderScaleMode) -> Unit,
    background: ReaderBackground,
    onBackgroundChange: (ReaderBackground) -> Unit,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reader settings") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Reading mode", style = MaterialTheme.typography.titleSmall)
                ReaderReadingMode.entries.forEach { mode ->
                    ReaderChoiceRow(mode.title, readingMode == mode) { onReadingModeChange(mode) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Image scaling", style = MaterialTheme.typography.titleSmall)
                ReaderScaleMode.entries.forEach { scale ->
                    ReaderChoiceRow(scale.title, scaleMode == scale) { onScaleModeChange(scale) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Page background", style = MaterialTheme.typography.titleSmall)
                ReaderBackground.entries.forEach { color ->
                    ReaderChoiceRow(color.title, background == color) { onBackgroundChange(color) }
                }
                Spacer(Modifier.height(8.dp))
                ReaderSwitchRow("Fullscreen (hide system bars)", fullscreen, onFullscreenChange)
                ReaderSwitchRow("Keep screen awake", keepScreenOn, onKeepScreenOnChange)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ReaderChoiceRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReaderSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReaderMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(message)
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
