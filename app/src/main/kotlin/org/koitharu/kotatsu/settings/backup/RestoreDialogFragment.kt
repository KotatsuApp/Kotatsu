package org.koitharu.kotatsu.settings.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.DialogRestoreBinding
import org.koitharu.kotatsu.main.ui.welcome.WelcomeSheet
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

@AndroidEntryPoint
class RestoreDialogFragment : AlertDialogFragment<DialogRestoreBinding>(), OnListItemClickListener<BackupEntryModel>,
	View.OnClickListener {

	private val viewModel: RestoreViewModel by viewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogRestoreBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: DialogRestoreBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = BackupEntriesAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.buttonCancel.setOnClickListener(this)
		binding.buttonRestore.setOnClickListener(this)
		viewModel.availableEntries.observe(viewLifecycleOwner, adapter)
		viewModel.progress.observe(viewLifecycleOwner, this::onProgressChanged)
		viewModel.onRestoreDone.observeEvent(viewLifecycleOwner, this::onRestoreDone)
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
			R.id.button_restore -> viewModel.restore()
		}
	}

	override fun onItemClick(item: BackupEntryModel, view: View) {
		viewModel.onItemClick(item)
	}

	private fun onLoadingChanged(value: Triple<Boolean, List<BackupEntryModel>, Date?>) {
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
		if (!result.isEmpty && !result.isAllFailed) {
			WelcomeSheet.dismiss(parentFragmentManager)
		}
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
