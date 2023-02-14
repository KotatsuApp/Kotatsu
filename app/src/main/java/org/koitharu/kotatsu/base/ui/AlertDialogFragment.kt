package org.koitharu.kotatsu.base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class AlertDialogFragment<B : ViewBinding> : DialogFragment() {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val binding = onInflateView(layoutInflater, null)
		viewBinding = binding
		return MaterialAlertDialogBuilder(requireContext(), theme)
			.setView(binding.root)
			.run(::onBuildDialog)
			.create()
	}

	final override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = viewBinding?.root

	@CallSuper
	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	open fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder = builder

	open fun onDialogCreated(dialog: AlertDialog) = Unit

	protected fun bindingOrNull(): B? = viewBinding

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B
}
