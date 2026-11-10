package com.metrolist.music.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.utils.CrashReporter

@Composable
fun CrashReportDialog(report: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    fun finish() { CrashReporter.deletePending(context); onDismiss() }
    fun copyReport() {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Huni crash report", report))
        finish()
    }
    fun open(intent: Intent) {
        if (CrashReporter.needsClipboard(report)) {
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("Huni crash report", report))
        }
        runCatching { context.startActivity(intent) }
        finish()
    }
    AlertDialog(
        onDismissRequest = ::finish,
        title = { Text("Huni crashed last time") },
        text = {
            Column {
                Text("Send a redacted crash report to help fix the issue?")
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide preview" else "Show preview") }
                if (expanded) {
                    Text(report, fontSize = 11.sp, modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()).padding(4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { open(Intent(Intent.ACTION_VIEW, CrashReporter.githubUri(report))) }) { Text("Report on GitHub") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = { open(Intent(Intent.ACTION_SENDTO, CrashReporter.emailUri(report))) }) { Text("Email instead") }
                TextButton(onClick = ::copyReport) { Text("Copy") }
                TextButton(onClick = ::finish) { Text("Dismiss") }
            }
        },
    )
}
