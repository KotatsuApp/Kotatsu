package org.koitharu.kotatsu.core.exceptions.resolve

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.utils.ext.findActivity
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

abstract class ErrorObserver(
	protected val host: View,
	protected val fragment: Fragment?,
	private val resolver: ExceptionResolver?,
	private val onResolved: Consumer<Boolean>?,
) : Observer<Throwable> {

	protected val activity = host.context.findActivity()

	private val lifecycleScope: LifecycleCoroutineScope
		get() = checkNotNull(fragment?.viewLifecycleScope ?: (activity as? LifecycleOwner)?.lifecycle?.coroutineScope)

	protected val fragmentManager: FragmentManager?
		get() = fragment?.childFragmentManager ?: (activity as? AppCompatActivity)?.supportFragmentManager

	protected fun canResolve(error: Throwable): Boolean {
		return resolver != null && ExceptionResolver.canResolve(error)
	}

	protected fun resolve(error: Throwable) {
		lifecycleScope.launch {
			val isResolved = resolver?.resolve(error) ?: false
			if (isActive) {
				onResolved?.accept(isResolved)
			}
		}
	}
}
