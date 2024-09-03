package org.koitharu.kotatsu.core.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.EntryPointAccessors
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.ui.util.ActionModeDelegate
import org.koitharu.kotatsu.core.ui.util.WindowInsetsDelegate

abstract class BaseFragment<B : ViewBinding> :
	Fragment(),
	ExceptionResolver.Host,
	WindowInsetsDelegate.WindowInsetsListener {

	var viewBinding: B? = null
		private set

	protected lateinit var exceptionResolver: ExceptionResolver
		private set

	@JvmField
	protected val insetsDelegate = WindowInsetsDelegate()

	protected val actionModeDelegate: ActionModeDelegate
		get() = (requireActivity() as BaseActivity<*>).actionModeDelegate

	override fun onAttach(context: Context) {
		super.onAttach(context)
		val entryPoint = EntryPointAccessors.fromApplication<BaseActivityEntryPoint>(context)
		exceptionResolver = entryPoint.exceptionResolverFactory.create(this)
	}

	final override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val binding = onCreateViewBinding(inflater, container)
		viewBinding = binding
		return binding.root
	}

	final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		insetsDelegate.onViewCreated(view)
		insetsDelegate.addInsetsListener(this)
		onViewBindingCreated(requireViewBinding(), savedInstanceState)
	}

	override fun onDestroyView() {
		viewBinding = null
		insetsDelegate.removeInsetsListener(this)
		insetsDelegate.onDestroyView()
		super.onDestroyView()
	}

	fun requireViewBinding(): B = checkNotNull(viewBinding) {
		"Fragment $this did not return a ViewBinding from onCreateView() or this was called before onCreateView()."
	}

	protected abstract fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

	protected open fun onViewBindingCreated(binding: B, savedInstanceState: Bundle?) = Unit
}
