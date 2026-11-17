package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } && currentRoute.startsWith("$screenRoute/")) return true
    if (screenRoute == "search_input" && (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true
    if (screenRoute == "reader" && currentRoute.startsWith("reader/")) return true
    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    NavigationRail(modifier = modifier, containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface) {
        Spacer(modifier = Modifier.weight(1f))
        navigationItems.forEach { screen ->
            val isSelected = isRouteSelected(currentRoute, screen.route, navigationItems)
            val currentIsSelected by rememberUpdatedState(isSelected)
            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> if (!isLongClick) onItemClick(screen, currentIsSelected)
                            is PressInteraction.Cancel -> isLongClick = false
                        }
                    }
                }
            }
            NavigationRailItem(
                selected = isSelected,
                onClick = { if (!isSearchItem && !isHomeHoldItem) onItemClick(screen, currentIsSelected) },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(if (isSelected) screen.iconIdActive else screen.iconIdInactive),
                        contentDescription = stringResource(screen.titleId),
                    )
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {},
    menuExpanded: Boolean = false,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
    glassStyle: Boolean = true,
    collapseFraction: Float = 0f,
    collapseOnScroll: Boolean = true,
    keepExpanded: Boolean = false,
    fluidMotion: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val surface = if (pureBlack) Color.Black else scheme.surface
    val dockColor = if (glassStyle) surface.copy(alpha = 0.78f) else scheme.surfaceContainer
    val contentColor = scheme.onSurface
    val outline = scheme.outline.copy(alpha = if (glassStyle) 0.42f else 0.28f)
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val latestItems = rememberUpdatedState(navigationItems)
    val latestRoute = rememberUpdatedState(currentRoute)
    val latestOnItemClick = rememberUpdatedState(onItemClick)
    var dragIndex by remember { mutableIntStateOf(-1) }
    val menuProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (menuExpanded) 1f else 0f,
        animationSpec = tween(180),
        label = "menuIconMorph",
    )
    val metrics = navigationDockMetrics(
        collapseFraction = if (collapseOnScroll) collapseFraction else 0f,
        compact = slimNav,
        keepExpanded = keepExpanded,
    )
    val dockHeight by animateDpAsState(metrics.heightDp.dp, label = "navigationDockHeight")
    val dockPadding by animateDpAsState(metrics.horizontalPaddingDp.dp, label = "navigationDockPadding")
    val labelAlpha by animateFloatAsState(metrics.labelAlpha, label = "navigationDockLabels")
    val menuSize by animateDpAsState(metrics.menuSizeDp.dp, label = "navigationDockMenu")
    val selectedIndex = remember(currentRoute, navigationItems) {
        navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }.coerceAtLeast(0)
    }

    Box(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .padding(start = dockPadding, end = dockPadding, bottom = bottomInset + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).height(dockHeight)
                    .shadow(18.dp, RoundedCornerShape(32.dp), clip = false)
                    .clip(RoundedCornerShape(32.dp))
                    .background(dockColor)
                    .border(1.dp, outline, RoundedCornerShape(32.dp))
                    .pointerInput(navigationItems) {
                        val itemCount = navigationItems.size.coerceAtLeast(1)
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var dragging = false
                            var lastIndex = -1
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y
                                    if (!dragging && dx * dx + dy * dy > viewConfiguration.touchSlop * viewConfiguration.touchSlop) {
                                        dragging = true
                                    }
                                    if (dragging) {
                                        val itemWidth = size.width.toFloat() / itemCount
                                        val index = (change.position.x / itemWidth).toInt().coerceIn(0, itemCount - 1)
                                        dragIndex = index
                                        if (index != lastIndex) {
                                            lastIndex = index
                                            val latest = latestItems.value
                                            val screen = latest[index]
                                            val selected = isRouteSelected(latestRoute.value, screen.route, latest)
                                            if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            latestOnItemClick.value(screen, selected)
                                        }
                                        change.consume()
                                    }
                                }
                            } finally {
                                dragIndex = -1
                            }
                        }
                    },
            ) {
                val itemCount = navigationItems.size.coerceAtLeast(1)
                val itemWidth = maxWidth / itemCount
                val indicatorOffset by animateDpAsState(itemWidth * selectedIndex, animationSpec = tween(180), label = "navigationSelection")
                Box(
                    modifier = Modifier.offset(x = indicatorOffset + 4.dp).align(Alignment.CenterStart)
                        .width(itemWidth - 8.dp).height((dockHeight - 8.dp).coerceAtLeast(40.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(scheme.primary.copy(alpha = if (glassStyle) 0.16f else 0.12f))
                        .border(1.dp, scheme.primary.copy(alpha = 0.28f), RoundedCornerShape(28.dp)),
                )
                if (glassStyle) GlassHighlight(Modifier.matchParentSize())
                Row(Modifier.fillMaxSize()) {
                    navigationItems.forEachIndexed { index, screen ->
                        val tabLabel = stringResource(screen.titleId)
                        val isSelected = isRouteSelected(currentRoute, screen.route, navigationItems)
                        val distanceFromFinger = if (dragIndex < 0) Int.MAX_VALUE else abs(index - dragIndex)
                        val scale by animateFloatAsState(
                            targetValue = if (!fluidMotion) 1f else when (distanceFromFinger) {
                                0 -> 1.18f
                                1 -> 1.06f
                                else -> 1f
                            },
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 520f),
                            label = "navTabMotion$index",
                        )
                        val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                        val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                        val interactionSource = remember { MutableInteractionSource() }
                        if (isSearchItem || isHomeHoldItem) {
                            LaunchedEffect(interactionSource) {
                                var isLongClick = false
                                interactionSource.interactions.collectLatest { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> {
                                            isLongClick = false
                                            delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                            isLongClick = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                        }
                                        is PressInteraction.Release -> if (!isLongClick) {
                                            if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onItemClick(screen, isSelected)
                                        }
                                        is PressInteraction.Cancel -> isLongClick = false
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clickable(interactionSource = interactionSource, indication = null) {
                                    if (!isSearchItem && !isHomeHoldItem) {
                                        if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onItemClick(screen, isSelected)
                                    }
                                }
                                .semantics {
                                    contentDescription = tabLabel
                                    role = Role.Tab
                                    selected = isSelected
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    painter = painterResource(if (isSelected) screen.iconIdActive else screen.iconIdInactive),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) scheme.primary else contentColor.copy(alpha = 0.82f),
                                )
                                Text(
                                    text = tabLabel,
                                    modifier = Modifier.graphicsLayer { alpha = labelAlpha },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) contentColor else contentColor.copy(alpha = 0.88f),
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.size(menuSize).shadow(18.dp, CircleShape, clip = false).clip(CircleShape)
                    .background(dockColor)
                    .border(1.dp, outline, CircleShape)
                    .clickable {
                        onMenuClick()
                    }
                    .semantics { contentDescription = "Menu" },
                contentAlignment = Alignment.Center,
            ) {
                if (glassStyle) GlassHighlight(Modifier.matchParentSize())
                Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = menuProgress * 90f }) {
                    val lineWidth = 2.2.dp.toPx()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val halfLength = 8.dp.toPx()
                    val gap = 6.dp.toPx()
                    drawLine(contentColor, Offset(centerX - halfLength, centerY - gap), Offset(centerX + halfLength, centerY - gap + 2f * gap * menuProgress), lineWidth, cap = StrokeCap.Round)
                    drawLine(contentColor, Offset(centerX - halfLength, centerY + gap), Offset(centerX + halfLength, centerY + gap - 2f * gap * menuProgress), lineWidth, cap = StrokeCap.Round)
                    drawLine(contentColor.copy(alpha = 1f - menuProgress), Offset(centerX - halfLength, centerY), Offset(centerX + halfLength, centerY), lineWidth, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun GlassHighlight(modifier: Modifier = Modifier) {
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val leftGlow = Offset(size.width * 0.16f, -size.height * 0.12f)
        val rightGlow = Offset(size.width * 0.88f, size.height * 0.2f)
        drawCircle(
            brush = Brush.radialGradient(listOf(highlightColor.copy(alpha = 0.16f), Color.Transparent), leftGlow, size.width * 0.78f),
            radius = size.width * 0.78f,
            center = leftGlow,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(accentColor.copy(alpha = 0.12f), Color.Transparent), rightGlow, size.width * 0.54f),
            radius = size.width * 0.54f,
            center = rightGlow,
        )
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, highlightColor.copy(alpha = 0.24f), Color.Transparent)),
            start = Offset(size.width * 0.12f, 1.dp.toPx()),
            end = Offset(size.width * 0.88f, 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
