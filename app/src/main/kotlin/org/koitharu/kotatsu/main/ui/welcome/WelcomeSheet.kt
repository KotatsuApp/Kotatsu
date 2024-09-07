package org.koitharu.kotatsu.main.ui.welcome

import android.accounts.AccountManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.getDisplayName
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.databinding.SheetWelcomeBinding
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.settings.backup.RestoreDialogFragment
import java.util.Locale

@AndroidEntryPoint
class WelcomeSheet : BaseAdaptiveSheet<SheetWelcomeBinding>(), ChipsView.OnChipClickListener, View.OnClickListener,
	ActivityResultCallback<Uri?> {

	private val viewModel by viewModels<WelcomeViewModel>()

	private val backupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
		this,
	)

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetWelcomeBinding {
		return SheetWelcomeBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetWelcomeBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewWelcomeTitle.isGone = resources.getBoolean(R.bool.is_tablet)
		binding.chipsLocales.onChipClickListener = this
		binding.chipsType.onChipClickListener = this
		binding.chipBackup.setOnClickListener(this)
		binding.chipSync.setOnClickListener(this)

		viewModel.locales.observe(viewLifecycleOwner, ::onLocalesChanged)
		viewModel.types.observe(viewLifecycleOwner, ::onTypesChanged)
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		when (data) {
			is ContentType -> viewModel.setTypeChecked(data, !chip.isChecked)
			is Locale -> viewModel.setLocaleChecked(data, !chip.isChecked)
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.chip_backup -> {
				if (!backupSelectCall.tryLaunch(arrayOf("*/*"))) {
					Snackbar.make(
						v, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
					).show()
				}
			}

			R.id.chip_sync -> {
				val am = AccountManager.get(v.context)
				val accountType = getString(R.string.account_type_sync)
				am.addAccount(accountType, accountType, null, null, requireActivity(), null, null)
			}
		}
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			RestoreDialogFragment.show(parentFragmentManager, result)
		}
	}

	private fun onLocalesChanged(value: FilterProperty<Locale>) {
		val chips = viewBinding?.chipsLocales ?: return
		chips.setChips(
			value.availableItems.map {
				ChipsView.ChipModel(
					title = it.getDisplayName(chips.context),
					isChecked = it in value.selectedItems,
					data = it,
				)
			},
		)
	}

	private fun onTypesChanged(value: FilterProperty<ContentType>) {
		val chips = viewBinding?.chipsType ?: return
		chips.setChips(
			value.availableItems.map {
				ChipsView.ChipModel(
					title = getString(it.titleResId),
					isChecked = it in value.selectedItems,
					data = it,
				)
			},
		)
	}

	companion object {

		private const val TAG = "WelcomeSheet"

		fun show(fm: FragmentManager) = WelcomeSheet().showDistinct(fm, TAG)

		fun dismiss(fm: FragmentManager): Boolean {
			val sheet = fm.findFragmentByTag(TAG) as? WelcomeSheet ?: return false
			sheet.dismissAllowingStateLoss()
			return true
		}
	}
}
