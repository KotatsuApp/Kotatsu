package org.koitharu.kotatsu.core.exceptions.resolve

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.util.ext.findActivity
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope

abstract class ErrorObserver(
	protected val host: View,
	protected val fragment: Fragment?,
	private val resolver: ExceptionResolver?,
	private val onResolved: Consumer<Boolean>?,
) : FlowCollector<Throwable> {

	protected open val activity = host.context.findActivity()

	private val lifecycleScope: LifecycleCoroutineScope
		get() = checkNotNull(fragment?.viewLifecycleScope ?: (activity as? LifecycleOwner)?.lifecycle?.coroutineScope)

	protected val fragmentManager: FragmentManager?
		get() = fragment?.childFragmentManager ?: (activity as? AppCompatActivity)?.supportFragmentManager

	protected fun canResolve(error: Throwable): Boolean {
		return resolver != null && ExceptionResolver.canResolve(error)
	}

	private fun isAlive(): Boolean {
		return when {
			fragment != null -> fragment.view != null
			activity != null -> activity?.isDestroyed == false
			else -> true
		}
	}

	protected fun resolve(error: Throwable) {
		if (isAlive()) {
			lifecycleScope.launch {
				val isResolved = resolver?.resolve(error) ?: false
				if (isActive) {
					onResolved?.accept(isResolved)
				}
			}
		}
	}
}
