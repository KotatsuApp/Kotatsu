package org.koitharu.kotatsu.base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koitharu.kotatsu.utils.UiUtils

abstract class BaseBottomSheet(@LayoutRes private val layoutResId: Int) :
	BottomSheetDialogFragment() {

	final override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? = inflater.inflate(layoutResId, container, false)

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return if (UiUtils.isTablet(requireContext())) {
			AppCompatDialog(context, theme)
		} else super.onCreateDialog(savedInstanceState)
	}
}