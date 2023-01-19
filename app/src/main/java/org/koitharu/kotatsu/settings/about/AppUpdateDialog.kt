package org.koitharu.kotatsu.settings.about

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.material.R as materialR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.Markwon
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.utils.FileSize

class AppUpdateDialog(private val context: Context) {

	fun show(version: AppVersion) {
		val message = buildString {
			append(context.getString(R.string.new_version_s, version.name))
			appendLine()
			append(context.getString(R.string.size_s, FileSize.BYTES.format(context, version.apkSize)))
			appendLine()
			appendLine()
			append(version.description)
		}
		MaterialAlertDialogBuilder(
			context,
			materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered,
		)
			.setTitle(R.string.app_update_available)
			.setMessage(Markwon.create(context).toMarkdown(message))
			.setIcon(R.drawable.ic_app_update)
			.setPositiveButton(R.string.download) { _, _ ->
				val intent = Intent(Intent.ACTION_VIEW, version.apkUrl.toUri())
				context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_in_browser)))
			}
			.setNegativeButton(R.string.close, null)
			.setCancelable(false)
			.create()
			.show()
	}
}
