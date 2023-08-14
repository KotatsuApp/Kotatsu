package org.koitharu.kotatsu.core.util.ext

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
	val b = Bundle(size)
	b.block()
	this.arguments = b
	return this
}

val Fragment.viewLifecycleScope
	inline get() = viewLifecycleOwner.lifecycle.coroutineScope

fun DialogFragment.showAllowStateLoss(manager: FragmentManager, tag: String?) {
	if (!manager.isStateSaved) {
		show(manager, tag)
	}
}

fun Fragment.addMenuProvider(provider: MenuProvider) {
	requireActivity().addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.RESUMED)
}

@MainThread
suspend fun Fragment.awaitViewLifecycle(): LifecycleOwner {
	val liveData = viewLifecycleOwnerLiveData
	liveData.value?.let { return it }
	return suspendCancellableCoroutine { cont ->
		val observer = object : Observer<LifecycleOwner?> {
			override fun onChanged(value: LifecycleOwner?) {
				if (value != null) {
					liveData.removeObserver(this)
					cont.resume(value)
				}
			}
		}
		liveData.observeForever(observer)
		cont.invokeOnCancellation {
			liveData.removeObserver(observer)
		}
	}
}

fun DialogFragment.showDistinct(fm: FragmentManager, tag: String) {
	val existing = fm.findFragmentByTag(tag) as? DialogFragment?
	if (existing != null && existing.isVisible && existing.arguments == this.arguments) {
		return
	}
	show(fm, tag)
}
