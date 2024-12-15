package org.koitharu.kotatsu.core.backup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.UiContext
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.BaseHttpClient
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.getBooleanOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.parseJson
import java.io.File
import javax.inject.Inject

class TelegramBackupUploader @Inject constructor(
	private val settings: AppSettings,
	@BaseHttpClient private val client: OkHttpClient,
	@ApplicationContext private val context: Context,
) {

	private val botToken = context.getString(R.string.tg_backup_bot_token)

	suspend fun uploadBackup(file: File) = withContext(Dispatchers.IO) {
		val requestBody = file.asRequestBody("application/zip".toMediaTypeOrNull())
		val multipartBody = MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("chat_id", requireChatId())
			.addFormDataPart("document", file.name, requestBody)
			.build()
		val request = Request.Builder()
			.url("https://api.telegram.org/bot$botToken/sendDocument")
			.post(multipartBody)
			.build()
		client.newCall(request).await().consume()
	}

	suspend fun sendTestMessage() {
		val request = Request.Builder()
			.url("https://api.telegram.org/bot$botToken/getMe")
			.build()
		client.newCall(request).await().consume()
		sendMessage(context.getString(R.string.backup_tg_echo))
	}

	@SuppressLint("UnsafeImplicitIntentLaunch")
	fun openBotInApp(@UiContext context: Context): Boolean {
		val botUsername = context.getString(R.string.tg_backup_bot_name)
		return runCatching {
			context.startActivity(Intent(Intent.ACTION_VIEW, "tg://resolve?domain=$botUsername".toUri()))
		}.recoverCatching {
			context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/$botUsername".toUri()))
		}.onFailure {
			Toast.makeText(context, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}.isSuccess
	}

	private suspend fun sendMessage(message: String) {
		val url = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=${requireChatId()}&text=$message"
		val request = Request.Builder()
			.url(url)
			.build()
		client.newCall(request).await().consume()
	}

	private fun requireChatId() = checkNotNull(settings.backupTelegramChatId) {
		"Telegram chat ID not set in settings"
	}

	private fun Response.consume() {
		if (isSuccessful) {
			closeQuietly()
			return
		}
		val jo = parseJson()
		if (!jo.getBooleanOrDefault("ok", true)) {
			throw RuntimeException(jo.getStringOrNull("description"))
		}
	}
}
