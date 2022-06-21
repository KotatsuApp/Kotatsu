package org.koitharu.kotatsu.utils.ext

import android.os.Bundle
import android.os.Parcelable
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import java.io.Serializable

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
	val b = Bundle(size)
	b.block()
	this.arguments = b
	return this
}

val Fragment.viewLifecycleScope
	inline get() = viewLifecycleOwner.lifecycle.coroutineScope

fun <T : Parcelable> Fragment.parcelableArgument(name: String): Lazy<T> {
	return lazy(LazyThreadSafetyMode.NONE) {
		requireNotNull(arguments?.getParcelable(name)) {
			"No argument $name passed into ${javaClass.simpleName}"
		}
	}
}

fun <T : Serializable> Fragment.serializableArgument(name: String): Lazy<T> {
	return lazy(LazyThreadSafetyMode.NONE) {
		@Suppress("UNCHECKED_CAST")
		requireNotNull(arguments?.getSerializable(name)) {
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
	requireActivity().addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.RESUMED)
}