/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        if (moodAndGenresList == null) {
            item(key = "mood_and_genres_shimmer") {
                ShimmerHost(
                    modifier = Modifier.animateItem()
                ) {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEachIndexed { index, moodAndGenres ->
            item(key = "mood_and_genres_section_$index") {
                Column(
                    modifier = Modifier
                        .animateItem(),
                ) {
                    NavigationTitle(
                        title = moodAndGenres.title,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        moodAndGenres.items.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { item ->
                                    MoodAndGenresButton(
                                        title = item.title,
                                        onClick = {
                                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.mood_and_genres), maxLines = 1, softWrap = false) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
fun MoodAndGenresButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
        modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

val MoodAndGenresButtonHeight = 48.dp
