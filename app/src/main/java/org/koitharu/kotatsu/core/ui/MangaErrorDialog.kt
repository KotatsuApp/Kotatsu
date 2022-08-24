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
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.DialogMangaErrorBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.report
import org.koitharu.kotatsu.utils.ext.requireParcelable
import org.koitharu.kotatsu.utils.ext.requireSerializable
import org.koitharu.kotatsu.utils.ext.withArgs

class MangaErrorDialog : AlertDialogFragment<DialogMangaErrorBinding>() {

	private lateinit var error: Throwable
	private lateinit var manga: Manga

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val args = requireArguments()
		manga = args.requireParcelable<ParcelableManga>(ARG_MANGA).manga
		error = args.requireSerializable(ARG_ERROR)
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
				this@MangaErrorDialog.error.message?.htmlEncode().orEmpty(),
				manga.publicUrl,
			).parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY)
		}
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.report) { _, _ ->
				dismiss()
				error.report(TAG)
			}.setTitle(R.string.error_occurred)
	}

	companion object {

		private const val TAG = "MangaErrorDialog"
		private const val ARG_ERROR = "error"
		private const val ARG_MANGA = "manga"

		fun show(fm: FragmentManager, manga: Manga, error: Throwable) = MangaErrorDialog().withArgs(2) {
			putParcelable(ARG_MANGA, ParcelableManga(manga, false))
			putSerializable(ARG_ERROR, error)
		}.show(fm, TAG)
	}
}
