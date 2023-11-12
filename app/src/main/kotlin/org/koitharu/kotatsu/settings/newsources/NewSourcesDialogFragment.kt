package org.koitharu.kotatsu.settings.newsources

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import coil.ImageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.DialogOnboardBinding
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@AndroidEntryPoint
class NewSourcesDialogFragment :
	AlertDialogFragment<DialogOnboardBinding>(),
	SourceConfigListener,
	DialogInterface.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<NewSourcesViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	): DialogOnboardBinding {
		return DialogOnboardBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogOnboardBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = SourcesSelectAdapter(this, coil, viewLifecycleOwner)
		binding.recyclerView.adapter = adapter
		binding.textViewTitle.setText(R.string.new_sources_text)

		viewModel.content.observe(viewLifecycleOwner) { adapter.items = it }
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setPositiveButton(R.string.done, this)
			.setCancelable(true)
			.setTitle(R.string.remote_sources)
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		dialog.dismiss()
	}

	override fun onItemSettingsClick(item: SourceConfigItem.SourceItem) = Unit

	override fun onItemLiftClick(item: SourceConfigItem.SourceItem) = Unit

	override fun onItemShortcutClick(item: SourceConfigItem.SourceItem) = Unit

	override fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		viewModel.onItemEnabledChanged(item, isEnabled)
	}

	override fun onCloseTip(tip: SourceConfigItem.Tip) = Unit

	companion object {

		private const val TAG = "NewSources"

		fun show(fm: FragmentManager) = NewSourcesDialogFragment().show(fm, TAG)
	}
}
