package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.databinding.DialogListModeBinding

class ListModeSelectDialog : AlertDialogFragment<DialogListModeBinding>(), View.OnClickListener,
	SeekBar.OnSeekBarChangeListener {

	private val settings by inject<AppSettings>(mode = LazyThreadSafetyMode.NONE)

	private var mode: ListMode = ListMode.GRID
	private var pendingGridSize: Int = 100

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = settings.listMode
		pendingGridSize = settings.gridSize
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = DialogListModeBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.list_mode)
			.setPositiveButton(R.string.done, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonList.isChecked = mode == ListMode.LIST
		binding.buttonListDetailed.isChecked = mode == ListMode.DETAILED_LIST
		binding.buttonGrid.isChecked = mode == ListMode.GRID
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.seekbarGrid.isVisible = mode == ListMode.GRID

		with(binding.seekbarGrid) {
			progress = pendingGridSize - 50
			setOnSeekBarChangeListener(this@ListModeSelectDialog)
		}

		binding.buttonList.setOnClickListener(this)
		binding.buttonGrid.setOnClickListener(this)
		binding.buttonListDetailed.setOnClickListener(this)
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
		binding.textViewGridTitle.isVisible = mode == ListMode.GRID
		binding.seekbarGrid.isVisible = mode == ListMode.GRID
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