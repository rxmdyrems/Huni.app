package com.metrolist.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import kotlinx.coroutines.launch

internal data class DashboardMenuItem(
    val label: Int,
    val route: String,
    val icon: Int,
)

internal val dashboardMenuItems = listOf(
    DashboardMenuItem(R.string.settings, "settings", R.drawable.settings),
    DashboardMenuItem(R.string.music_dashboard, "home", R.drawable.library_music_outlined),
    DashboardMenuItem(R.string.anime_dashboard, "anime", R.drawable.slow_motion_video),
    DashboardMenuItem(R.string.manga_dashboard, "manga_dashboard", R.drawable.library_add),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardMenuSheet(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50),
            ) { Spacer(Modifier.size(width = 34.dp, height = 4.dp)) }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.dashboard_choose), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            dashboardMenuItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                    onSelect(item.route)
                                }
                            },
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(stringResource(item.label), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
