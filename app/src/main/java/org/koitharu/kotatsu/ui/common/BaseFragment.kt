package org.koitharu.kotatsu.ui.common

import android.content.Context
import android.os.Parcelable
import androidx.annotation.LayoutRes
import moxy.MvpAppCompatFragment
import org.koitharu.kotatsu.utils.delegates.ParcelableArgumentDelegate
import org.koitharu.kotatsu.utils.delegates.StringArgumentDelegate

abstract class BaseFragment(@LayoutRes contentLayoutId: Int) :
	MvpAppCompatFragment(contentLayoutId) {

	fun stringArg(name: String) = StringArgumentDelegate(name)

	fun <T : Parcelable> arg(name: String) = ParcelableArgumentDelegate<T>(name)

	open fun getTitle(): CharSequence? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		getTitle()?.let {
			activity?.title = it
		}
	}
}