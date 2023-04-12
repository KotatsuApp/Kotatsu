package org.koitharu.kotatsu.local.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.databinding.DialogImportBinding
import org.koitharu.kotatsu.settings.backup.BackupDialogFragment
import org.koitharu.kotatsu.settings.backup.RestoreDialogFragment

class ImportDialogFragment : AlertDialogFragment<DialogImportBinding>(), View.OnClickListener {

	private val importFileCall = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
		startImport(it)
	}
	private val importDirCall = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
		startImport(listOfNotNull(it))
	}
	private val backupSelectCall = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		restoreBackup(it)
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): DialogImportBinding {
		return DialogImportBinding.inflate(inflater, container, false)
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setTitle(R.string._import)
			.setNegativeButton(android.R.string.cancel, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonDir.setOnClickListener(this)
		binding.buttonFile.setOnClickListener(this)
		binding.buttonBackup.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_file -> importFileCall.launch(arrayOf("*/*"))
			R.id.button_dir -> importDirCall.launch(null)
			R.id.button_backup -> backupSelectCall.launch(arrayOf("*/*"))
		}
	}

	private fun startImport(uris: Collection<Uri>) {
		if (uris.isEmpty()) {
			return
		}
		val ctx = requireContext()
		ImportWorker.start(ctx, uris)
		Toast.makeText(ctx, R.string.import_will_start_soon, Toast.LENGTH_LONG).show()
		dismiss()
	}

	private fun restoreBackup(uri: Uri?) {
		RestoreDialogFragment.newInstance(uri ?: return)
			.show(parentFragmentManager, BackupDialogFragment.TAG)
		dismiss()
	}

	companion object {

		private const val TAG = "ImportDialogFragment"

		fun show(fm: FragmentManager) = ImportDialogFragment().show(fm, TAG)
	}
}
