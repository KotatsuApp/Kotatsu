package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.base.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver

abstract class BaseFragment<B : ViewBinding> : Fragment(),
	WindowInsetsDelegate.WindowInsetsListener {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	@Suppress("LeakingThis")
	protected val exceptionResolver = ExceptionResolver(this)

	@Suppress("LeakingThis")
	protected val insetsDelegate = WindowInsetsDelegate(this)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val binding = onInflateView(inflater, container)
		viewBinding = binding
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		insetsDelegate.onViewCreated(view)
	}

	override fun onDestroyView() {
		viewBinding = null
		insetsDelegate.onDestroyView()
		super.onDestroyView()
	}

	protected fun bindingOrNull() = viewBinding

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B
}
