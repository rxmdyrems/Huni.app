package com.metrolist.music.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.metrolist.music.R
import com.metrolist.music.constants.OnboardingDoneKey
import com.metrolist.music.constants.UserAvatarKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private val avatarPresets = listOf(
    R.drawable.avatar_amber, R.drawable.avatar_violet, R.drawable.avatar_green, R.drawable.avatar_maroon,
    R.drawable.avatar_ocean, R.drawable.avatar_teal, R.drawable.avatar_rose, R.drawable.avatar_orange,
    R.drawable.avatar_indigo, R.drawable.avatar_slate, R.drawable.avatar_mint, R.drawable.avatar_coral,
)
private val avatarNames = listOf("Amber bird", "Violet bird", "Green bird", "Maroon bird", "Ocean bird", "Teal bird", "Rose bird", "Orange bird", "Indigo bird", "Slate bird", "Mint bird", "Coral bird")
private val placeholders = listOf("Music lover", "Listener", "Friend")

fun sanitizeProfileName(value: String): String = value.replace(Regex("<[^>]*>"), "").filterNot(Char::isISOControl).trim().take(20)
fun placeholderForEmptyName(index: Int = 0): String = placeholders[index.coerceIn(placeholders.indices)]

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatarId by rememberSaveable { mutableStateOf("avatar_amber") }
    var uploadedPreview by remember { mutableStateOf<Bitmap?>(null) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            saveAvatar(context, uri)
            uploadedPreview = decodeAvatar(context, uri)
            avatarId = "file"
        }
    }

    fun finish(skip: Boolean = false) {
        val clean = sanitizeProfileName(name).ifBlank { placeholderForEmptyName() }
        scope.launch {
            context.safeDataStoreEdit {
                it[UserNameKey] = clean
                it[UserAvatarKey] = if (skip) "avatar_amber" else avatarId
                it[OnboardingDoneKey] = true
            }
            onFinished()
        }
    }

    BackHandler {
        if (page > 0) page-- else (context as? Activity)?.finish()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Huni", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                when (page) {
                    0 -> WelcomePage()
                    1 -> NamePage(name, { name = sanitizeProfileName(it) })
                    2 -> AvatarPage(avatarId, uploadedPreview, { avatarId = it }, { picker.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) })
                    else -> FinishPage(name.ifBlank { placeholderForEmptyName() }, avatarId)
                }
            }
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(4) { index ->
                        Box(Modifier.padding(4.dp).size(if (index == page) 10.dp else 8.dp).clip(CircleShape).background(if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { if (page > 0) page-- else finish(true) }) { Text(if (page == 0) "Skip" else "Back") }
                    if (page < 3) Button(onClick = { page++ }) { Text(if (page == 2) "Finish" else "Next") } else Button(onClick = { finish() }) { Text("Start listening") }
                }
            }
        }
    }
}

@Composable private fun WelcomePage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(50.dp))
        Text("Your music, your mood", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Free and open source. Play YouTube and your local files, with no account needed to listen.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable private fun NamePage(name: String, onNameChange: (String) -> Unit) {
    Column {
        Text("What should we call you?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name or nickname") }, singleLine = true, supportingText = { Text("${name.length}/20") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text("Good evening, ${name.ifBlank { placeholderForEmptyName() }}", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable private fun AvatarPage(selected: String, uploaded: Bitmap?, onSelected: (String) -> Unit, onUpload: () -> Unit) {
    Column {
        Text("Pick an avatar", style = MaterialTheme.typography.headlineMedium)
        Text("Photos never leave this phone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(280.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(avatarPresets.indices.toList()) { index ->
                val id = "avatar_${avatarNames[index].substringBefore(' ').lowercase()}"
                Image(painterResource(avatarPresets[index]), avatarNames[index], contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(CircleShape).background(if (selected == id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { onSelected(id) })
            }
        }
        if (uploaded != null && selected == "file") Image(uploaded.asImageBitmap(), "Your uploaded photo", contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(CircleShape))
        OutlinedButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) { Text("Upload your own photo") }
    }
}

@Composable private fun FinishPage(name: String, avatar: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(60.dp))
        Text("Good evening, $name", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("You can change your name and avatar later in About.", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun decodeAvatar(context: Context, uri: Uri): Bitmap? = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

private fun saveAvatar(context: Context, uri: Uri) {
    val source = decodeAvatar(context, uri) ?: return
    val size = minOf(source.width, source.height)
    val cropped = Bitmap.createBitmap(source, (source.width - size) / 2, (source.height - size) / 2, size, size, Matrix(), true)
    val scaled = Bitmap.createScaledBitmap(cropped, 512, 512, true)
    val dir = File(context.filesDir, "profile").apply { mkdirs() }
    File(dir, "avatar.webp").outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, it) }
    if (cropped != source) cropped.recycle()
    if (scaled != cropped) scaled.recycle()
}
