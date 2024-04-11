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
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.databinding.DialogImportBinding
import org.koitharu.kotatsu.local.data.LocalStorageManager
import javax.inject.Inject

@AndroidEntryPoint
class ImportDialogFragment : AlertDialogFragment<DialogImportBinding>(), View.OnClickListener {

	@Inject
	lateinit var storageManager: LocalStorageManager

	private val importFileCall = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
		startImport(it)
	}
	private val importDirCall = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
		startImport(listOfNotNull(it))
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogImportBinding {
		return DialogImportBinding.inflate(inflater, container, false)
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setTitle(R.string._import)
			.setNegativeButton(android.R.string.cancel, null)
			.setCancelable(true)
	}

	override fun onViewBindingCreated(binding: DialogImportBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.buttonDir.setOnClickListener(this)
		binding.buttonFile.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		val res = when (v.id) {
			R.id.button_file -> importFileCall.tryLaunch(arrayOf("*/*"))
			R.id.button_dir -> importDirCall.tryLaunch(null)
			else -> true
		}
		if (!res) {
			Toast.makeText(v.context, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}
	}

	private fun startImport(uris: Collection<Uri>) {
		if (uris.isEmpty()) {
			return
		}
		uris.forEach {
			storageManager.takePermissions(it)
		}
		val ctx = requireContext()
		val msg = if (ImportService.start(ctx, uris)) {
			R.string.import_will_start_soon
		} else {
			R.string.error_occurred
		}
		Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
		dismiss()
	}

	companion object {

		private const val TAG = "ImportDialogFragment"

		fun show(fm: FragmentManager) = ImportDialogFragment().show(fm, TAG)
	}
}
