package org.koitharu.kotatsu.sync.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.databinding.PreferenceDialogAutocompletetextviewBinding
import org.koitharu.kotatsu.settings.utils.validation.DomainValidator
import org.koitharu.kotatsu.sync.data.SyncSettings
import javax.inject.Inject

@AndroidEntryPoint
class SyncHostDialogFragment : AlertDialogFragment<PreferenceDialogAutocompletetextviewBinding>(),
	DialogInterface.OnClickListener {

	@Inject
	lateinit var syncSettings: SyncSettings

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = PreferenceDialogAutocompletetextviewBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setPositiveButton(android.R.string.ok, this)
			.setNegativeButton(android.R.string.cancel, this)
			.setCancelable(false)
			.setTitle(R.string.server_address)
	}

	override fun onViewBindingCreated(
		binding: PreferenceDialogAutocompletetextviewBinding,
		savedInstanceState: Bundle?
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.message.updateLayoutParams<MarginLayoutParams> {
			topMargin = binding.root.resources.getDimensionPixelOffset(R.dimen.screen_padding)
			bottomMargin = topMargin
		}
		binding.message.setText(R.string.sync_host_description)
		val entries = binding.root.resources.getStringArray(R.array.sync_host_list)
		val editText = binding.edit
		editText.setText(syncSettings.host)
		editText.threshold = 0
		editText.setAdapter(ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_dropdown_item, entries))
		binding.dropdown.setOnClickListener {
			editText.showDropDown()
		}
		DomainValidator().attachToEditText(editText)
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		when (which) {
			DialogInterface.BUTTON_POSITIVE -> {
				val result = requireViewBinding().edit.text?.toString().orEmpty()
				syncSettings.host = result
				parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(KEY_HOST to result))
			}
		}
		dialog.dismiss()
	}

	companion object {

		private const val TAG = "SyncHostDialogFragment"
		const val REQUEST_KEY = "sync_host"
		const val KEY_HOST = "host"

		fun show(fm: FragmentManager) = SyncHostDialogFragment().show(fm, TAG)
	}
}
