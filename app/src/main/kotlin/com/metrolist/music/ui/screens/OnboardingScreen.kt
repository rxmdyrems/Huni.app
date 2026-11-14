package com.metrolist.music.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrolist.music.R
import com.metrolist.music.constants.OnboardingDoneKey
import com.metrolist.music.constants.UserAvatarKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.metrolist.music.utils.greetingForCurrentTime
import com.metrolist.music.utils.AvatarStore

private val avatarPresets = listOf(R.drawable.avatar_amber, R.drawable.avatar_violet, R.drawable.avatar_green, R.drawable.avatar_maroon, R.drawable.avatar_ocean, R.drawable.avatar_teal, R.drawable.avatar_rose, R.drawable.avatar_orange, R.drawable.avatar_indigo, R.drawable.avatar_slate, R.drawable.avatar_mint, R.drawable.avatar_coral)
private val avatarNames = listOf("Amber bird", "Violet bird", "Green bird", "Maroon bird", "Ocean bird", "Teal bird", "Rose bird", "Orange bird", "Indigo bird", "Slate bird", "Mint bird", "Coral bird")
private val placeholders = listOf("Music lover", "Listener", "Friend")
fun sanitizeProfileName(value: String): String = value.replace(Regex("<[^>]*>"), "").filterNot(Char::isISOControl).trim().take(20)
fun placeholderForEmptyName(index: Int = 0): String = placeholders[index.coerceIn(placeholders.indices)]

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    var transitioning by remember { mutableStateOf(false) }
    var greetingRefresh by remember { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatarId by rememberSaveable { mutableStateOf("avatar_amber") }
    var fallbackAvatarId by rememberSaveable { mutableStateOf("avatar_amber") }
    var uploadedPreview by remember { mutableStateOf<Bitmap?>(null) }
    var settingUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val avatarStore = remember(context) { AvatarStore(context.filesDir) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val stored = runCatching { avatarStore.replace(uri) { context.contentResolver.openInputStream(it) } }.getOrDefault(false)
            if (stored) {
                uploadedPreview = BitmapFactory.decodeFile(avatarStore.avatarFile.absolutePath)
                avatarId = "file"
            }
        }
    }
    DisposableEffect(uploadedPreview) {
        val preview = uploadedPreview
        onDispose { preview?.recycle() }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) greetingRefresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(page) {
        if (transitioning) {
            delay(260)
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
        settingUp = true
        val clean = sanitizeProfileName(name).ifBlank { placeholderForEmptyName() }
        scope.launch {
            val chosenAvatar = if (skip) "avatar_amber" else avatarId
            if (chosenAvatar != "file") avatarStore.remove()
            context.safeDataStoreEdit { it[UserNameKey] = clean; it[UserAvatarKey] = if (chosenAvatar == "file") AvatarStore.customAvatarId(fallbackAvatarId) else chosenAvatar; it[OnboardingDoneKey] = true }
            onFinished()
        }
    }
    BackHandler { if (settingUp) return@BackHandler; if (page > 0) goToPage(page - 1) else (context as? Activity)?.finish() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.fillMaxWidth().weight(1f).pointerInput(page, transitioning, settingUp) {
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
                    if (!childConsumed && !transitioning && !settingUp && kotlin.math.abs(horizontal) >= threshold && kotlin.math.abs(horizontal) > kotlin.math.abs(vertical)) {
                        goToPage(if (horizontal < 0) page + 1 else page - 1)
                    }
                }
            }) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val direction = transitionDirection
                        (slideInHorizontally(tween(240)) { width -> direction * width } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(240)) { width -> -direction * width } + fadeOut(tween(180)))
                    },
                    label = "onboarding-page-transition",
                ) { targetPage ->
                    when (targetPage) {
                        0 -> WelcomePage()
                        1 -> Column { Text("Huni", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(20.dp)); NamePage(name, currentGreeting, { name = sanitizeProfileName(it) }) }
                        2 -> Column { Text("Huni", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(20.dp)); AvatarPage(avatarId, uploadedPreview, { selected ->
                            if (selected != "file") {
                                avatarStore.remove()
                                uploadedPreview = null
                            }
                            avatarId = selected
                            if (selected != "file") fallbackAvatarId = selected
                        }) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        else -> Column { Text("Huni", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(20.dp)); FinishPage(name.ifBlank { placeholderForEmptyName() }) }
                    }
                }
            }
            if (!settingUp) { Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { repeat(4) { i -> Box(Modifier.padding(4.dp).size(if (i == page) 10.dp else 8.dp).clip(CircleShape).background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) } }; Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { OutlinedButton(enabled = !transitioning, onClick = { if (page > 0) goToPage(page - 1) else finish(true) }) { Text(if (page == 0) "Skip" else "Back") }; Button(enabled = !transitioning, onClick = { if (page < 3) goToPage(page + 1) else finish() }) { Text(if (page == 2) "Finish" else if (page == 3) "Enjoy Huni" else "Next") } } } } else SetupPage()
        }
    }
}

@Composable private fun WelcomePage() { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Spacer(Modifier.height(36.dp)); Image(painter = painterResource(R.drawable.app_logo), contentDescription = "Huni app logo", modifier = Modifier.size(180.dp), contentScale = ContentScale.Fit); Spacer(Modifier.height(28.dp)); Text("Welcome to Huni", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)); Spacer(Modifier.height(12.dp)); Text("Thanks for installing Huni. Your music stays close, simple, and yours.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(18.dp)); Text("Let’s make it feel like home.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) } }

@Composable
private fun SetupPage() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        WavySetupLoader()
        Spacer(Modifier.height(20.dp))
        Text("Setting up Huni…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun NamePage(name: String, greeting: String, onChange: (String) -> Unit) { Column { Text("What should we call you?", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(16.dp)); OutlinedTextField(name, onChange, label = { Text("Name or nickname") }, singleLine = true, supportingText = { Text("${name.length}/20") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)); Text(greeting, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false) } }
@Composable private fun AvatarPage(selected: String, uploaded: Bitmap?, onSelected: (String) -> Unit, onUpload: () -> Unit) { Column { Text("Pick an avatar", style = MaterialTheme.typography.headlineMedium); Text("Photos never leave this phone.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); LazyVerticalGrid(GridCells.Fixed(4), Modifier.height(280.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(avatarPresets.indices.toList()) { i -> val id = "avatar_${avatarNames[i].substringBefore(' ').lowercase()}"; Image(painterResource(avatarPresets[i]), avatarNames[i], Modifier.size(64.dp).clip(CircleShape).clickable { onSelected(id) }, contentScale = ContentScale.Crop) } }; if (uploaded != null && selected == "file") Image(uploaded.asImageBitmap(), "Your uploaded photo", Modifier.size(64.dp).clip(CircleShape)); OutlinedButton(onUpload, Modifier.fillMaxWidth()) { Text("Upload your own photo") } } }
@Composable private fun FinishPage(name: String) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Spacer(Modifier.height(60.dp)); Text("Good evening, $name", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(12.dp)); Text("You’re all set. Thanks for choosing Huni — enjoy the app.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
