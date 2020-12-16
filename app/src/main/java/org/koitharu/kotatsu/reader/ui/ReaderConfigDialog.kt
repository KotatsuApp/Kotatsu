package org.koitharu.kotatsu.reader.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.databinding.DialogReaderConfigBinding
import org.koitharu.kotatsu.utils.ext.withArgs

class ReaderConfigDialog : AlertDialogFragment<DialogReaderConfigBinding>(),
	View.OnClickListener {

	private lateinit var mode: ReaderMode

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = DialogReaderConfigBinding.inflate(inflater, container, false)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = arguments?.getInt(ARG_MODE)
			?.let { ReaderMode.valueOf(it) }
			?: ReaderMode.STANDARD
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.read_mode)
			.setPositiveButton(R.string.done, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonStandard.isChecked = mode == ReaderMode.STANDARD
		binding.buttonReversed.isChecked = mode == ReaderMode.REVERSED
		binding.buttonWebtoon.isChecked = mode == ReaderMode.WEBTOON

		binding.buttonStandard.setOnClickListener(this)
		binding.buttonReversed.setOnClickListener(this)
		binding.buttonWebtoon.setOnClickListener(this)
	}

	override fun onDismiss(dialog: DialogInterface) {
		((parentFragment as? Callback)
			?: (activity as? Callback))?.onReaderModeChanged(mode)
		super.onDismiss(dialog)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_standard -> mode = ReaderMode.STANDARD
			R.id.button_webtoon -> mode = ReaderMode.WEBTOON
			R.id.button_reversed -> mode = ReaderMode.REVERSED
		}
	}

	interface Callback {

		fun onReaderModeChanged(mode: ReaderMode)
	}

	companion object {

		private const val TAG = "ReaderConfigDialog"
		private const val ARG_MODE = "mode"

		fun show(fm: FragmentManager, mode: ReaderMode) = ReaderConfigDialog().withArgs(1) {
			putInt(ARG_MODE, mode.id)
		}.show(fm, TAG)
	}
}