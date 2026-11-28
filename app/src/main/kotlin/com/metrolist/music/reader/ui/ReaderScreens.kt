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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.metrolist.music.R
import com.metrolist.music.constants.ReaderBackgroundKey
import com.metrolist.music.constants.ReaderAutoAdvanceKey
import com.metrolist.music.constants.ReaderFullscreenKey
import com.metrolist.music.constants.ReaderKeepScreenOnKey
import com.metrolist.music.constants.ReaderLibraryLayoutKey
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
    var chapterStory by remember { mutableStateOf<ReaderStory?>(null) }
    var libraryLayout by rememberEnumPreference(ReaderLibraryLayoutKey, ReaderLibraryLayout.BLOCKS)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importUris(uris)
    }
    val stories = remember(books) {
        books.groupBy { it.storyId ?: it.id }.values.map { chapters ->
            val ordered = chapters.sortedByDescending { if (it.lastReadAt > 0L) it.lastReadAt else it.addedAt }
            val latest = ordered.first()
            ReaderStory(
                id = latest.storyId ?: latest.id,
                title = latest.storyTitle?.takeIf(String::isNotBlank) ?: latest.title.substringBefore(" — "),
                resumeBook = latest,
                chapters = ordered,
                coverUrl = ordered.mapNotNull { it.coverUrl }.firstOrNull(),
                lastReadAt = latest.lastReadAt.takeIf { it > 0L } ?: latest.addedAt,
            )
        }.sortedByDescending { it.lastReadAt }
    }
    Scaffold { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (libraryLayout == ReaderLibraryLayout.BLOCKS) 2 else 1),
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        ) {
            item(key = "reader-home-heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Your stories", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Pick up where you left off, or add a story to your library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { launcher.launch(arrayOf("application/zip", "application/x-cbz", "image/*")) },
                        ) {
                            Icon(painterResource(R.drawable.library_music_outlined), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import files", maxLines = 1)
                        }
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = { navController.navigate("reader/extensions") }) {
                            Icon(painterResource(R.drawable.manage_search), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse sources", maxLines = 1)
                        }
                    }
                }
            }
            if (stories.isEmpty()) {
                item(key = "reader-empty-state", span = { GridItemSpan(maxLineSpan) }) {
                    ReaderMessage(
                        "Your reader library is empty.",
                        Modifier.fillMaxWidth().height(220.dp),
                    )
                }
            } else {
                val featuredStory = stories.first()
                item(key = "reader-featured", span = { GridItemSpan(maxLineSpan) }) {
                    ReaderFeaturedStoryCard(
                        story = featuredStory,
                        onResume = { navController.navigate("reader/${featuredStory.resumeBook.id}") },
                        onOpenChapters = { chapterStory = featuredStory },
                    )
                }
                if (stories.size > 1) {
                    item(key = "reader-continue-rail", span = { GridItemSpan(maxLineSpan) }) {
                        ReaderStoryRail(
                            title = "Continue reading",
                            stories = stories.drop(1),
                            onOpenChapters = { chapterStory = it },
                        )
                    }
                }
                item(key = "reader-all-stories-heading", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth()) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("All stories", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            FilterChip(
                                selected = libraryLayout == ReaderLibraryLayout.BLOCKS,
                                onClick = { libraryLayout = ReaderLibraryLayout.BLOCKS },
                                label = { Text("Blocks") },
                            )
                            FilterChip(
                                selected = libraryLayout == ReaderLibraryLayout.BIG_TYPE,
                                onClick = { libraryLayout = ReaderLibraryLayout.BIG_TYPE },
                                label = { Text("Big type") },
                            )
                        }
                    }
                }
                gridItems(stories, key = { it.id }) { story ->
                    ReaderStoryCard(
                        story = story,
                        bigType = libraryLayout == ReaderLibraryLayout.BIG_TYPE,
                        onResume = { navController.navigate("reader/${story.resumeBook.id}") },
                        onOpenChapters = { chapterStory = story },
                        onDelete = { viewModel.deleteStory(story.chapters) },
                    )
                }
            }
        }
    }
    chapterStory?.let { story ->
        AlertDialog(
            onDismissRequest = { chapterStory = null },
            title = { Text(story.title) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    story.chapters.forEach { chapter ->
                        TextButton(
                            onClick = {
                                chapterStory = null
                                navController.navigate("reader/${chapter.id}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(chapter.chapterName ?: chapter.sourceName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Page ${chapter.currentPage + 1}${if (chapter.downloadedAt != null) " · Available offline" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { chapterStory = null }) { Text("Close") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(bookId: String, navController: NavController, viewModel: ReaderViewModel = hiltViewModel()) {
    val book by viewModel.book(bookId).collectAsState(initial = null)
    val pages by viewModel.pages(bookId).collectAsState(initial = emptyList())
    var readingMode by rememberEnumPreference(ReaderReadingModeKey, ReaderReadingMode.RIGHT_TO_LEFT)
    var scaleMode by rememberEnumPreference(ReaderScaleModeKey, ReaderScaleMode.FIT_SCREEN)
    var pageBackground by rememberEnumPreference(ReaderBackgroundKey, ReaderBackground.BLACK)
    var fullscreen by rememberPreference(ReaderFullscreenKey, false)
    var keepScreenOn by rememberPreference(ReaderKeepScreenOnKey, false)
    var autoAdvance by rememberPreference(ReaderAutoAdvanceKey, false)
    var showControls by remember(bookId) { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var currentPage by remember(bookId) { mutableIntStateOf(0) }
    var pageInitialized by remember(bookId) { mutableStateOf(false) }
    var chapterActionRunning by remember(bookId) { mutableStateOf(false) }
    var chapterOptions by remember(bookId) { mutableStateOf(emptyList<ReaderChapterOption>()) }
    var chapterOptionsLoading by remember(bookId) { mutableStateOf(false) }
    var showChapterPicker by remember(bookId) { mutableStateOf(false) }
    var downloadRunning by remember(bookId) { mutableStateOf(false) }
    var autoAdvanceArmed by remember(bookId) { mutableStateOf(false) }
    val snackbarHostState = remember(bookId) { SnackbarHostState() }

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

    LaunchedEffect(book?.id, book?.storyId) {
        val loadedBook = book
        if (loadedBook?.storyId == null) {
            chapterOptions = emptyList()
            chapterOptionsLoading = false
        } else {
            chapterOptionsLoading = true
            chapterOptions = try {
                viewModel.chapterOptions(loadedBook.id)
            } catch (_: Exception) {
                emptyList()
            }
            chapterOptionsLoading = false
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

    fun loadNextChapter() {
        if (chapterActionRunning) return
        chapterActionRunning = true
        coroutineScope.launch {
            try {
                val nextId = viewModel.importNextChapter(bookId)
                if (nextId == null) {
                    snackbarHostState.showSnackbar("No next chapter is available from this source.")
                } else {
                    navController.navigate("reader/$nextId") { launchSingleTop = true }
                }
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Couldn't load the next chapter. Check the source and connection.")
            } finally {
                chapterActionRunning = false
            }
        }
    }

    fun openChapterOption(option: ReaderChapterOption) {
        if (chapterActionRunning) return
        showChapterPicker = false
        val savedBookId = option.bookId
        if (savedBookId != null) {
            if (savedBookId != bookId) {
                navController.navigate("reader/$savedBookId") { launchSingleTop = true }
            }
            return
        }
        chapterActionRunning = true
        coroutineScope.launch {
            try {
                val selectedBookId = viewModel.openChapter(bookId, option.url)
                if (selectedBookId == null) {
                    snackbarHostState.showSnackbar("That chapter is no longer available from this source.")
                } else {
                    navController.navigate("reader/$selectedBookId") { launchSingleTop = true }
                }
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Couldn't load that chapter. Check the source and connection.")
            } finally {
                chapterActionRunning = false
            }
        }
    }

    fun switchChapter(offset: Int) {
        val index = chapterOptions.indexOfFirst { it.bookId == bookId || it.url == book?.chapterUrl }
        val target = chapterOptions.getOrNull(index + offset)
        if (index >= 0 && target != null) {
            openChapterOption(target)
        } else if (offset > 0 && book?.storyId != null) {
            loadNextChapter()
        }
    }

    fun saveChapterOffline() {
        if (downloadRunning || book?.downloadedAt != null) return
        downloadRunning = true
        coroutineScope.launch {
            try {
                viewModel.downloadChapter(bookId)
                snackbarHostState.showSnackbar("Chapter saved for offline reading.")
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Couldn't download this chapter. Check the connection and try again.")
            } finally {
                downloadRunning = false
            }
        }
    }

    val pageCount = pages.size
    val displayedPage = if (pageCount == 0) 0 else currentPage.coerceIn(0, pageCount - 1)
    val chapterIndex = chapterOptions.indexOfFirst { it.bookId == bookId || it.url == book?.chapterUrl }
    LaunchedEffect(pageInitialized, displayedPage, pageCount, autoAdvance, book?.storyId) {
        if (!pageInitialized || pageCount == 0 || book?.storyId == null) return@LaunchedEffect
        if (displayedPage < pageCount - 1) {
            autoAdvanceArmed = true
        } else if (autoAdvance && autoAdvanceArmed && !chapterActionRunning) {
            loadNextChapter()
        }
    }
    Scaffold(
        containerColor = pageBackground.color,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showControls) {
                TopAppBar(
                    title = {
                        val canSelectChapter = book?.storyId != null
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (canSelectChapter) 0.72f else 0f))
                                .clickable(enabled = canSelectChapter, role = Role.Button) { showChapterPicker = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(book?.storyTitle ?: book?.title ?: "Reader", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val subtitle = listOfNotNull(
                                    book?.chapterName,
                                    if (pageCount > 0) "${displayedPage + 1} / $pageCount · ${readingMode.title}" else null,
                                    if (book?.downloadedAt != null) "Available offline" else null,
                                ).joinToString(" · ")
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (canSelectChapter) {
                                Icon(painterResource(R.drawable.expand_more), contentDescription = "Choose chapter", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = book != null && pages.isNotEmpty() && book?.downloadedAt == null && !downloadRunning,
                            onClick = ::saveChapterOffline,
                        ) {
                            Icon(
                                painterResource(if (book?.downloadedAt != null) R.drawable.library_add_check else R.drawable.download),
                                contentDescription = when {
                                    book?.downloadedAt != null -> "Chapter available offline"
                                    downloadRunning -> "Saving chapter offline"
                                    else -> "Save chapter offline"
                                },
                            )
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(painterResource(R.drawable.settings), contentDescription = "Reader settings")
                        }
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
                                    referer = book?.referer,
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
                                referer = book?.referer,
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
                                referer = book?.referer,
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
                        canLoadNextChapter = book?.storyId != null,
                        chapterLoading = chapterActionRunning,
                        chapterIndex = chapterIndex,
                        chapterCount = chapterOptions.size,
                        onOpenChapterPicker = { showChapterPicker = true },
                        onPreviousChapter = { switchChapter(-1) },
                        onNextChapter = { switchChapter(1) },
                    )
                }
            }
        }
    }

    if (showChapterPicker) {
        ModalBottomSheet(onDismissRequest = { showChapterPicker = false }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "reader-chapter-picker-heading") {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Chapters", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            book?.storyTitle ?: "Available chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (chapterOptionsLoading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else {
                            Text(
                                "Select a saved chapter to resume, or choose another chapter to load it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                if (chapterOptions.isEmpty() && !chapterOptionsLoading) {
                    item(key = "reader-no-chapters") {
                        ReaderMessage("No chapters are available for this story.", Modifier.fillMaxWidth().height(120.dp))
                    }
                }
                items(chapterOptions, key = { it.url }) { option ->
                    val isCurrent = option.bookId == bookId || option.url == book?.chapterUrl
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            )
                            .clickable(enabled = !chapterActionRunning, role = Role.Button) { openChapterOption(option) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(option.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    option.bookId == null -> "Tap to load from source"
                                    option.downloaded -> "Available offline · page ${option.currentPage + 1}"
                                    else -> "Saved · page ${option.currentPage + 1}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when {
                            isCurrent -> Icon(painterResource(R.drawable.check), contentDescription = "Current chapter", tint = MaterialTheme.colorScheme.primary)
                            option.downloaded -> Text("Offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
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
            autoAdvance = autoAdvance,
            onAutoAdvanceChange = { autoAdvance = it },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun ReaderPageImage(
    page: com.metrolist.music.reader.db.ReaderPageEntity,
    referer: String?,
    pageNumber: Int,
    mode: ReaderReadingMode,
    scaleMode: ReaderScaleMode,
    modifier: Modifier,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleControls: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnToggle by rememberUpdatedState(onToggleControls)
    val request = remember(page.filePath, referer) {
        ImageRequest.Builder(context)
            .data(page.filePath)
            .apply {
                if (!referer.isNullOrBlank() && (page.filePath.startsWith("http://") || page.filePath.startsWith("https://"))) {
                    httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
                }
            }
            .build()
    }
    val pageImagePainter = rememberAsyncImagePainter(model = request)
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
    canLoadNextChapter: Boolean,
    chapterLoading: Boolean,
    chapterIndex: Int,
    chapterCount: Int,
    onOpenChapterPicker: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
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
        if (canLoadNextChapter) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(enabled = chapterIndex > 0 && !chapterLoading, onClick = onPreviousChapter) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prev")
                }
                TextButton(onClick = onOpenChapterPicker) {
                    Text(if (chapterIndex >= 0 && chapterCount > 0) "Ch. ${chapterIndex + 1} / $chapterCount" else "Chapters")
                    Icon(painterResource(R.drawable.expand_more), contentDescription = null, modifier = Modifier.size(16.dp))
                }
                TextButton(
                    enabled = !chapterLoading &&
                        ((chapterIndex < 0 && canLoadNextChapter) || (chapterIndex >= 0 && chapterIndex < chapterCount - 1)),
                    onClick = onNextChapter,
                ) {
                    Text(if (chapterLoading) "Loading…" else "Next")
                    Spacer(Modifier.width(4.dp))
                    Icon(painterResource(R.drawable.arrow_forward), contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
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
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
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
                ReaderSwitchRow("Automatically open the next chapter", autoAdvance, onAutoAdvanceChange)
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


private data class ReaderStory(
    val id: String,
    val title: String,
    val resumeBook: com.metrolist.music.reader.db.ReaderBookEntity,
    val chapters: List<com.metrolist.music.reader.db.ReaderBookEntity>,
    val coverUrl: String?,
    val lastReadAt: Long,
)

@Composable
private fun ReaderFeaturedStoryCard(
    story: ReaderStory,
    onResume: () -> Unit,
    onOpenChapters: () -> Unit,
) {
    val context = LocalContext.current
    val coverRequest = remember(story.coverUrl, story.resumeBook.referer) {
        ImageRequest.Builder(context).data(story.coverUrl).apply {
            val referer = story.resumeBook.referer
            if (!referer.isNullOrBlank() && story.coverUrl?.startsWith("http") == true) {
                httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
            }
        }.build()
    }
    Box(Modifier.fillMaxWidth().height(316.dp).clip(RoundedCornerShape(24.dp))) {
        ReaderStoryCover(coverRequest, Modifier.fillMaxSize(), story.title)
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("CONTINUE READING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(story.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${story.resumeBook.chapterName ?: story.resumeBook.sourceName} · Page ${story.resumeBook.currentPage + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text("Resume") }
                OutlinedButton(onClick = onOpenChapters, modifier = Modifier.weight(1f)) { Text("Chapters") }
            }
        }
    }
}

@Composable
private fun ReaderStoryRail(
    title: String,
    stories: List<ReaderStory>,
    onOpenChapters: (ReaderStory) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp),
        ) {
            items(stories, key = { it.id }) { story ->
                val context = LocalContext.current
                val coverRequest = remember(story.coverUrl, story.resumeBook.referer) {
                    ImageRequest.Builder(context).data(story.coverUrl).apply {
                        val referer = story.resumeBook.referer
                        if (!referer.isNullOrBlank() && story.coverUrl?.startsWith("http") == true) {
                            httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
                        }
                    }.build()
                }
                Column(
                    Modifier.width(116.dp).clickable(onClick = { onOpenChapters(story) }),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ReaderStoryCover(
                        coverRequest,
                        Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                        story.title,
                    )
                    Text(story.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${story.chapters.size} chapter${if (story.chapters.size == 1) "" else "s"} · page ${story.resumeBook.currentPage + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderStoryCard(
    story: ReaderStory,
    bigType: Boolean,
    onResume: () -> Unit,
    onOpenChapters: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val coverRequest = remember(story.coverUrl, story.resumeBook.referer) {
        ImageRequest.Builder(context).data(story.coverUrl).apply {
            val referer = story.resumeBook.referer
            if (!referer.isNullOrBlank() && story.coverUrl?.startsWith("http") == true) {
                httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
            }
        }.build()
    }
    Card(
        Modifier.fillMaxWidth()
            .then(if (!bigType) Modifier.fillMaxHeight() else Modifier)
            .clickable(onClick = onOpenChapters),
    ) {
        if (bigType) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ReaderStoryCover(coverRequest, Modifier.size(width = 92.dp, height = 128.dp), story.title)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(story.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(
                        story.resumeBook.chapterName ?: story.resumeBook.sourceName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${story.chapters.size} chapter${if (story.chapters.size == 1) "" else "s"} · resume at page ${story.resumeBook.currentPage + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (story.resumeBook.downloadedAt != null) {
                        Text("Available offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text("Resume") }
                        IconButton(onClick = onDelete) {
                            Icon(painterResource(R.drawable.delete), contentDescription = "Remove story")
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                ReaderStoryCover(coverRequest, Modifier.fillMaxWidth().height(132.dp), story.title)
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        story.resumeBook.chapterName ?: story.resumeBook.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Page ${story.resumeBook.currentPage + 1} · ${story.chapters.size} chapter${if (story.chapters.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (story.resumeBook.downloadedAt != null) {
                        Text("Available offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text("Resume") }
                        IconButton(onClick = onDelete) {
                            Icon(painterResource(R.drawable.delete), contentDescription = "Remove story")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderStoryCover(request: ImageRequest, modifier: Modifier, title: String) {
    if (request.data == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.library_music_outlined),
                contentDescription = "$title cover",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
    } else {
        AsyncImage(
            model = request,
            contentDescription = "$title cover",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = modifier,
        )
    }
}
