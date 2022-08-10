package org.koitharu.kotatsu.local.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.databinding.DialogImportBinding

class ImportDialogFragment : AlertDialogFragment<DialogImportBinding>(), View.OnClickListener {

	private val viewModel by activityViewModels<LocalListViewModel>()
	private val importFileCall = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
		startImport(it)
	}
	private val importDirCall = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
		startImport(listOfNotNull(it))
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): DialogImportBinding {
		return DialogImportBinding.inflate(inflater, container, false)
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder) {
		builder.setTitle(R.string._import)
			.setNegativeButton(android.R.string.cancel, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.buttonDir.setOnClickListener(this)
		binding.buttonFile.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_file -> importFileCall.launch(arrayOf("*/*"))
			R.id.button_dir -> importDirCall.launch(null)
		}
	}

	private fun startImport(uris: Collection<Uri>) {
		ImportService.start(requireContext(), uris)
		dismiss()
	}

	companion object {

		private const val TAG = "ImportDialogFragment"

		fun show(fm: FragmentManager) = ImportDialogFragment().show(fm, TAG)
	}
}
