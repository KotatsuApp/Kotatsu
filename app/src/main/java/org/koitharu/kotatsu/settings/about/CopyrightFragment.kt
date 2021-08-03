package org.koitharu.kotatsu.settings.about

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.updatePadding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.databinding.FragmentCopyrightBinding

class CopyrightFragment : BaseFragment<FragmentCopyrightBinding>() {

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.textView.apply {
			text =
				SpannableStringBuilder(resources.openRawResource(R.raw.copyright).bufferedReader()
					.readText()
					.parseAsHtml(HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST))
			movementMethod = LinkMovementMethod.getInstance()
		}
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentCopyrightBinding.inflate(inflater, container, false)

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.about_copyright)
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

}