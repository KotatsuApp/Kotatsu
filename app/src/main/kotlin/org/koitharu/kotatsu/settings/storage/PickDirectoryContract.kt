package org.koitharu.kotatsu.settings.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

//FIXME: https://stackoverflow.com/questions/77555641/saf-no-activity-found-to-handle-intent-android-intent-action-open-document-tr
class PickDirectoryContract : ActivityResultContracts.OpenDocumentTree() {

	override fun createIntent(context: Context, input: Uri?): Intent {
		val intent = super.createIntent(context, input)
		intent.addFlags(
			Intent.FLAG_GRANT_READ_URI_PERMISSION
				or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
				or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
		)
		return intent
	}
}
