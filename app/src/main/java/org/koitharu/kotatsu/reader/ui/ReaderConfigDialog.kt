package org.koitharu.kotatsu.reader.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.base.ui.widgets.CheckableButtonGroup
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.databinding.DialogReaderConfigBinding
import org.koitharu.kotatsu.utils.ext.withArgs

class ReaderConfigDialog : AlertDialogFragment<DialogReaderConfigBinding>(),
	CheckableButtonGroup.OnCheckedChangeListener {

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

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder) {
		builder.setTitle(R.string.read_mode)
			.setPositiveButton(R.string.done, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonStandard.isChecked = mode == ReaderMode.STANDARD
		binding.buttonReversed.isChecked = mode == ReaderMode.REVERSED
		binding.buttonWebtoon.isChecked = mode == ReaderMode.WEBTOON

		binding.checkableGroup.onCheckedChangeListener = this
	}

	override fun onDismiss(dialog: DialogInterface) {
		((parentFragment as? Callback)
			?: (activity as? Callback))?.onReaderModeChanged(mode)
		super.onDismiss(dialog)
	}

	override fun onCheckedChanged(group: CheckableButtonGroup, checkedId: Int) {
		mode = when (checkedId) {
			R.id.button_standard -> ReaderMode.STANDARD
			R.id.button_webtoon -> ReaderMode.WEBTOON
			R.id.button_reversed -> ReaderMode.REVERSED
			else -> return
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