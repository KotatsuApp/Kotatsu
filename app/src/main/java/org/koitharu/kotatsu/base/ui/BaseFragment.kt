package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver

abstract class BaseFragment<B : ViewBinding> : Fragment(), OnApplyWindowInsetsListener {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	@Suppress("LeakingThis")
	protected val exceptionResolver = ExceptionResolver(this)

	private var lastInsets: Insets = Insets.NONE

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
		lastInsets = Insets.NONE
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	override fun onApplyWindowInsets(v: View?, insets: WindowInsetsCompat): WindowInsetsCompat {
		val newInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		if (newInsets != lastInsets) {
			onWindowInsetsChanged(newInsets)
			lastInsets = newInsets
		}
		return insets
	}

	protected fun bindingOrNull() = viewBinding

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B

	protected abstract fun onWindowInsetsChanged(insets: Insets)
}
