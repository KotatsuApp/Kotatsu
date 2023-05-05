package org.koitharu.kotatsu.settings.tools

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.databinding.FragmentToolsBinding
import org.koitharu.kotatsu.download.ui.list.DownloadsActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.about.AppUpdateDialog

@AndroidEntryPoint
class ToolsFragment :
	BaseFragment<FragmentToolsBinding>(),
	CompoundButton.OnCheckedChangeListener,
	View.OnClickListener {

	private val viewModel by viewModels<ToolsViewModel>()

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentToolsBinding {
		return FragmentToolsBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonSettings.setOnClickListener(this)
		binding.buttonDownloads.setOnClickListener(this)
		binding.cardUpdate.buttonChangelog.setOnClickListener(this)
		binding.cardUpdate.buttonDownload.setOnClickListener(this)
		binding.switchIncognito.setOnCheckedChangeListener(this)
		binding.memoryUsageView.setManageButtonOnClickListener(this)

		viewModel.isIncognitoModeEnabled.observe(viewLifecycleOwner) {
			binding.switchIncognito.isChecked = it
		}
		viewModel.storageUsage.observe(viewLifecycleOwner) {
			binding.memoryUsageView.bind(it)
		}
		viewModel.appUpdate.observe(viewLifecycleOwner, ::onAppUpdateAvailable)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_settings -> startActivity(SettingsActivity.newIntent(v.context))
			R.id.button_manage -> startActivity(SettingsActivity.newHistorySettingsIntent(v.context))
			R.id.button_downloads -> startActivity(DownloadsActivity.newIntent(v.context))
			R.id.button_download -> {
				val url = viewModel.appUpdate.value?.apkUrl ?: return
				val intent = Intent(Intent.ACTION_VIEW)
				intent.data = url.toUri()
				startActivity(Intent.createChooser(intent, getString(R.string.open_in_browser)))
			}

			R.id.button_changelog -> {
				val version = viewModel.appUpdate.value ?: return
				AppUpdateDialog(v.context).show(version)
			}
		}
	}

	override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
		viewModel.toggleIncognitoMode(isChecked)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			bottom = insets.bottom,
		)
	}

	private fun onAppUpdateAvailable(version: AppVersion?) {
		if (version == null) {
			binding.cardUpdate.root.isVisible = false
			return
		}
		binding.cardUpdate.textSecondary.text = getString(R.string.new_version_s, version.name)
		binding.cardUpdate.root.isVisible = true
	}

	companion object {

		fun newInstance() = ToolsFragment()
	}
}
