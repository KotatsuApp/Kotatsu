package org.koitharu.kotatsu.ui.base

import android.content.Context
import androidx.annotation.LayoutRes
import coil.ImageLoader
import moxy.MvpAppCompatFragment
import org.koin.android.ext.android.inject

abstract class BaseFragment(
	@LayoutRes contentLayoutId: Int
) : MvpAppCompatFragment(contentLayoutId) {

	protected val coil by inject<ImageLoader>()

	open fun getTitle(): CharSequence? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		getTitle()?.let {
			activity?.title = it
		}
	}
}