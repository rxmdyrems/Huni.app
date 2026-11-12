package com.metrolist.music.reader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.metrolist.music.R
import com.metrolist.music.reader.db.ReaderBookEntity

@Composable
fun ReaderLibraryScreen(navController: NavController, viewModel: ReaderViewModel = hiltViewModel()) {
    val books by viewModel.books.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        viewModel.importUris(uris)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Reader") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Text("Local manga and stories", style = MaterialTheme.typography.headlineSmall)
            Text("Import CBZ, ZIP, or image files. Everything stays on this device.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { launcher.launch(arrayOf("application/zip", "application/x-cbz", "image/*")) }) {
                Icon(painterResource(R.drawable.library_music_outlined), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import files")
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
    var page by remember(book?.currentPage) { mutableIntStateOf(book?.currentPage ?: 0) }
    LaunchedEffect(page) { if (pages.isNotEmpty()) viewModel.updateProgress(bookId, page) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(book?.title ?: "Reader") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back") } },
        )
    }) { padding ->
        if (pages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No readable pages found.") }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    AsyncImage(model = pages[page].filePath, contentDescription = "Page ${page + 1}", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Button(enabled = page > 0, onClick = { page-- }) { Text("Previous") }
                    Text("${page + 1} / ${pages.size}")
                    Button(enabled = page < pages.lastIndex, onClick = { page++ }) { Text("Next") }
                }
            }
        }
    }
}
