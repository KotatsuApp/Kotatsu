package org.koitharu.kotatsu.settings.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.DialogProgressBinding
import kotlin.math.roundToInt

@AndroidEntryPoint
class RestoreDialogFragment : AlertDialogFragment<DialogProgressBinding>() {

	private val viewModel: RestoreViewModel by viewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogProgressBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: DialogProgressBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewTitle.setText(R.string.restore_backup)
		binding.textViewSubtitle.setText(R.string.preparing_)

		viewModel.progress.observe(viewLifecycleOwner, this::onProgressChanged)
		viewModel.onRestoreDone.observeEvent(viewLifecycleOwner, this::onRestoreDone)
		viewModel.onError.observeEvent(viewLifecycleOwner, this::onError)
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setCancelable(false)
	}

	private fun onError(e: Throwable) {
		MaterialAlertDialogBuilder(context ?: return)
			.setNegativeButton(R.string.close, null)
			.setTitle(R.string.error)
			.setMessage(e.getDisplayMessage(resources))
			.show()
		dismiss()
	}

	private fun onProgressChanged(value: Float) {
		with(requireViewBinding().progressBar) {
			isVisible = true
			val wasIndeterminate = isIndeterminate
			isIndeterminate = value < 0
			if (value >= 0) {
				setProgressCompat((value * max).roundToInt(), !wasIndeterminate)
			}
		}
	}

	private fun onRestoreDone(result: CompositeResult) {
		val builder = MaterialAlertDialogBuilder(context ?: return)
		when {
			result.isEmpty -> {
				builder.setTitle(R.string.data_not_restored)
					.setMessage(R.string.data_not_restored_text)
			}

			result.isAllSuccess -> {
				builder.setTitle(R.string.data_restored)
					.setMessage(R.string.data_restored_success)
			}

			result.isAllFailed -> builder.setTitle(R.string.error)
				.setMessage(
					result.failures.map {
						it.getDisplayMessage(resources)
					}.distinct().joinToString("\n"),
				)

			else -> builder.setTitle(R.string.data_restored)
				.setMessage(R.string.data_restored_with_errors)
		}
		builder.setPositiveButton(android.R.string.ok, null)
			.show()
		dismiss()
	}


	companion object {

		const val ARG_FILE = "file"
		private const val TAG = "RestoreDialogFragment"

		fun show(fm: FragmentManager, uri: Uri) {
			RestoreDialogFragment().withArgs(1) {
				putString(ARG_FILE, uri.toString())
			}.show(fm, TAG)
		}
	}
}
