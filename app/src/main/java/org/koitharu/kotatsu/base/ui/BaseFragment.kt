package org.koitharu.kotatsu.base.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver

abstract class BaseFragment<B : ViewBinding> : Fragment(), OnApplyWindowInsetsListener {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	@Suppress("LeakingThis")
	protected val exceptionResolver = ExceptionResolver(this)

	private var lastInsets: Insets? = null

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
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
		view.doOnNextLayout {
			// Listener may not be called
			if (lastInsets == null) {
				onWindowInsetsChanged(Insets.NONE)
			}
		}
	}

	override fun onDestroyView() {
		viewBinding = null
		lastInsets = null
		super.onDestroyView()
	}

	open fun getTitle(): CharSequence? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		getTitle()?.let {
			activity?.title = it
		}
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
