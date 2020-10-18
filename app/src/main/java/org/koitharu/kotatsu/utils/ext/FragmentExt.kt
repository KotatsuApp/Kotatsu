package org.koitharu.kotatsu.utils.ext

import android.os.Bundle
import androidx.fragment.app.Fragment

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
	val b = Bundle(size)
	b.block()
	this.arguments = b
	return this
}