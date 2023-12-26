package org.koitharu.kotatsu.settings.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.powerManager

@SuppressLint("BatteryLife")
class DozeHelper(
	private val fragment: PreferenceFragmentCompat,
) {

	private val startForDozeResult = fragment.registerForActivityResult(
		ActivityResultContracts.StartActivityForResult(),
	) {
		updatePreference()
	}

	fun updatePreference() {
		val preference = fragment.findPreference<Preference>(AppSettings.KEY_IGNORE_DOZE) ?: return
		preference.isVisible = isDozeIgnoreAvailable()
	}

	fun startIgnoreDoseActivity(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			Snackbar.make(fragment.listView ?: return false, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
			return false
		}
		val context = fragment.context ?: return false
		val packageName = context.packageName
		val powerManager = context.powerManager ?: return false
		return if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
			try {
				val intent = Intent(
					Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
					"package:$packageName".toUri(),
				)
				startForDozeResult.launch(intent)
				true
			} catch (e: ActivityNotFoundException) {
				Snackbar.make(fragment.listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
				false
			}
		} else {
			false
		}
	}

	private fun isDozeIgnoreAvailable(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return false
		}
		val context = fragment.context ?: return false
		val packageName = context.packageName
		val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
		return !powerManager.isIgnoringBatteryOptimizations(packageName)
	}
}
