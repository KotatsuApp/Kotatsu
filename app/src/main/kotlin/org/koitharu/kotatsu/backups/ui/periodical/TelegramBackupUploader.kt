package org.koitharu.kotatsu.backups.ui.periodical

import android.content.Context
import androidx.annotation.CheckResult
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
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

	val isAvailable: Boolean
		get() = botToken.isNotEmpty()

	suspend fun uploadBackup(file: File) {
		val requestBody = file.asRequestBody("application/zip".toMediaTypeOrNull())
		val multipartBody = MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("chat_id", requireChatId())
			.addFormDataPart("document", file.name, requestBody)
			.build()
		val request = Request.Builder()
			.url(urlOf("sendDocument").build())
			.post(multipartBody)
			.build()
		client.newCall(request).await().consume()
	}

	suspend fun sendTestMessage() {
		val request = Request.Builder()
			.url(urlOf("getMe").build())
			.build()
		client.newCall(request).await().consume()
		sendMessage(context.getString(R.string.backup_tg_echo))
	}

	@CheckResult
	fun openBotInApp(router: AppRouter): Boolean {
		val botUsername = context.getString(R.string.tg_backup_bot_name)
		return router.openExternalBrowser("tg://resolve?domain=$botUsername") ||
			router.openExternalBrowser("https://t.me/$botUsername")
	}

	private suspend fun sendMessage(message: String) {
		val url = urlOf("sendMessage")
			.addQueryParameter("chat_id", requireChatId())
			.addQueryParameter("text", message)
			.build()
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

	private fun urlOf(method: String) = HttpUrl.Builder()
		.scheme("https")
		.host("api.telegram.org")
		.addPathSegment("bot$botToken")
		.addPathSegment(method)
}
