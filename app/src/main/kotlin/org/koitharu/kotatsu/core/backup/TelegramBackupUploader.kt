package org.koitharu.kotatsu.core.backup

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.UiContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.BaseHttpClient
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.parsers.util.await
import java.io.File
import javax.inject.Inject

class TelegramBackupUploader @Inject constructor(
	private val settings: AppSettings,
	@BaseHttpClient private val client: OkHttpClient,
	@ApplicationContext private val context: Context,
) {

	private val botToken = context.getString(R.string.tg_backup_bot_token)

	suspend fun uploadBackupToTelegram(file: File) = withContext(Dispatchers.IO) {

		val mediaType = "application/zip".toMediaTypeOrNull()
		val requestBody = file.asRequestBody(mediaType)

		val multipartBody = MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("chat_id", requireChatId())
			.addFormDataPart("document", file.name, requestBody)
			.build()

		val request = Request.Builder()
			.url("https://api.telegram.org/bot$botToken/sendDocument")
			.post(multipartBody)
			.build()

		client.newCall(request).await().ensureSuccess().closeQuietly()
	}

	suspend fun checkTelegramBotApiKey(apiKey: String) {
		val request = Request.Builder()
			.url("https://api.telegram.org/bot$apiKey/getMe")
			.build()
		client.newCall(request).await().ensureSuccess().closeQuietly()
		sendMessageToTelegram(apiKey, context.getString(R.string.backup_tg_echo))
	}

	@SuppressLint("UnsafeImplicitIntentLaunch")
	fun openTelegramBot(@UiContext context: Context) {
		val botUsername = context.getString(R.string.tg_backup_bot_name)
		try {
			val telegramIntent = Intent(Intent.ACTION_VIEW)
			telegramIntent.data = Uri.parse("tg://resolve?domain=$botUsername")
			context.startActivity(telegramIntent)
		} catch (e: ActivityNotFoundException) {
			val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$botUsername"))
			context.startActivity(browserIntent)
		}
	}

	private suspend fun sendMessageToTelegram(apiKey: String, message: String) {
		val url = "https://api.telegram.org/bot$apiKey/sendMessage?chat_id=${requireChatId()}&text=$message"
		val request = Request.Builder()
			.url(url)
			.build()

		client.newCall(request).await().ensureSuccess().closeQuietly()
	}

	private fun requireChatId() = checkNotNull(settings.backupTelegramChatId) {
		"Telegram chat ID not set in settings"
	}
}
