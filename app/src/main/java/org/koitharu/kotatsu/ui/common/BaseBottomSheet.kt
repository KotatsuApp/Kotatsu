package org.koitharu.kotatsu.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import moxy.MvpBottomSheetDialogFragment

abstract class BaseBottomSheet(@LayoutRes private val layoutResId: Int) : MvpBottomSheetDialogFragment() {

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? = inflater.inflate(layoutResId, container, false)
}