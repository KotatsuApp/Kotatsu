package org.koitharu.kotatsu.settings.onboard

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.DialogOnboardBinding
import org.koitharu.kotatsu.settings.onboard.adapter.SourceLocalesAdapter
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale
import org.koitharu.kotatsu.utils.ext.observeNotNull
import org.koitharu.kotatsu.utils.ext.showAllowStateLoss
import org.koitharu.kotatsu.utils.ext.withArgs

class OnboardDialogFragment :
	AlertDialogFragment<DialogOnboardBinding>(),
	OnListItemClickListener<SourceLocale>,
	DialogInterface.OnClickListener {

	private val viewModel by viewModel<OnboardViewModel>()
	private var isWelcome: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.run {
			isWelcome = getBoolean(ARG_WELCOME, false)
		}
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogOnboardBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder) {
		builder
			.setPositiveButton(R.string.done, this)
			.setCancelable(true)
		if (isWelcome) {
			builder.setTitle(R.string.welcome)
		} else {
			builder
				.setTitle(R.string.remote_sources)
				.setNegativeButton(android.R.string.cancel, this)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = SourceLocalesAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.textViewTitle.setText(R.string.onboard_text)
		viewModel.list.observeNotNull(viewLifecycleOwner) {
			adapter.items = it
		}
	}

	override fun onItemClick(item: SourceLocale, view: View) {
		viewModel.setItemChecked(item.key, !item.isChecked)
	}

	override fun onClick(dialog: DialogInterface?, which: Int) {
		when (which) {
			DialogInterface.BUTTON_POSITIVE -> viewModel.apply()
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