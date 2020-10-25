package org.koitharu.kotatsu.ui.reader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_list_mode.button_ok
import kotlinx.android.synthetic.main.dialog_reader_config.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.ui.base.AlertDialogFragment
import org.koitharu.kotatsu.utils.ext.withArgs

class ReaderConfigDialog : AlertDialogFragment(R.layout.dialog_reader_config),
	View.OnClickListener {

	private lateinit var mode: ReaderMode

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = arguments?.getInt(ARG_MODE, ReaderMode.UNKNOWN.id)
			?.let { ReaderMode.valueOf(it) }
			?.takeUnless { it == ReaderMode.UNKNOWN }
			?: ReaderMode.STANDARD
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.read_mode)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		button_standard.isChecked = mode == ReaderMode.STANDARD
		button_reversed.isChecked = mode == ReaderMode.REVERSED
		button_webtoon.isChecked = mode == ReaderMode.WEBTOON

		button_ok.setOnClickListener(this)
		button_standard.setOnClickListener(this)
		button_reversed.setOnClickListener(this)
		button_webtoon.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_ok -> {
				((parentFragment as? Callback)
					?: (activity as? Callback))?.onReaderModeChanged(mode)
				dismiss()
			}
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