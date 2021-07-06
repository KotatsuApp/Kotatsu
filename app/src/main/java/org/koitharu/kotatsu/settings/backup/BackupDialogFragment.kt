package org.koitharu.kotatsu.settings.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.databinding.DialogProgressBinding
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.progress.Progress
import java.io.File
import java.io.FileOutputStream

class BackupDialogFragment : AlertDialogFragment<DialogProgressBinding>() {

	private val viewModel by viewModel<BackupViewModel>(mode = LazyThreadSafetyMode.NONE)

	private var backup: File? = null
	private val saveFileContract =
		registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
			val file = backup
			if (uri != null && file != null) {
				saveBackup(file, uri)
			} else {
				dismiss()
			}
		}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogProgressBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.textViewTitle.setText(R.string.create_backup)
		binding.textViewSubtitle.setText(R.string.processing_)

		viewModel.progress.observe(viewLifecycleOwner, this::onProgressChanged)
		viewModel.onBackupDone.observe(viewLifecycleOwner, this::onBackupDone)
		viewModel.onError.observe(viewLifecycleOwner, this::onError)
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setCancelable(false)
			.setNegativeButton(android.R.string.cancel, null)
	}

	private fun onError(e: Throwable) {
		AlertDialog.Builder(context ?: return)
			.setNegativeButton(R.string.close, null)
			.setTitle(R.string.error)
			.setMessage(e.getDisplayMessage(resources))
			.show()
		dismiss()
	}

	private fun onProgressChanged(progress: Progress?) {
		with(binding.progressBar) {
			isVisible = true
			isIndeterminate = progress == null
			if (progress != null) {
				this.max = progress.total
				this.progress = progress.value
			}
		}
	}

	private fun onBackupDone(file: File) {
		this.backup = file
		saveFileContract.launch(file.name)
	}

	private fun saveBackup(file: File, output: Uri) {
		try {
			requireContext().contentResolver.openFileDescriptor(output, "w")?.use { fd ->
				FileOutputStream(fd.fileDescriptor).use {
					it.write(file.readBytes())
				}
			}
			Toast.makeText(requireContext(), R.string.backup_saved, Toast.LENGTH_LONG).show()
			dismiss()
		} catch (e: Exception) {
			onError(e)
		}
	}

	companion object {

		const val TAG = "BackupDialogFragment"
	}
}