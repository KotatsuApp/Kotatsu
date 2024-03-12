package org.koitharu.kotatsu.core.util.ext

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.fs.FileSequence
import java.io.File
import java.io.FileFilter
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.readAttributes
import kotlin.io.path.walk

fun File.subdir(name: String) = File(this, name).also {
	if (!it.exists()) it.mkdirs()
}

fun File.takeIfReadable() = takeIf { it.exists() && it.canRead() }

fun File.takeIfWriteable() = takeIf { it.exists() && it.canWrite() }

fun File.isNotEmpty() = length() != 0L

fun ZipFile.readText(entry: ZipEntry) = getInputStream(entry).bufferedReader().use {
	it.readText()
}

fun File.getStorageName(context: Context): String = runCatching {
	val manager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		manager.getStorageVolume(this)?.getDescription(context)?.let {
			return@runCatching it
		}
	}
	when {
		Environment.isExternalStorageEmulated(this) -> context.getString(R.string.internal_storage)
		Environment.isExternalStorageRemovable(this) -> context.getString(R.string.external_storage)
		else -> null
	}
}.getOrNull() ?: context.getString(R.string.other_storage)

fun Uri.toFileOrNull() = if (scheme == URI_SCHEME_FILE) path?.let(::File) else null

suspend fun File.deleteAwait() = runInterruptible(Dispatchers.IO) {
	delete() || deleteRecursively()
}

fun ContentResolver.resolveName(uri: Uri): String? {
	val fallback = uri.lastPathSegment
	if (uri.scheme != "content") {
		return fallback
	}
	query(uri, null, null, null, null)?.use {
		if (it.moveToFirst()) {
			it.getStringOrNull(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))?.let { name ->
				return name
			}
		}
	}
	return fallback
}

suspend fun File.computeSize(): Long = runInterruptible(Dispatchers.IO) {
	walkCompat(includeDirectories = false).sumOf { it.length() }
}

fun File.children() = FileSequence(this)

fun Sequence<File>.filterWith(filter: FileFilter): Sequence<File> = filter { f -> filter.accept(f) }

val File.creationTime
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		toPath().readAttributes<BasicFileAttributes>().creationTime().toMillis()
	} else {
		lastModified()
	}

@OptIn(ExperimentalPathApi::class)
fun File.walkCompat(includeDirectories: Boolean): Sequence<File> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	// Use lazy loading on Android 8.0 and later
	val walk = if (includeDirectories) {
		toPath().walk(PathWalkOption.INCLUDE_DIRECTORIES)
	} else {
		toPath().walk()
	}
	walk.map { it.toFile() }
} else {
	// Directories are excluded by default in Path.walk(), so do it here as well
	val walk = walk()
	if (includeDirectories) walk else walk.filter { it.isFile }
}
