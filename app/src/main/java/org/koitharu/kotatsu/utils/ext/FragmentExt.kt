package org.koitharu.kotatsu.utils.ext

import android.os.Bundle
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import java.io.Serializable
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
	val b = Bundle(size)
	b.block()
	this.arguments = b
	return this
}

val Fragment.viewLifecycleScope
	inline get() = viewLifecycleOwner.lifecycle.coroutineScope

fun <T : Serializable> Fragment.serializableArgument(name: String): Lazy<T> {
	return lazy(LazyThreadSafetyMode.NONE) {
		@Suppress("UNCHECKED_CAST")
		requireNotNull(arguments?.getSerializableCompat(name)) {
			"No argument $name passed into ${javaClass.simpleName}"
		} as T
	}
}

fun Fragment.stringArgument(name: String) = lazy(LazyThreadSafetyMode.NONE) {
	arguments?.getString(name)
}

fun DialogFragment.showAllowStateLoss(manager: FragmentManager, tag: String?) {
	if (!manager.isStateSaved) {
		show(manager, tag)
	}
}

fun Fragment.addMenuProvider(provider: MenuProvider) {
	requireActivity().addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.STARTED)
}

suspend fun Fragment.awaitViewLifecycle(): LifecycleOwner = suspendCancellableCoroutine { cont ->
	val liveData = viewLifecycleOwnerLiveData
	val observer = object : Observer<LifecycleOwner> {
		override fun onChanged(result: LifecycleOwner?) {
			if (result != null) {
				liveData.removeObserver(this)
				cont.resume(result)
			}
		}
	}
	liveData.observeForever(observer)
	cont.invokeOnCancellation {
		liveData.removeObserver(observer)
	}
}
