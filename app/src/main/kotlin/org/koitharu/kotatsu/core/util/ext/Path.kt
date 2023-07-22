package org.koitharu.kotatsu.core.util.ext

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.walk

@OptIn(ExperimentalPathApi::class)
suspend fun Path.computeSize(): Long = runInterruptible(Dispatchers.IO) {
	// Directories are not included by default
	walk().sumOf { it.fileSize() }
}

fun Uri.toPathOrNull() = if (scheme == "file") path?.let { Path(it) } else null
