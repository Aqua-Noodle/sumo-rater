package com.aquanoodle.sumorater.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copies a picked photo into app-internal storage, keyed by rikishi id, so it survives reboots. */
suspend fun copyPhotoToInternalStorage(context: Context, uri: Uri, rikishiId: Int): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val dest = File(dir, "$rikishiId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            dest.absolutePath
        } catch (e: Exception) {
            null
        }
    }
