package org.koitharu.kotatsu.settings.newsources

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.databinding.DialogOnboardBinding
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

@AndroidEntryPoint
class NewSourcesDialogFragment :
	AlertDialogFragment<DialogOnboardBinding>(),
	SourceConfigListener,
	DialogInterface.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<NewSourcesViewModel>()

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): DialogOnboardBinding {
		return DialogOnboardBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = SourceConfigAdapter(this, coil, viewLifecycleOwner)
		binding.recyclerView.adapter = adapter
		binding.textViewTitle.setText(R.string.new_sources_text)

		viewModel.sources.observe(viewLifecycleOwner) { adapter.items = it }
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder) {
		builder
			.setPositiveButton(R.string.done, this)
			.setCancelable(true)
			.setTitle(R.string.remote_sources)
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		viewModel.apply()
		dialog.dismiss()
	}

	override fun onItemSettingsClick(item: SourceConfigItem.SourceItem) = Unit

	override fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		viewModel.onItemEnabledChanged(item, isEnabled)
	}

	override fun onDragHandleTouch(holder: RecyclerView.ViewHolder) = Unit

	override fun onHeaderClick(header: SourceConfigItem.LocaleGroup) = Unit

	companion object {

		private const val TAG = "NewSources"

		fun show(fm: FragmentManager) = NewSourcesDialogFragment().show(fm, TAG)
	}
}
