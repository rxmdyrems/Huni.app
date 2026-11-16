/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.LocalNavController
import com.metrolist.music.R
import com.metrolist.music.ui.screens.settings.AccountSettings
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavController

@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                horizontalAlignment = horizontalAlignment,
                modifier =
                    modifier
                        .padding(24.dp),
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                        Box(
                            Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            icon()
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            Box(
                                // Align the title to the center when an icon is present.
                                Modifier.align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally),
                            ) {
                                title()
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                content()

                if (buttons != null) {
                    Spacer(Modifier.height(24.dp))

                    FlowRow(
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                            ProvideTextStyle(
                                value = MaterialTheme.typography.labelLarge,
                            ) {
                                buttons()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSettingsDialog(
    onDismiss: () -> Unit,
    latestVersionName: String,
    latestVersionCode: Int,
    onSettingsClick: () -> Unit = {},
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val navController = LocalNavController.current
    val visible = remember { mutableStateOf(false) }
    val closing = remember { mutableStateOf(false) }
    val glassSweep by rememberInfiniteTransition(label = "accountGlassSweep").animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "accountGlassSweep",
    )
    val closeModal = {
        if (!closing.value) {
            closing.value = true
            visible.value = false
        }
    }
    LaunchedEffect(Unit) { visible.value = true }
    LaunchedEffect(closing.value) {
        if (closing.value) {
            delay(420)
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = closeModal,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        val dialogWindow = (LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val targetWidth = (maxWidth - 32.dp).coerceAtLeast(64.dp)
            val targetHeight = (maxHeight - 152.dp).coerceAtLeast(240.dp)
            val startX = (maxWidth - 80.dp).coerceAtLeast(0.dp)
            val startY = (maxHeight - bottomInset - 72.dp).coerceAtLeast(0.dp)
            val offsetX by animateDpAsState(
                targetValue = if (visible.value) 16.dp else startX,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
                label = "accountModalX",
            )
            val offsetY by animateDpAsState(
                targetValue = if (visible.value) 72.dp else startY,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
                label = "accountModalY",
            )
            val width by animateDpAsState(
                targetValue = if (visible.value) targetWidth else 64.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
                label = "accountModalWidth",
            )
            val height by animateDpAsState(
                targetValue = if (visible.value) targetHeight else 64.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
                label = "accountModalHeight",
            )
            val corner by animateDpAsState(
                targetValue = if (visible.value) 28.dp else 32.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
                label = "accountModalCorner",
            )
            val scrimAlpha by animateFloatAsState(
                targetValue = if (visible.value) 0.52f else 0f,
                animationSpec = tween(200),
                label = "accountModalScrim",
            )
            val contentAlpha by animateFloatAsState(
                targetValue = if (visible.value) 1f else 0f,
                animationSpec = tween(150, delayMillis = 80),
                label = "accountModalContent",
            )
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { closeModal() },
                )
                Box(
                    Modifier.offset(x = offsetX, y = offsetY).size(width, height)
                        .clip(RoundedCornerShape(corner))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.17f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.91f),
                                    Color(0xFF17131D).copy(alpha = 0.87f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(corner))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(corner),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                        tonalElevation = 4.dp,
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Canvas(Modifier.fillMaxSize()) {
                                val leftGlow = Offset(size.width * 0.12f, -size.height * 0.08f)
                                val rightGlow = Offset(size.width * 0.92f, size.height * 0.18f)
                                drawCircle(
                                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), leftGlow, size.width * 0.8f),
                                    radius = size.width * 0.8f,
                                    center = leftGlow,
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(listOf(Color(0xFFFFB84D).copy(alpha = 0.10f), Color.Transparent), rightGlow, size.width * 0.65f),
                                    radius = size.width * 0.65f,
                                    center = rightGlow,
                                )
                                val sweepX = size.width * glassSweep
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.055f), Color.Transparent),
                                        start = Offset(sweepX - size.height * 0.25f, 0f),
                                        end = Offset(sweepX + size.height * 0.25f, size.height),
                                    ),
                                    topLeft = Offset(sweepX - size.height * 0.25f, 0f),
                                    size = Size(size.height * 0.5f, size.height),
                                )
                            }
                            Box(Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                                AccountSettings(
                                    navController = navController,
                                    onClose = closeModal,
                                    latestVersionName = latestVersionName,
                                    latestVersionCode = latestVersionCode,
                                    onSettingsClick = {
                                        onSettingsClick()
                                        closeModal()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPromptDialog(
    title: String? = null,
    titleBar: @Composable (RowScope.() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReset: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title =
            if (titleBar != null) {
                { Row { titleBar() } }
            } else if (title != null) {
                {
                    Text(
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            } else {
                null
            },
        buttons = {
            if (onReset != null) {
                Row(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { onReset() },
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                }
            }

            if (onCancel != null) {
                TextButton(
                    onClick = { onCancel() },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }

            TextButton(
                onClick = { onConfirm() },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        content()
    }
}

@Composable
fun ListDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    modifier
                        .padding(vertical = 24.dp)
                        .imePadding(),
            ) {
                LazyColumn(content = content)
            }
        }
    }
}

@Composable
fun InfoLabel(text: String) =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(4.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    initialTextFieldValue: TextFieldValue = TextFieldValue(),
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    autoFocus: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 10,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int? = null,
    onDone: (String) -> Unit = {},
    // new multi-field support
    textFields: List<Pair<String, TextFieldValue>>? = null,
    onTextFieldsChange: ((Int, TextFieldValue) -> Unit)? = null,
    onDoneMultiple: ((List<String>) -> Unit)? = null,
    onDismiss: () -> Unit,
    autoDismiss: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val legacyFieldState = remember { mutableStateOf(initialTextFieldValue) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        icon = icon,
        title = title,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }

            val isValid =
                textFields?.all { isInputValid(it.second.text) }
                    ?: isInputValid(legacyFieldState.value.text)

            TextButton(
                enabled = isValid,
                onClick = {
                    if (autoDismiss) onDismiss()
                    if (textFields != null && onDoneMultiple != null) {
                        onDoneMultiple(textFields.map { it.second.text })
                    } else {
                        onDone(legacyFieldState.value.text)
                    }
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f, fill = false),
        ) {
            if (textFields != null) {
                textFields.forEachIndexed { index, (label, value) ->
                    TextField(
                        value = value,
                        onValueChange = { onTextFieldsChange?.invoke(index, it) },
                        placeholder = { Text(label) },
                        singleLine = singleLine,
                        maxLines = maxLines,
                        colors = OutlinedTextFieldDefaults.colors(),
                        keyboardOptions =
                            KeyboardOptions(
                                imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                                keyboardType = keyboardType,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    if (onDoneMultiple != null) {
                                        onDoneMultiple(textFields.map { it.second.text })
                                        if (autoDismiss) onDismiss()
                                    }
                                },
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (index < textFields.size - 1) 12.dp else 0.dp)
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                }
            } else {
                // Wrap in Box with pointerInput to prevent double-click crashes in TextClassifier
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { /* Consume double-tap to prevent TextClassifier crash */ },
                                )
                            },
                ) {
                    TextField(
                        value = legacyFieldState.value,
                        onValueChange = {
                            legacyFieldState.value =
                                if (maxLength != null && it.text.length > maxLength) {
                                    it.copy(text = it.text.take(maxLength))
                                } else {
                                    it
                                }
                        },
                        placeholder = placeholder,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        colors = OutlinedTextFieldDefaults.colors(),
                        keyboardOptions =
                            KeyboardOptions(
                                imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                                keyboardType = keyboardType,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    onDone(legacyFieldState.value.text)
                                    if (autoDismiss) onDismiss()
                                },
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                    )
                }
                if (maxLength != null) {
                    Text(
                        text = "${legacyFieldState.value.text.length}/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (legacyFieldState.value.text.length >= maxLength) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier =
                            Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
                    )
                }
            }

            extraContent?.invoke()
        }
    }
}
