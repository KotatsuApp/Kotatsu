package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_list_mode.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode

class ListModeSelectDialog : AlertDialogFragment(R.layout.dialog_list_mode), View.OnClickListener,
	SeekBar.OnSeekBarChangeListener {

	private val settings by inject<AppSettings>()

	private var mode: ListMode = ListMode.GRID
	private var pendingGridSize: Int = 100

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = settings.listMode
		pendingGridSize = settings.gridSize
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.list_mode)
			.setPositiveButton(R.string.done, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		button_list.isChecked = mode == ListMode.LIST
		button_list_detailed.isChecked = mode == ListMode.DETAILED_LIST
		button_grid.isChecked = mode == ListMode.GRID

		with(seekbar_grid) {
			progress = pendingGridSize - 50
			setOnSeekBarChangeListener(this@ListModeSelectDialog)
		}

		button_list.setOnClickListener(this)
		button_grid.setOnClickListener(this)
		button_list_detailed.setOnClickListener(this)
	}

	override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
		pendingGridSize = progress + 50
	}

	override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

	override fun onStopTrackingTouch(seekBar: SeekBar?) {
		settings.gridSize = pendingGridSize
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_list -> mode = ListMode.LIST
			R.id.button_list_detailed -> mode = ListMode.DETAILED_LIST
			R.id.button_grid -> mode = ListMode.GRID
		}
		settings.listMode = mode
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