package org.koitharu.kotatsu.core.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.htmlEncode
import androidx.core.text.parseAsHtml
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.databinding.DialogMangaErrorBinding
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.utils.ext.report
import org.koitharu.kotatsu.utils.ext.requireSerializable
import org.koitharu.kotatsu.utils.ext.withArgs

class MangaErrorDialog : AlertDialogFragment<DialogMangaErrorBinding>() {

	private lateinit var exception: ParseException

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val args = requireArguments()
		exception = args.requireSerializable(ARG_ERROR)
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): DialogMangaErrorBinding {
		return DialogMangaErrorBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		with(binding.textViewMessage) {
			movementMethod = LinkMovementMethod.getInstance()
			text = context.getString(
				R.string.manga_error_description_pattern,
				exception.message?.htmlEncode().orEmpty(),
				exception.url,
			).parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY)
		}
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.report) { _, _ ->
				dismiss()
				exception.report()
			}.setTitle(R.string.error_occurred)
	}

	companion object {

		private const val TAG = "MangaErrorDialog"
		private const val ARG_ERROR = "error"

		fun show(fm: FragmentManager, error: ParseException) = MangaErrorDialog().withArgs(1) {
			putSerializable(ARG_ERROR, error)
		}.show(fm, TAG)
	}
}
