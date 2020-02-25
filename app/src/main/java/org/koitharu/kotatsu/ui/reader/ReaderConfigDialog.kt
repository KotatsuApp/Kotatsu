package org.koitharu.kotatsu.ui.reader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.AlertDialogFragment

class ReaderConfigDialog : AlertDialogFragment(R.layout.dialog_reader_config),
	View.OnClickListener {

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder//.setTitle(R.string.list_mode)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_ok -> dismiss()

		}
	}

	companion object {

		private const val TAG = "ReaderConfigDialog"

		fun show(fm: FragmentManager) = ReaderConfigDialog().show(fm, TAG)
	}
}