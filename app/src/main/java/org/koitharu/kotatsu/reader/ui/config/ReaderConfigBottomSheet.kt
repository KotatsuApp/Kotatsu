package org.koitharu.kotatsu.reader.ui.config

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.widgets.CheckableButtonGroup
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.databinding.SheetReaderConfigBinding
import org.koitharu.kotatsu.reader.ui.PageSaveContract
import org.koitharu.kotatsu.reader.ui.ReaderViewModel
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.utils.BottomSheetToolbarController
import org.koitharu.kotatsu.utils.ScreenOrientationHelper
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.ext.withArgs

class ReaderConfigBottomSheet : BaseBottomSheet<SheetReaderConfigBinding>(),
	CheckableButtonGroup.OnCheckedChangeListener,
	ActivityResultCallback<Uri?>, View.OnClickListener {

	private val viewModel by sharedViewModel<ReaderViewModel>()
	private val savePageRequest = registerForActivityResult(PageSaveContract(), this)
	private var orientationHelper: ScreenOrientationHelper? = null
	private lateinit var mode: ReaderMode

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = arguments?.getInt(ARG_MODE)
			?.let { ReaderMode.valueOf(it) }
			?: ReaderMode.STANDARD
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetReaderConfigBinding {
		return SheetReaderConfigBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		observeScreenOrientation()
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		behavior?.addBottomSheetCallback(BottomSheetToolbarController(binding.toolbar))
		if (!resources.getBoolean(R.bool.is_tablet)) {
			binding.toolbar.navigationIcon = null
		}
		binding.buttonStandard.isChecked = mode == ReaderMode.STANDARD
		binding.buttonReversed.isChecked = mode == ReaderMode.REVERSED
		binding.buttonWebtoon.isChecked = mode == ReaderMode.WEBTOON

		binding.checkableGroup.onCheckedChangeListener = this
		binding.buttonSavePage.setOnClickListener(this)
		binding.buttonScreenRotate.setOnClickListener(this)
		binding.buttonSettings.setOnClickListener(this)

	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_settings -> {
				startActivity(SettingsActivity.newReaderSettingsIntent(v.context))
				dismissAllowingStateLoss()
			}
			R.id.button_save_page -> {
				val page = viewModel.getCurrentPage() ?: return
				viewModel.saveCurrentPage(page, savePageRequest)
				dismissAllowingStateLoss()
			}
			R.id.button_screen_rotate -> {
				orientationHelper?.toggleOrientation()
			}
		}
	}

	override fun onCheckedChanged(group: CheckableButtonGroup, checkedId: Int) {
		val newMode = when (checkedId) {
			R.id.button_standard -> ReaderMode.STANDARD
			R.id.button_webtoon -> ReaderMode.WEBTOON
			R.id.button_reversed -> ReaderMode.REVERSED
			else -> return
		}
		if (newMode == mode) {
			return
		}
		findCallback()?.onReaderModeChanged(newMode) ?: return
		mode = newMode
	}

	override fun onActivityResult(uri: Uri?) {
		viewModel.onActivityResult(uri)
	}

	private fun observeScreenOrientation() {
		val helper = ScreenOrientationHelper(requireActivity())
		orientationHelper = helper
		helper.observeAutoOrientation()
			.flowWithLifecycle(lifecycle)
			.onEach {
				binding.buttonScreenRotate.isGone = it
			}.launchIn(viewLifecycleScope)
	}

	private fun findCallback(): Callback? {
		return (parentFragment as? Callback) ?: (activity as? Callback)
	}

	interface Callback {

		fun onReaderModeChanged(mode: ReaderMode)
	}

	companion object {

		private const val TAG = "ReaderConfigBottomSheet"
		private const val ARG_MODE = "mode"

		fun show(fm: FragmentManager, mode: ReaderMode) = ReaderConfigBottomSheet().withArgs(1) {
			putInt(ARG_MODE, mode.id)
		}.show(fm, TAG)
	}
}
