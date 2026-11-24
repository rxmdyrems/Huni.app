package com.metrolist.music.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrolist.music.R
import com.metrolist.music.constants.OnboardingDoneKey
import com.metrolist.music.constants.UserAvatarKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.ui.theme.bbhBartle
import com.metrolist.music.utils.AvatarStore
import com.metrolist.music.utils.UserAvatarImage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.greetingForCurrentTime
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val avatarPresets = listOf(
    R.drawable.avatar_amber,
    R.drawable.avatar_violet,
    R.drawable.avatar_green,
    R.drawable.avatar_maroon,
    R.drawable.avatar_ocean,
    R.drawable.avatar_teal,
    R.drawable.avatar_rose,
    R.drawable.avatar_orange,
    R.drawable.avatar_indigo,
    R.drawable.avatar_slate,
    R.drawable.avatar_mint,
    R.drawable.avatar_coral,
)
private val avatarNames = listOf(
    "Amber bird", "Violet bird", "Green bird", "Maroon bird", "Ocean bird", "Teal bird",
    "Rose bird", "Orange bird", "Indigo bird", "Slate bird", "Mint bird", "Coral bird",
)
private val placeholders = listOf("Music lover", "Listener", "Friend")

fun sanitizeProfileName(value: String): String =
    value.replace(Regex("<[^>]*>"), "").filterNot(Char::isISOControl).trim().take(20)

fun placeholderForEmptyName(index: Int = 0): String = placeholders[index.coerceIn(placeholders.indices)]

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val animationsEnabled = rememberSystemAnimationsEnabled()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    var transitioning by remember { mutableStateOf(false) }
    var greetingRefresh by remember { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    val avatarPreference = rememberPreference(UserAvatarKey, "avatar_amber")
    val avatarId by avatarPreference
    var settingUp by remember { mutableStateOf(false) }
    var avatarSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val avatarStore = remember(context) { AvatarStore(context.filesDir) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && !avatarSaving) {
            avatarSaving = true
            val previousAvatarId = avatarId
            scope.launch(Dispatchers.IO) {
                try {
                    val previousPhoto = try {
                        avatarStore.snapshot()
                    } catch (_: Exception) {
                        return@launch
                    }
                    val stored = runCatching {
                        avatarStore.replace(uri) { context.contentResolver.openInputStream(it) }
                    }.getOrDefault(false)
                    if (stored) {
                        val nextAvatarId = AvatarStore.nextCustomAvatarId(
                            AvatarStore.fallbackPreset(previousAvatarId),
                            previousAvatarId,
                        )
                        val persisted = runCatching {
                            context.safeDataStoreEdit { it[UserAvatarKey] = nextAvatarId }
                        }.getOrDefault(false)
                        if (!persisted) runCatching { avatarStore.restore(previousPhoto) }
                    }
                } finally {
                    withContext(Dispatchers.Main) { avatarSaving = false }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) greetingRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(page, animationsEnabled) {
        if (transitioning) {
            if (animationsEnabled) delay(260)
            transitioning = false
        }
    }

    fun goToPage(targetPage: Int) {
        if (transitioning || settingUp) return
        val target = targetPage.coerceIn(0, 3)
        if (target != page) {
            transitionDirection = if (target > page) 1 else -1
            transitioning = true
            page = target
        }
    }

    val currentGreeting = remember(greetingRefresh, name) {
        greetingForCurrentTime(name.ifBlank { placeholderForEmptyName() })
    }

    fun finish(skip: Boolean = false) {
        if (avatarSaving || settingUp) return
        settingUp = true
        val clean = sanitizeProfileName(name).ifBlank { placeholderForEmptyName() }
        scope.launch {
            val chosenAvatar = if (skip) {
                "avatar_amber"
            } else {
                context.dataStore.data.first()[UserAvatarKey] ?: "avatar_amber"
            }
            val persisted = context.safeDataStoreEdit {
                it[UserNameKey] = clean
                it[UserAvatarKey] = chosenAvatar
                it[OnboardingDoneKey] = true
            }
            if (persisted) {
                if (skip || !AvatarStore.isCustomAvatarId(chosenAvatar)) avatarStore.remove()
                withContext(Dispatchers.Main) { onFinished() }
            } else {
                settingUp = false
            }
        }
    }

    BackHandler {
        if (settingUp) return@BackHandler
        if (page > 0) goToPage(page - 1) else (context as? Activity)?.finish()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(page, transitioning, settingUp) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var horizontal = 0f
                            var vertical = 0f
                            var childConsumed = down.isConsumed
                            val pointer = down.id
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointer } ?: event.changes.first()
                                val movement = change.position - change.previousPosition
                                childConsumed = childConsumed || change.isConsumed
                                horizontal += movement.x
                                vertical += movement.y
                                if (!change.pressed) break
                            } while (true)
                            val threshold = with(density) { 100.dp.toPx() }
                            if (!childConsumed && !transitioning && !settingUp &&
                                kotlin.math.abs(horizontal) >= threshold && kotlin.math.abs(horizontal) > kotlin.math.abs(vertical)
                            ) {
                                goToPage(if (horizontal < 0) page + 1 else page - 1)
                            }
                        }
                    },
            ) {
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = page,
                    transitionSpec = {
                        val direction = transitionDirection
                        val slideDuration = if (animationsEnabled) 240 else 0
                        val fadeDuration = if (animationsEnabled) 180 else 0
                        (slideInHorizontally(tween(slideDuration)) { width -> direction * width } +
                            fadeIn(tween(fadeDuration))) togetherWith
                            (slideOutHorizontally(tween(slideDuration)) { width -> -direction * width } +
                                fadeOut(tween(fadeDuration)))
                    },
                    label = "onboarding-page-transition",
                ) { targetPage ->
                    when (targetPage) {
                        0 -> WelcomePage()
                        1 -> StepPage {
                            NamePage(name, currentGreeting, onChange = { name = sanitizeProfileName(it) })
                        }
                        2 -> StepPage {
                            AvatarPage(
                                selected = avatarId,
                                enabled = !avatarSaving,
                                onSelected = { selected ->
                                    if (selected != avatarId && !avatarSaving) {
                                        avatarSaving = true
                                        scope.launch(Dispatchers.IO) {
                                            val persisted = context.safeDataStoreEdit { it[UserAvatarKey] = selected }
                                            if (persisted) avatarStore.selectPreset()
                                            withContext(Dispatchers.Main) { avatarSaving = false }
                                        }
                                    }
                                },
                                onUpload = {
                                    if (!avatarSaving) {
                                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                },
                            )
                        }
                        else -> StepPage { FinishPage(currentGreeting) }
                    }
                }
            }

            if (!settingUp) {
                Column(Modifier.imePadding()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(4) { index ->
                            Box(
                                Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(width = if (index == page) 24.dp else 8.dp, height = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (index == page) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            enabled = !transitioning && !avatarSaving,
                            onClick = { if (page > 0) goToPage(page - 1) else finish(skip = true) },
                        ) {
                            Text(if (page == 0) "Skip" else "Back")
                        }
                        Button(
                            enabled = !transitioning && !avatarSaving,
                            onClick = { if (page < 3) goToPage(page + 1) else finish() },
                        ) {
                            Text(if (page == 2) "Finish" else if (page == 3) "Enjoy Huni" else "Next")
                        }
                    }
                }
            } else {
                FirstRunSetupContent(
                    step = 0,
                    animationsEnabled = animationsEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun StepPage(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.huni_wordmark_bird),
            contentDescription = "Huni wordmark with bird",
            modifier = Modifier.width(176.dp).height(58.dp),
            contentScale = ContentScale.Fit,
        )
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

@Composable
private fun WelcomePage() {
    val colors = MaterialTheme.colorScheme
    val animationsEnabled = rememberSystemAnimationsEnabled()
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(colors.surfaceContainerHigh.copy(alpha = .86f), colors.background)),
            RoundedCornerShape(28.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        FallingSakura(Modifier.fillMaxSize(), animate = animationsEnabled)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.huni_wordmark_bird),
                contentDescription = "Huni wordmark with bird",
                modifier = Modifier.width(280.dp).height(112.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(34.dp))
            Text(
                "Your music,\nin its own light.",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A listening space that feels like yours. Your music stays personal, right here on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Let’s make it feel like home.",
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NamePage(name: String, greeting: String, onChange: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("What should we call you?", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = name,
            onValueChange = onChange,
            label = { Text("Name or nickname") },
            singleLine = true,
            supportingText = { Text("${name.length}/20") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            greeting,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun AvatarPage(
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    onUpload: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pick an avatar", style = MaterialTheme.typography.headlineMedium)
        Text("Photos never leave this phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().heightIn(min = 196.dp, max = 292.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(avatarPresets.indices.toList()) { index ->
                val id = "avatar_${avatarNames[index].substringBefore(' ').lowercase()}"
                AvatarChoice(
                    image = { Image(painterResource(avatarPresets[index]), avatarNames[index], contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) },
                    label = avatarNames[index],
                    selected = selected == id,
                    enabled = enabled,
                    onClick = { onSelected(id) },
                )
            }
        }
        if (AvatarStore.isCustomAvatarId(selected)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UserAvatarImage(selected, Modifier.size(44.dp).clip(CircleShape), "Your uploaded photo")
                Text("Your photo", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Icon(painterResource(R.drawable.check), contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
        OutlinedButton(enabled = enabled, onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
            Text("Upload your own photo")
        }
    }
}

@Composable
private fun AvatarChoice(
    image: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().padding(3.dp).clip(CircleShape)) { image() }
        if (selected) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp).size(22.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = "$label selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishPage(greeting: String) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(greeting, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            "You’re all set. Thanks for choosing Huni — enjoy the app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
