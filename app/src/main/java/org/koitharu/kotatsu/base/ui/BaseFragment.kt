package org.koitharu.kotatsu.base.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<B : ViewBinding> : Fragment() {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val binding = onInflateView(inflater, container)
		viewBinding = binding
		return binding.root
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	open fun getTitle(): CharSequence? = null

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B

	override fun onAttach(context: Context) {
		super.onAttach(context)
		getTitle()?.let {
			activity?.title = it
		}
	}
}