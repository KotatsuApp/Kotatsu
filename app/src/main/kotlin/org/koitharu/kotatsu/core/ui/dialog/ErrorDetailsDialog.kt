package org.koitharu.kotatsu.core.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.htmlEncode
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.text.parseAsHtml
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.util.ext.isReportable
import org.koitharu.kotatsu.core.util.ext.report
import org.koitharu.kotatsu.core.util.ext.requireSerializable
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.DialogErrorDetailsBinding

class ErrorDetailsDialog : AlertDialogFragment<DialogErrorDetailsBinding>() {

	private lateinit var exception: Throwable

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val args = requireArguments()
		exception = args.requireSerializable(ARG_ERROR)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogErrorDetailsBinding {
		return DialogErrorDetailsBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogErrorDetailsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		with(binding.textViewMessage) {
			movementMethod = LinkMovementMethodCompat.getInstance()
			text = context.getString(
				R.string.manga_error_description_pattern,
				exception.message?.htmlEncode().orEmpty(),
				arguments?.getString(ARG_URL),
			).parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY)
		}
	}

	@Suppress("NAME_SHADOWING")
	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		val builder = super.onBuildDialog(builder)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setTitle(R.string.error_occurred)
			.setNeutralButton(androidx.preference.R.string.copy) { _, _ ->
				copyToClipboard()
			}
		if (exception.isReportable()) {
			builder.setPositiveButton(R.string.report) { _, _ ->
				dismiss()
				exception.report()
			}
		}
		return builder
	}

	private fun copyToClipboard() {
		val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
			?: return
		clipboardManager.setPrimaryClip(
			ClipData.newPlainText(getString(R.string.error), exception.stackTraceToString()),
		)
	}

	companion object {

		private const val TAG = "ErrorDetailsDialog"
		private const val ARG_ERROR = "error"
		private const val ARG_URL = "url"

		fun show(fm: FragmentManager, error: Throwable, url: String?) = ErrorDetailsDialog().withArgs(2) {
			putSerializable(ARG_ERROR, error)
			putString(ARG_URL, url)
		}.show(fm, TAG)
	}
}
