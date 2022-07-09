package org.koitharu.kotatsu.settings.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.databinding.FragmentToolsBinding
import org.koitharu.kotatsu.settings.AppUpdateChecker
import org.koitharu.kotatsu.settings.SettingsActivity

class ToolsFragment : BaseFragment<FragmentToolsBinding>(), View.OnClickListener {

	private var updateChecker: AppUpdateChecker? = null

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentToolsBinding {
		return FragmentToolsBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonSettings.setOnClickListener(this)
		binding.cardUpdate.root.setOnClickListener(this)
		binding.cardUpdate.buttonDownload.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_settings -> startActivity(SettingsActivity.newIntent(v.context))
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
	}

	companion object {

		fun newInstance() = ToolsFragment()
	}
}