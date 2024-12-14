package org.koitharu.kotatsu.core.backup

import android.content.Context
import android.net.Uri
import androidx.annotation.CheckResult
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.buffer
import okio.sink
import okio.source
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import java.io.IOException
import javax.inject.Inject

class ExternalBackupStorage @Inject constructor(
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) {

	suspend fun list(): List<BackupFile> = runInterruptible(Dispatchers.IO) {
		getRootOrThrow().listFiles().mapNotNull {
			if (it.isFile && it.canRead()) {
				BackupFile(
					uri = it.uri,
					dateTime = it.name?.let { fileName ->
						BackupZipOutput.parseBackupDateTime(fileName)
					} ?: return@mapNotNull null,
				)
			} else {
				null
			}
		}.sortedDescending()
	}

	suspend fun listOrNull() = runCatchingCancellable {
		list()
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrNull()

	suspend fun put(file: File): Uri = runInterruptible(Dispatchers.IO) {
		val out = checkNotNull(getRootOrThrow().createFile("application/zip", file.nameWithoutExtension)) {
			"Cannot create target backup file"
		}
		checkNotNull(context.contentResolver.openOutputStream(out.uri, "wt")).sink().use { sink ->
			file.source().buffer().use { src ->
				src.readAll(sink)
			}
		}
		out.uri
	}

	@CheckResult
	suspend fun delete(victim: BackupFile) = runInterruptible(Dispatchers.IO) {
		val df = DocumentFile.fromSingleUri(context, victim.uri)
		df != null && df.delete()
	}

	suspend fun getLastBackupDate() = listOrNull()?.maxOfOrNull { it.dateTime }

	suspend fun trim(maxCount: Int): Boolean {
		if (maxCount == Int.MAX_VALUE) {
			return false
		}
		val list = listOrNull()
		if (list == null || list.size <= maxCount) {
			return false
		}
		var result = false
		for (i in maxCount until list.size) {
			if (delete(list[i])) {
				result = true
			}
		}
		return result
	}

	@Blocking
	private fun getRootOrThrow(): DocumentFile {
		val uri = checkNotNull(settings.periodicalBackupDirectory) {
			"Backup directory is not specified"
		}
		val root = DocumentFile.fromTreeUri(context, uri)
		return checkNotNull(root) { "Cannot obtain DocumentFile from $uri" }
	}
}
class TelegramBackupUploader @Inject constructor(private val settings: AppSettings) {

	private val client = OkHttpClient()

	suspend fun uploadBackupToTelegram(file: File) = withContext(Dispatchers.IO) {
		val botToken = "7455491254:AAGYJKgpP1DZN3d9KZfb8tvtIdaIMxUayXM"
		val chatId = settings.telegramChatId

		if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
			throw IllegalStateException("Telegram API key or chat ID not set in settings.")
		}

		val mediaType = "application/zip".toMediaTypeOrNull()
		val requestBody = file.asRequestBody(mediaType)

		val multipartBody = MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("chat_id", chatId)
			.addFormDataPart("document", file.name, requestBody)
			.build()

		val request = Request.Builder()
			.url("https://api.telegram.org/bot$botToken/sendDocument")
			.post(multipartBody)
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw IOException("Failed to send backup to Telegram: ${response.message}")
			}
		}
	}
}
