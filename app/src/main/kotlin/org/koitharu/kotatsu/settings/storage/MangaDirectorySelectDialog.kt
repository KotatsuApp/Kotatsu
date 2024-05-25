package org.koitharu.kotatsu.settings.storage

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ToastErrorObserver
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.databinding.DialogDirectorySelectBinding

@AndroidEntryPoint
class MangaDirectorySelectDialog : AlertDialogFragment<DialogDirectorySelectBinding>(),
	OnListItemClickListener<DirectoryModel> {

	private val viewModel: MangaDirectorySelectViewModel by viewModels()
	private val pickFileTreeLauncher = registerForActivityResult(PickDirectoryContract()) {
		if (it != null) viewModel.onCustomDirectoryPicked(it)
	}
	private val permissionRequestLauncher = registerForActivityResult(
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			RequestStorageManagerPermissionContract()
		} else {
			ActivityResultContracts.RequestPermission()
		},
	) {
		if (it) {
			viewModel.refresh()
			if (!pickFileTreeLauncher.tryLaunch(null)) {
				Toast.makeText(
					context ?: return@registerForActivityResult,
					R.string.operation_not_supported,
					Toast.LENGTH_SHORT,
				).show()
			}
		}
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogDirectorySelectBinding {
		return DialogDirectorySelectBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogDirectorySelectBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = AsyncListDifferDelegationAdapter(DirectoryDiffCallback(), directoryAD(this))
		binding.root.adapter = adapter
		viewModel.items.observe(viewLifecycleOwner) { adapter.items = it }
		viewModel.onDismissDialog.observeEvent(viewLifecycleOwner) { dismiss() }
		viewModel.onPickDirectory.observeEvent(viewLifecycleOwner) { pickCustomDirectory() }
		viewModel.onError.observeEvent(viewLifecycleOwner, ToastErrorObserver(binding.root, this))
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setCancelable(true)
			.setTitle(R.string.manga_save_location)
			.setNegativeButton(android.R.string.cancel, null)
	}

	override fun onItemClick(item: DirectoryModel, view: View) {
		viewModel.onItemClick(item)
	}

	private fun pickCustomDirectory() {
		if (!permissionRequestLauncher.tryLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			Toast.makeText(context ?: return, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}
	}

	companion object {

		private const val TAG = "MangaDirectorySelectDialog"

		fun show(fm: FragmentManager) = MangaDirectorySelectDialog()
			.showDistinct(fm, TAG)
	}
}
