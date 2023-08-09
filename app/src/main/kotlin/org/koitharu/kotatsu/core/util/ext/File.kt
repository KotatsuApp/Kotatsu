package org.koitharu.kotatsu.core.util.ext

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import androidx.annotation.WorkerThread
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.fs.FileSequence
import java.io.File
import java.io.FileFilter
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.readAttributes

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

fun Uri.toFileOrNull() = if (scheme == "file") path?.let(::File) else null

suspend fun File.deleteAwait() = withContext(Dispatchers.IO) {
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
	computeSizeInternal(this)
}

@WorkerThread
private fun computeSizeInternal(file: File): Long {
	return if (file.isDirectory) {
		file.children().sumOf { computeSizeInternal(it) }
	} else {
		file.length()
	}
}

fun File.listFilesRecursive(filter: FileFilter? = null): Sequence<File> = sequence {
	listFilesRecursiveImpl(this@listFilesRecursive, filter)
}

private suspend fun SequenceScope<File>.listFilesRecursiveImpl(root: File, filter: FileFilter?) {
	val ss = root.children()
	for (f in ss) {
		if (f.isDirectory) {
			listFilesRecursiveImpl(f, filter)
		} else if (filter == null || filter.accept(f)) {
			yield(f)
		}
	}
}

fun File.children() = FileSequence(this)

fun Sequence<File>.filterWith(filter: FileFilter): Sequence<File> = filter { f -> filter.accept(f) }

val File.creationTime
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		toPath().readAttributes<BasicFileAttributes>().creationTime().toMillis()
	} else {
		lastModified()
	}
