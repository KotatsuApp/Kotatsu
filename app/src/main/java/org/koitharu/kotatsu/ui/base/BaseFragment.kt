package org.koitharu.kotatsu.ui.base

import android.content.Context
import android.os.Parcelable
import androidx.annotation.LayoutRes
import coil.ImageLoader
import moxy.MvpAppCompatFragment
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.utils.delegates.ParcelableArgumentDelegate
import org.koitharu.kotatsu.utils.delegates.StringArgumentDelegate

abstract class BaseFragment(
	@LayoutRes contentLayoutId: Int
) : MvpAppCompatFragment(contentLayoutId) {

	protected val coil by inject<ImageLoader>()

	fun stringArg(name: String) = StringArgumentDelegate(name)

	@Deprecated("Use extension", replaceWith = ReplaceWith("parcelableArgument(name)"))
	fun <T : Parcelable> arg(name: String) = ParcelableArgumentDelegate<T>(name)

	open fun getTitle(): CharSequence? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		getTitle()?.let {
			activity?.title = it
		}
	}
}