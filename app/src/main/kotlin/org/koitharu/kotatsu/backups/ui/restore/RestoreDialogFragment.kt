package org.koitharu.kotatsu.backups.ui.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.DialogRestoreBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class RestoreDialogFragment : AlertDialogFragment<DialogRestoreBinding>(), OnListItemClickListener<BackupSectionModel>,
	View.OnClickListener {

	private val viewModel: RestoreViewModel by viewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogRestoreBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: DialogRestoreBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = BackupSectionsAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.buttonCancel.setOnClickListener(this)
		binding.buttonRestore.setOnClickListener(this)
		viewModel.availableEntries.observe(viewLifecycleOwner, adapter)
		viewModel.onError.observeEvent(viewLifecycleOwner, this::onError)
		combine(
			viewModel.isLoading,
			viewModel.availableEntries,
			viewModel.backupDate,
			::Triple,
		).observe(viewLifecycleOwner, this::onLoadingChanged)
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setTitle(R.string.restore_backup)
			.setCancelable(false)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> dismiss()
			R.id.button_restore -> {
				if (startRestoreService()) {
					Toast.makeText(v.context, R.string.backup_restored_background, Toast.LENGTH_SHORT).show()
					router.closeWelcomeSheet()
					dismiss()
				} else {
					Toast.makeText(v.context, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	override fun onItemClick(item: BackupSectionModel, view: View) {
		viewModel.onItemClick(item)
	}

	private fun onLoadingChanged(value: Triple<Boolean, List<BackupSectionModel>, Date?>) {
		val (isLoading, entries, backupDate) = value
		val hasEntries = entries.isNotEmpty()
		with(requireViewBinding()) {
			progressBar.isVisible = isLoading
			recyclerView.isGone = isLoading
			textViewSubtitle.textAndVisible =
				when {
					!isLoading -> backupDate?.formatBackupDate()
					hasEntries -> getString(R.string.processing_)
					else -> getString(R.string.loading_)
				}
			buttonRestore.isEnabled = !isLoading && entries.any { it.isChecked }
		}
	}

	private fun startRestoreService(): Boolean {
		return RestoreService.start(
			context ?: return false,
			viewModel.uri ?: return false,
			viewModel.getCheckedSections(),
		)
	}

	private fun Date.formatBackupDate(): String {
		return getString(
			R.string.backup_date_,
			SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this),
		)
	}

	private fun onError(e: Throwable) {
		MaterialAlertDialogBuilder(context ?: return)
			.setNegativeButton(R.string.close, null)
			.setTitle(R.string.error)
			.setMessage(e.getDisplayMessage(resources))
			.show()
		dismiss()
	}
}
