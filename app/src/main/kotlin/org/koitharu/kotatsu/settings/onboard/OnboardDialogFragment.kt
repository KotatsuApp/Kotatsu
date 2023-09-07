package org.koitharu.kotatsu.settings.onboard

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showAllowStateLoss
import org.koitharu.kotatsu.databinding.DialogOnboardBinding
import org.koitharu.kotatsu.settings.onboard.adapter.SourceLocaleListener
import org.koitharu.kotatsu.settings.onboard.adapter.SourceLocalesAdapter
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

@AndroidEntryPoint
class OnboardDialogFragment :
	AlertDialogFragment<DialogOnboardBinding>(),
	DialogInterface.OnClickListener, SourceLocaleListener {

	private val viewModel by viewModels<OnboardViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogOnboardBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		super.onBuildDialog(builder)
			.setPositiveButton(R.string.done, this)
			.setCancelable(false)
		builder.setTitle(R.string.welcome)
		return builder
	}

	override fun onViewBindingCreated(binding: DialogOnboardBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = SourceLocalesAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.textViewTitle.setText(R.string.onboard_text)
		viewModel.list.observe(viewLifecycleOwner, adapter)
	}

	override fun onItemCheckedChanged(item: SourceLocale, isChecked: Boolean) {
		viewModel.setItemChecked(item.key, isChecked)
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		when (which) {
			DialogInterface.BUTTON_POSITIVE -> dialog.dismiss()
		}
	}

	companion object {

		private const val TAG = "OnboardDialog"

		fun show(fm: FragmentManager) = OnboardDialogFragment().showAllowStateLoss(fm, TAG)
	}
}
