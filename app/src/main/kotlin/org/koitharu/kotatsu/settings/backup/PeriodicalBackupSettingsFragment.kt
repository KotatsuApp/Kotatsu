package org.koitharu.kotatsu.settings.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.backup.BackupZipOutput.Companion.DIR_BACKUPS
import org.koitharu.kotatsu.core.backup.ExternalBackupStorage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.resolveFile
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class PeriodicalBackupSettingsFragment : BasePreferenceFragment(R.string.periodic_backups),
	ActivityResultCallback<Uri?> {

	@Inject
	lateinit var backupStorage: ExternalBackupStorage

	private val outputSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree(),
		this,
	)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_backup_periodic)

		val openTelegramBotPreference = findPreference<Preference>("open_telegram_chat")

		openTelegramBotPreference?.setOnPreferenceClickListener {
			openTelegramBot("kotatsu_backup_bot")
			true
		}
		val checkApiButton = Preference(requireContext()).apply {
			key = "check_api_working"
			title = "Проверить работу API"
			summary = "Нажмите для проверки работы Telegram Bot API"
		}

		checkApiButton.setOnPreferenceClickListener {
			val apiKey = "7455491254:AAGYJKgpP1DZN3d9KZfb8tvtIdaIMxUayXM" // Получите API Key из настроек
			if (apiKey.isNotEmpty()) {
				checkTelegramBotApiKey(apiKey)
			} else {
				Toast.makeText(requireContext(), "Введите API Key в настройках!", Toast.LENGTH_SHORT).show()
			}
			true
		}

		preferenceScreen.addPreference(checkApiButton)
	}
	private fun checkTelegramBotApiKey(apiKey: String) {
		val url = "https://api.telegram.org/bot$apiKey/getMe"

		val client = OkHttpClient()
		val request = Request.Builder()
			.url(url)
			.build()

		client.newCall(request).enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				requireActivity().runOnUiThread {
					if (response.isSuccessful) {
						sendMessageToTelegram(apiKey, "Kotatsu's backup in Telegram is working!!")
					}
				}
			}

			override fun onFailure(call: Call, e: IOException) {
				requireActivity().runOnUiThread {
					Toast.makeText(requireContext(), "Network error! Check your Net", Toast.LENGTH_SHORT).show()
				}
			}
		})
	}
	private fun openTelegramBot(botUsername: String) {
		try {
			val telegramIntent = Intent(Intent.ACTION_VIEW)
			telegramIntent.data = Uri.parse("https://t.me/$botUsername")
			telegramIntent.setPackage("org.telegram.messenger")
			startActivity(telegramIntent)
		} catch (e: Exception) {
			val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$botUsername"))
			startActivity(browserIntent)
		}
	}
	private fun sendMessageToTelegram(apiKey: String, message: String) {
		val chatId = settings.telegramChatId
		if (chatId.isNullOrEmpty()) {
			Toast.makeText(requireContext(), "Chat ID is not set!", Toast.LENGTH_SHORT).show()
			return
		}

		val url = "https://api.telegram.org/bot$apiKey/sendMessage?chat_id=$chatId&text=$message"
		val client = OkHttpClient()
		val request = Request.Builder()
			.url(url)
			.build()

		client.newCall(request).enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				requireActivity().runOnUiThread {
					if (response.isSuccessful) {
						Toast.makeText(requireContext(), "Success! Check Telegram Bot", Toast.LENGTH_SHORT).show()
					} else {
						Toast.makeText(requireContext(), "OOPS! Something went wrong", Toast.LENGTH_SHORT).show()
					}
				}
			}

			override fun onFailure(call: Call, e: IOException) {
				requireActivity().runOnUiThread {
					Toast.makeText(requireContext(), "Network error!", Toast.LENGTH_SHORT).show()
				}
			}
		})
	}


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindOutputSummary()
		bindLastBackupInfo()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT -> outputSelectCall.tryLaunch(null)
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			context?.contentResolver?.takePersistableUriPermission(result, takeFlags)
			settings.periodicalBackupDirectory = result
			bindOutputSummary()
			bindLastBackupInfo()
		}
	}

	private fun bindOutputSummary() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT) ?: return
		viewLifecycleScope.launch {
			preference.summary = withContext(Dispatchers.Default) {
				val value = settings.periodicalBackupDirectory
				value?.toUserFriendlyString(preference.context) ?: preference.context.run {
					getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
				}.path
			}
		}
	}

	private fun bindLastBackupInfo() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_LAST) ?: return
		viewLifecycleScope.launch {
			val lastDate = withContext(Dispatchers.Default) {
				backupStorage.getLastBackupDate()
			}
			preference.summary = lastDate?.let {
				preference.context.getString(
					R.string.last_successful_backup,
					DateUtils.getRelativeTimeSpanString(it.time),
				)
			}
			preference.isVisible = lastDate != null
		}
	}

	private fun Uri.toUserFriendlyString(context: Context): String {
		val df = DocumentFile.fromTreeUri(context, this)
		if (df?.canWrite() != true) {
			return context.getString(R.string.invalid_value_message)
		}
		return resolveFile(context)?.path ?: toString()
	}
}

