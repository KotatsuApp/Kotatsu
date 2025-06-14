package org.koitharu.kotatsu.backups.ui.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.progress.Progress
import org.koitharu.kotatsu.databinding.DialogProgressBinding

@AndroidEntryPoint
class BackupDialogFragment : AlertDialogFragment<DialogProgressBinding>() {

	private val viewModel by viewModels<BackupViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogProgressBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: DialogProgressBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewTitle.setText(R.string.create_backup)
		binding.textViewSubtitle.setText(R.string.processing_)

		viewModel.progress.observe(viewLifecycleOwner, this::onProgressChanged)
		viewModel.onBackupDone.observeEvent(viewLifecycleOwner, this::onBackupDone)
		viewModel.onError.observeEvent(viewLifecycleOwner, this::onError)
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setCancelable(false)
			.setNegativeButton(android.R.string.cancel, null)
	}

	private fun onError(e: Throwable) {
		MaterialAlertDialogBuilder(context ?: return)
			.setNegativeButton(R.string.close, null)
			.setTitle(R.string.error)
			.setMessage(e.getDisplayMessage(resources))
			.show()
		dismiss()
	}

	private fun onProgressChanged(value: Progress) {
		with(requireViewBinding().progressBar) {
			isVisible = true
			val wasIndeterminate = isIndeterminate
			isIndeterminate = value.isIndeterminate
			if (!value.isIndeterminate) {
				max = value.total
				setProgressCompat(value.progress, !wasIndeterminate)
			}
		}
	}

	private fun onBackupDone(uri: Uri) {
		Toast.makeText(requireContext(), R.string.backup_saved, Toast.LENGTH_SHORT).show()
		dismiss()
	}
}
