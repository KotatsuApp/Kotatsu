package org.koitharu.kotatsu.core.util.ext

import android.os.Bundle
import androidx.core.view.MenuProvider
import androidx.core.view.ancestors
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
	val b = Bundle(size)
	b.block()
	this.arguments = b
	return this
}

val Fragment.viewLifecycleScope
	inline get() = viewLifecycleOwner.lifecycle.coroutineScope

fun Fragment.addMenuProvider(provider: MenuProvider) {
	requireActivity().addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.RESUMED)
}

@Suppress("UNCHECKED_CAST")
tailrec fun <T> Fragment.findParentCallback(cls: Class<T>): T? {
	val parent = parentFragment
	return when {
		parent == null -> cls.castOrNull(activity)
		cls.isInstance(parent) -> parent as T
		else -> parent.findParentCallback(cls)
	}
}

val Fragment.container: FragmentContainerView?
	get() = view?.ancestors?.firstNotNullOfOrNull {
		it as? FragmentContainerView // TODO check if direct parent
	}
