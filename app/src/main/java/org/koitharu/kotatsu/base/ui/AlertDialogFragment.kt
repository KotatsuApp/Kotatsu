package org.koitharu.kotatsu.base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

abstract class AlertDialogFragment(
	@LayoutRes private val layoutResId: Int
) : DialogFragment() {

	private var rootView: View? = null

	final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val view = activity?.layoutInflater?.inflate(layoutResId, null)
		rootView = view
		if (view != null) {
			onViewCreated(view, savedInstanceState)
		}
		return AlertDialog.Builder(requireContext(), theme)
			.setView(view)
			.also(::onBuildDialog)
			.create()
	}

	@CallSuper
	override fun onDestroyView() {
		rootView = null
		super.onDestroyView()
	}

	final override fun getView(): View? {
		return rootView
	}

	open fun onBuildDialog(builder: AlertDialog.Builder) = Unit
}