package org.koitharu.kotatsu.base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koitharu.kotatsu.utils.UiUtils

abstract class BaseBottomSheet<B : ViewBinding> :
	BottomSheetDialogFragment() {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	final override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val binding = onInflateView(inflater, container)
		viewBinding = binding
		return binding.root
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return if (UiUtils.isTablet(requireContext())) {
			AppCompatDialog(context, theme)
		} else super.onCreateDialog(savedInstanceState)
	}

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B
}