package org.koitharu.kotatsu.core.os

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityOptionsCompat
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

// https://stackoverflow.com/questions/77555641/saf-no-activity-found-to-handle-intent-android-intent-action-open-document-tr
class OpenDocumentTreeHelper(
	activityResultCaller: ActivityResultCaller,
	flags: Int,
	callback: ActivityResultCallback<Uri?>
) : ActivityResultLauncher<Uri?>() {

	constructor(activityResultCaller: ActivityResultCaller, callback: ActivityResultCallback<Uri?>) : this(
		activityResultCaller,
		0,
		callback,
	)

	private val pickFileTreeLauncherQ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		activityResultCaller.registerForActivityResult(OpenDocumentTreeContractQ(flags), callback)
	} else {
		null
	}
	private val pickFileTreeLauncherLegacy = activityResultCaller.registerForActivityResult(
		contract = OpenDocumentTreeContractLegacy(flags),
		callback = callback,
	)

	override fun launch(input: Uri?, options: ActivityOptionsCompat?) {
		if (pickFileTreeLauncherQ == null) {
			pickFileTreeLauncherLegacy.launch(input, options)
			return
		}
		try {
			pickFileTreeLauncherQ.launch(input, options)
		} catch (e: Exception) {
			e.printStackTraceDebug()
			pickFileTreeLauncherLegacy.launch(input, options)
		}
	}

	override fun unregister() {
		pickFileTreeLauncherQ?.unregister()
		pickFileTreeLauncherLegacy.unregister()
	}

	override val contract: ActivityResultContract<Uri?, *>
		get() = pickFileTreeLauncherQ?.contract ?: pickFileTreeLauncherLegacy.contract

	private open class OpenDocumentTreeContractLegacy(
		private val flags: Int,
	) : ActivityResultContracts.OpenDocumentTree() {

		override fun createIntent(context: Context, input: Uri?): Intent {
			val intent = super.createIntent(context, input)
			intent.addFlags(flags)
			return intent
		}
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private class OpenDocumentTreeContractQ(
		private val flags: Int,
	) : OpenDocumentTreeContractLegacy(flags) {

		override fun createIntent(context: Context, input: Uri?): Intent {
			val intent = (context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager)
				?.primaryStorageVolume
				?.createOpenDocumentTreeIntent()
			if (intent == null) { // fallback
				return super.createIntent(context, input)
			}
			intent.addFlags(flags)
			if (input != null) {
				intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
			}
			return intent
		}
	}
}
