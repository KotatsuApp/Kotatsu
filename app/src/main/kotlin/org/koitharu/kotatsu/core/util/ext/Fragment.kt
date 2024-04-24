package org.koitharu.kotatsu.core.util.ext

import android.os.Bundle
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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

fun DialogFragment.showAllowStateLoss(manager: FragmentManager, tag: String?) {
	if (!manager.isStateSaved) {
		show(manager, tag)
	}
}

fun Fragment.addMenuProvider(provider: MenuProvider) {
	requireActivity().addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.RESUMED)
}

fun DialogFragment.showDistinct(fm: FragmentManager, tag: String) {
	val existing = fm.findFragmentByTag(tag) as? DialogFragment?
	if (existing != null && existing.isVisible && existing.arguments == this.arguments) {
		return
	}
	show(fm, tag)
}

tailrec fun Fragment.dismissParentDialog(): Boolean {
	return when (val parent = parentFragment) {
		null -> return false
		is DialogFragment -> {
			parent.dismiss()
			true
		}

		else -> parent.dismissParentDialog()
	}
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
