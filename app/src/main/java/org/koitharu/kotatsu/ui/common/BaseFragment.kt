package org.koitharu.kotatsu.ui.common

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import moxy.MvpAppCompatFragment
import org.koitharu.kotatsu.utils.delegates.ParcelableArgumentDelegate
import org.koitharu.kotatsu.utils.delegates.StringArgumentDelegate

abstract class BaseFragment(@LayoutRes contentLayoutId: Int) :
	MvpAppCompatFragment(contentLayoutId) {

	fun stringArg(name: String) = StringArgumentDelegate(name)

	fun <T : Parcelable> arg(name: String) = ParcelableArgumentDelegate<T>(name)
}