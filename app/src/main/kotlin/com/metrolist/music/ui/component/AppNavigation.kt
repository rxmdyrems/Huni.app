/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

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
    val containerColor = MaterialTheme.colorScheme.surface
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
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
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
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
) {
    val containerColor = if (glassStyle) Color(0xFF16121C).copy(alpha = 0.58f) else MaterialTheme.colorScheme.surface
    val outline = Color.White.copy(alpha = 0.25f)
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val latestItems = rememberUpdatedState(navigationItems)
    val latestRoute = rememberUpdatedState(currentRoute)
    val latestOnItemClick = rememberUpdatedState(onItemClick)
    var dragIndex by remember { mutableIntStateOf(-1) }
    val menuProgress by animateFloatAsState(
        targetValue = if (menuExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 440f),
        label = "menuIconMorph",
    )
    val selectedIndex = remember(currentRoute, navigationItems) {
        navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }.coerceAtLeast(0)
    }

    Box(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomInset + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.weight(1f).height(64.dp)
                    .shadow(18.dp, RoundedCornerShape(32.dp), clip = false)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (glassStyle) {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF8F8299).copy(alpha = 0.34f),
                                    Color(0xFF393241).copy(alpha = 0.82f),
                                    Color(0xFF16121C).copy(alpha = 0.92f),
                                ),
                            )
                        } else {
                            Brush.verticalGradient(listOf(containerColor, containerColor))
                        },
                    )
                    .border(1.dp, Color.White.copy(alpha = if (glassStyle) 0.38f else 0.25f), RoundedCornerShape(32.dp))
                    .pointerInput(navigationItems) {
                        val itemCount = navigationItems.size.coerceAtLeast(1)
                        val itemWidthPx = size.width.toFloat() / itemCount
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downIndex = (down.position.x / itemWidthPx).toInt().coerceIn(0, itemCount - 1)
                            dragIndex = downIndex
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
                                        val latest = latestItems.value
                                        val index = (change.position.x / itemWidthPx).toInt().coerceIn(0, latest.lastIndex)
                                        dragIndex = index
                                        if (index != lastIndex) {
                                            lastIndex = index
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
                val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.82f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                    ),
                    label = "navigationSelectionBubble",
                )
                Box(
                    modifier = Modifier.offset(x = indicatorOffset + 4.dp).align(androidx.compose.ui.Alignment.CenterStart)
                        .width(itemWidth - 8.dp).height(56.dp).clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.24f), Color(0xFFFFB84D).copy(alpha = 0.11f), Color.White.copy(alpha = 0.06f)),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(28.dp)),
                )
                if (glassStyle) GlassHighlight(Modifier.matchParentSize())
                Row(Modifier.fillMaxSize()) {
                    navigationItems.forEachIndexed { index, screen ->
                        val tabLabel = stringResource(screen.titleId)
                        val isSelected = remember(currentRoute, screen.route) {
                            isRouteSelected(currentRoute, screen.route, navigationItems)
                        }
                        val iconRes = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        val distanceFromFinger = if (dragIndex < 0) Int.MAX_VALUE else abs(index - dragIndex)
                        val scale by animateFloatAsState(
                            targetValue = when (distanceFromFinger) {
                                0 -> 1.22f
                                1 -> 1.08f
                                else -> 1f
                            },
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                            label = "navTabMagnification$index",
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
                                        is PressInteraction.Release -> {
                                            if (!isLongClick) {
                                                if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onItemClick(screen, isSelected)
                                            }
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
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                                    .zIndex(if (distanceFromFinger == 0) 1f else 0f),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) Color(0xFFFFB84D) else Color.White.copy(alpha = 0.82f),
                                )
                                Text(
                                    text = tabLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.88f),
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.size(64.dp).shadow(18.dp, CircleShape, clip = false).clip(CircleShape)
                    .background(
                        if (glassStyle) {
                            Brush.verticalGradient(
                                listOf(Color(0xFF8F8299).copy(alpha = 0.34f), Color(0xFF393241).copy(alpha = 0.82f), Color(0xFF16121C).copy(alpha = 0.92f)),
                            )
                        } else {
                            Brush.verticalGradient(listOf(containerColor, containerColor))
                        },
                    )
                    .border(1.dp, Color.White.copy(alpha = if (glassStyle) 0.38f else 0.25f), CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMenuClick()
                    }
                    .semantics { contentDescription = "Menu" },
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                if (glassStyle) GlassHighlight(Modifier.matchParentSize())
                Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = menuProgress * 90f }) {
                    val lineWidth = 2.2.dp.toPx()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val halfLength = 8.dp.toPx()
                    val gap = 6.dp.toPx()
                    val progress = menuProgress
                    drawLine(Color.White, Offset(centerX - halfLength, centerY - gap), Offset(centerX + halfLength, centerY - gap + 2f * gap * progress), lineWidth, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(centerX - halfLength, centerY + gap), Offset(centerX + halfLength, centerY + gap - 2f * gap * progress), lineWidth, cap = StrokeCap.Round)
                    drawLine(Color.White.copy(alpha = 1f - progress), Offset(centerX - halfLength, centerY), Offset(centerX + halfLength, centerY), lineWidth, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun GlassHighlight(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val leftGlow = Offset(size.width * 0.16f, -size.height * 0.12f)
        val rightGlow = Offset(size.width * 0.88f, size.height * 0.2f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), leftGlow, size.width * 0.78f),
            radius = size.width * 0.78f,
            center = leftGlow,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFB84D).copy(alpha = 0.10f), Color.Transparent), rightGlow, size.width * 0.54f),
            radius = size.width * 0.54f,
            center = rightGlow,
        )
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.34f), Color.Transparent)),
            start = Offset(size.width * 0.12f, 1.dp.toPx()),
            end = Offset(size.width * 0.88f, 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
