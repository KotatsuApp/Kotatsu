package org.koitharu.kotatsu.base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class AlertDialogFragment<B : ViewBinding> : DialogFragment() {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val inflater = activity?.layoutInflater ?: LayoutInflater.from(requireContext())
		val binding = onInflateView(inflater, null)
		viewBinding = binding
		onViewCreated(binding.root, savedInstanceState)
		return AlertDialog.Builder(requireContext(), theme)
			.setView(binding.root)
			.also(::onBuildDialog)
			.create()
	}

	@CallSuper
	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	final override fun getView(): View? {
		return viewBinding?.root
	}

	open fun onBuildDialog(builder: AlertDialog.Builder) = Unit

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B
}