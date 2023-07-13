package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetDescriptionBinding

class DescriptionSheet : BaseAdaptiveSheet<SheetDescriptionBinding>() {

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetDescriptionBinding {
		return SheetDescriptionBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetDescriptionBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewDescription.text = arguments?.getCharSequence(ARG_CONTENT)
	}

	companion object {

		private const val ARG_CONTENT = "content"
		private const val TAG = "DescriptionSheet"

		fun show(fm: FragmentManager, content: CharSequence) = DescriptionSheet().withArgs(1) {
			putCharSequence(ARG_CONTENT, content)
		}.showDistinct(fm, TAG)
	}
}
