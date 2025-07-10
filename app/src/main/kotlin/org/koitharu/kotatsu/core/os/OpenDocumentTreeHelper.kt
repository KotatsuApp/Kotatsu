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

	private val pickFileTreeLauncherPrimaryStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		activityResultCaller.registerForActivityResult(OpenDocumentTreeContractPrimaryStorage(flags), callback)
	} else {
		null
	}
	private val pickFileTreeLauncherDefault = activityResultCaller.registerForActivityResult(
		contract = OpenDocumentTreeContractDefault(flags),
		callback = callback,
	)

	override fun launch(input: Uri?, options: ActivityOptionsCompat?) {
		try {
			pickFileTreeLauncherDefault.launch(input, options)
		} catch (e: Exception) {
			if (pickFileTreeLauncherPrimaryStorage != null) {
				try {
					pickFileTreeLauncherPrimaryStorage.launch(input, options)
				} catch (e2: Exception) {
					e.addSuppressed(e2)
					throw e
				}
			} else {
				throw e
			}
		}
	}

	override fun unregister() {
		pickFileTreeLauncherPrimaryStorage?.unregister()
		pickFileTreeLauncherDefault.unregister()
	}

	override val contract: ActivityResultContract<Uri?, *>
		get() = pickFileTreeLauncherPrimaryStorage?.contract ?: pickFileTreeLauncherDefault.contract

	private open class OpenDocumentTreeContractDefault(
		private val flags: Int,
	) : ActivityResultContracts.OpenDocumentTree() {

		override fun createIntent(context: Context, input: Uri?): Intent {
			val intent = super.createIntent(context, input)
			intent.addFlags(flags)
			return intent
		}
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private class OpenDocumentTreeContractPrimaryStorage(
		private val flags: Int,
	) : OpenDocumentTreeContractDefault(flags) {

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
