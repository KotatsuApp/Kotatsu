package org.koitharu.kotatsu.base.ui

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import coil.ImageLoader
import org.koin.android.ext.android.inject

abstract class BaseFragment(
	@LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

	protected val coil by inject<ImageLoader>()

	open fun getTitle(): CharSequence? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		getTitle()?.let {
			activity?.title = it
		}
	}
}