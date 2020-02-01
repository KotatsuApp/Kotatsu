package org.koitharu.kotatsu.ui.main.list

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_list_mode.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode

class ListModeSelectDialog : DialogFragment(), View.OnClickListener {

	private val setting by inject<AppSettings>()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.dialog_list_mode, container, false)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return super.onCreateDialog(savedInstanceState).apply {
			setTitle(R.string.list_mode)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val mode = setting.listMode
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
			R.id.button_ok -> dismiss()
			R.id.button_list -> setting.listMode = ListMode.LIST
			R.id.button_list_detailed -> setting.listMode = ListMode.DETAILED_LIST
			R.id.button_grid -> setting.listMode = ListMode.GRID
		}
	}

	companion object {

		private const val TAG = "LIST_MODE"

		fun show(fm: FragmentManager) = ListModeSelectDialog().show(fm, TAG)
	}
}