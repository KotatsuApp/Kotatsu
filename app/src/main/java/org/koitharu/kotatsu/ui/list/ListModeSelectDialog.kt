package org.koitharu.kotatsu.ui.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_list_mode.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.base.AlertDialogFragment

class ListModeSelectDialog : AlertDialogFragment(R.layout.dialog_list_mode), View.OnClickListener {

	private val settings by inject<AppSettings>()

	private lateinit var mode: ListMode

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = settings.listMode
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.list_mode)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		button_list.isChecked = mode == ListMode.LIST
		button_list_detailed.isChecked = mode == ListMode.DETAILED_LIST
		button_grid.isChecked = mode == ListMode.GRID

		button_ok.setOnClickListener(this)
		button_list.setOnClickListener(this)
		button_grid.setOnClickListener(this)
		button_list_detailed.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_ok -> {
				settings.listMode = mode
				dismiss()
			}
			R.id.button_list -> mode = ListMode.LIST
			R.id.button_list_detailed -> mode = ListMode.DETAILED_LIST
			R.id.button_grid -> mode = ListMode.GRID
		}
	}

	companion object {

		private const val TAG = "ListModeSelectDialog"

		fun show(fm: FragmentManager) = ListModeSelectDialog()
			.show(
				fm,
				TAG
			)
	}
}