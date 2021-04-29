package org.koitharu.kotatsu.utils.ext

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
	val b = Bundle(size)
	b.block()
	this.arguments = b
	return this
}

val Fragment.viewLifecycleScope
	inline get() = viewLifecycleOwner.lifecycle.coroutineScope

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Parcelable> Fragment.parcelableArgument(name: String): Lazy<T> {
	return lazy(LazyThreadSafetyMode.NONE) {
		requireNotNull(arguments?.getParcelable(name)) {
			"No argument $name passed into ${javaClass.simpleName}"
		}
	}
}

@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.stringArgument(name: String) = lazy(LazyThreadSafetyMode.NONE) {
	arguments?.getString(name)
}