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
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.DialogOnboardBinding
import org.koitharu.kotatsu.settings.onboard.adapter.SourceLocaleListener
import org.koitharu.kotatsu.settings.onboard.adapter.SourceLocalesAdapter
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

@AndroidEntryPoint
class OnboardDialogFragment :
	AlertDialogFragment<DialogOnboardBinding>(),
	DialogInterface.OnClickListener, SourceLocaleListener {

	private val viewModel by viewModels<OnboardViewModel>()
	private var isWelcome: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.run {
			isWelcome = getBoolean(ARG_WELCOME, false)
		}
	}

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogOnboardBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		super.onBuildDialog(builder)
			.setPositiveButton(R.string.done, this)
			.setCancelable(false)
		if (isWelcome) {
			builder.setTitle(R.string.welcome)
		} else {
			builder
				.setTitle(R.string.remote_sources)
				.setNegativeButton(android.R.string.cancel, this)
		}
		return builder
	}

	override fun onViewBindingCreated(binding: DialogOnboardBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = SourceLocalesAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.textViewTitle.setText(R.string.onboard_text)
		viewModel.list.observe(viewLifecycleOwner) {
			adapter.items = it.orEmpty()
		}
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
		private const val ARG_WELCOME = "welcome"

		fun show(fm: FragmentManager) = OnboardDialogFragment().show(fm, TAG)

		fun showWelcome(fm: FragmentManager) {
			OnboardDialogFragment().withArgs(1) {
				putBoolean(ARG_WELCOME, true)
			}.showAllowStateLoss(fm, TAG)
		}
	}
}
