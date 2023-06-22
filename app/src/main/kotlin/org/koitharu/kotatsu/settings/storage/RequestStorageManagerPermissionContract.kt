package org.koitharu.kotatsu.settings.storage

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.core.net.toUri


@RequiresApi(Build.VERSION_CODES.R)
class RequestStorageManagerPermissionContract : ActivityResultContract<String, Boolean>() {

	override fun createIntent(context: Context, input: String): Intent {
		val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
		intent.addCategory("android.intent.category.DEFAULT")
		intent.data = "package:${context.packageName}".toUri()
		return intent
	}

	override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
		return Environment.isExternalStorageManager()
	}

	override fun getSynchronousResult(context: Context, input: String): SynchronousResult<Boolean>? {
		return if (Environment.isExternalStorageManager()) {
			SynchronousResult(true)
		} else {
			null
		}
	}
}
