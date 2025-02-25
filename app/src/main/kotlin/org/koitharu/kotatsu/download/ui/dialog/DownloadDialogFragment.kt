package org.koitharu.kotatsu.download.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.prefs.DownloadFormat
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.ui.widgets.TwoLinesItemView
import org.koitharu.kotatsu.core.util.ext.findActivity
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getQuantityStringSafe
import org.koitharu.kotatsu.core.util.ext.joinToStringWithLimit
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.databinding.DialogDownloadBinding
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.settings.storage.DirectoryModel

@AndroidEntryPoint
class DownloadDialogFragment : AlertDialogFragment<DialogDownloadBinding>(), View.OnClickListener {

	private val viewModel by viewModels<DownloadDialogViewModel>()
	private var optionViews: Array<out TwoLinesItemView>? = null

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
		DialogDownloadBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setTitle(R.string.save_manga)
			.setCancelable(true)
	}

	override fun onViewBindingCreated(binding: DialogDownloadBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		optionViews = arrayOf(
			binding.optionWholeManga,
			binding.optionWholeBranch,
			binding.optionFirstChapters,
			binding.optionUnreadChapters,
		).onEach {
			it.setOnClickListener(this)
			it.setOnButtonClickListener(this)
		}
		binding.buttonCancel.setOnClickListener(this)
		binding.buttonConfirm.setOnClickListener(this)
		binding.textViewMore.setOnClickListener(this)

		binding.textViewTip.isVisible = viewModel.manga.size == 1
		binding.textViewSummary.text = viewModel.manga.joinToStringWithLimit(binding.root.context, 120) { it.title }

		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.onScheduled.observeEvent(viewLifecycleOwner, this::onDownloadScheduled)
		viewModel.onError.observeEvent(viewLifecycleOwner, this::onError)
		viewModel.defaultFormat.observe(viewLifecycleOwner, this::onDefaultFormatChanged)
		viewModel.availableDestinations.observe(viewLifecycleOwner, this::onDestinationsChanged)
		viewModel.chaptersSelectOptions.observe(viewLifecycleOwner, this::onChapterSelectOptionsChanged)
		viewModel.isOptionsLoading.observe(viewLifecycleOwner, binding.progressBar::showOrHide)
	}

	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)
		showMoreOptions(requireViewBinding().textViewMore.isChecked)
		setCheckedOption(
			savedInstanceState?.getInt(KEY_CHECKED_OPTION, R.id.option_whole_manga) ?: R.id.option_whole_manga,
		)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		optionViews?.find { it.isChecked }?.let {
			outState.putInt(KEY_CHECKED_OPTION, it.id)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		optionViews = null
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> dialog?.cancel()
			R.id.button_confirm -> router.askForDownloadOverMeteredNetwork(::schedule)

			R.id.textView_more -> {
				val binding = viewBinding ?: return
				binding.textViewMore.toggle()
				showMoreOptions(binding.textViewMore.isChecked)
			}

			R.id.button -> when (v.parentView?.id ?: return) {
				R.id.option_whole_branch -> showBranchSelection(v)
				R.id.option_first_chapters -> showFirstChaptersCountSelection(v)
				R.id.option_unread_chapters -> showUnreadChaptersCountSelection(v)
			}

			else -> if (v is TwoLinesItemView) {
				setCheckedOption(v.id)
			}
		}
	}

	private fun schedule(allowMeteredNetwork: Boolean) {
		viewBinding?.run {
			val options = viewModel.chaptersSelectOptions.value
			viewModel.confirm(
				startNow = switchStart.isChecked,
				chaptersMacro = when {
					optionWholeManga.isChecked -> options.wholeManga
					optionWholeBranch.isChecked -> options.wholeBranch ?: return@run
					optionFirstChapters.isChecked -> options.firstChapters ?: return@run
					optionUnreadChapters.isChecked -> options.unreadChapters ?: return@run
					else -> return@run
				},
				format = DownloadFormat.entries.getOrNull(spinnerFormat.selectedItemPosition),
				destination = viewModel.availableDestinations.value.getOrNull(spinnerDestination.selectedItemPosition),
				allowMetered = allowMeteredNetwork,
			)
		}
	}

	private fun onError(e: Throwable) {
		MaterialAlertDialogBuilder(context ?: return)
			.setNegativeButton(R.string.close, null)
			.setTitle(R.string.error)
			.setMessage(e.getDisplayMessage(resources))
			.show()
		dismiss()
	}

	private fun onLoadingStateChanged(value: Boolean) {
		with(requireViewBinding()) {
			buttonConfirm.isEnabled = !value
		}
	}

	private fun onDefaultFormatChanged(format: DownloadFormat?) {
		val spinner = viewBinding?.spinnerFormat ?: return
		spinner.setSelection(format?.ordinal ?: Spinner.INVALID_POSITION)
	}

	private fun onDestinationsChanged(directories: List<DirectoryModel>) {
		viewBinding?.spinnerDestination?.run {
			adapter = DestinationsAdapter(context, directories)
			setSelection(directories.indexOfFirst { it.isChecked })
		}
	}

	private fun onChapterSelectOptionsChanged(options: ChapterSelectOptions) {
		with(viewBinding ?: return) {
			// Whole manga
			optionWholeManga.subtitle = if (options.wholeManga.chaptersCount > 0) {
				resources.getQuantityStringSafe(
					R.plurals.chapters,
					options.wholeManga.chaptersCount,
					options.wholeManga.chaptersCount,
				)
			} else {
				null
			}
			// All chapters for branch
			optionWholeBranch.isVisible = options.wholeBranch != null
			options.wholeBranch?.let {
				optionWholeBranch.title = resources.getString(
					R.string.download_option_all_chapters,
					it.selectedBranch,
				)
				optionWholeBranch.subtitle = if (it.chaptersCount > 0) {
					resources.getQuantityStringSafe(
						R.plurals.chapters,
						it.chaptersCount,
						it.chaptersCount,
					)
				} else {
					null
				}
			}
			// First N chapters
			optionFirstChapters.isVisible = options.firstChapters != null
			options.firstChapters?.let {
				optionFirstChapters.title = resources.getString(
					R.string.download_option_first_n_chapters,
					resources.getQuantityStringSafe(
						R.plurals.chapters,
						it.chaptersCount,
						it.chaptersCount,
					),
				)
				optionFirstChapters.subtitle = it.branch
			}
			// Next N unread chapters
			optionUnreadChapters.isVisible = options.unreadChapters != null
			options.unreadChapters?.let {
				optionUnreadChapters.title = if (it.chaptersCount == Int.MAX_VALUE) {
					resources.getString(R.string.download_option_all_unread)
				} else {
					resources.getString(
						R.string.download_option_next_unread_n_chapters,
						resources.getQuantityStringSafe(
							R.plurals.chapters,
							it.chaptersCount,
							it.chaptersCount,
						),
					)
				}
			}
		}
	}

	private fun onDownloadScheduled(isStarted: Boolean) {
		val bundle = Bundle(1)
		bundle.putBoolean(ARG_STARTED, isStarted)
		setFragmentResult(RESULT_KEY, bundle)
		dismiss()
	}

	private fun showMoreOptions(isVisible: Boolean) = viewBinding?.apply {
		cardFormat.isVisible = isVisible
		textViewFormat.isVisible = isVisible
		cardDestination.isVisible = isVisible
		textViewDestination.isVisible = isVisible
	}

	private fun setCheckedOption(id: Int) {
		for (optionView in optionViews ?: return) {
			optionView.isChecked = id == optionView.id
			optionView.isButtonEnabled = optionView.isChecked
		}
	}

	private fun showBranchSelection(v: View) {
		val option = viewModel.chaptersSelectOptions.value.wholeBranch ?: return
		val branches = option.branches.keys.toList()
		if (branches.size <= 1) {
			return
		}
		val menu = PopupMenu(v.context, v)
		for ((i, branch) in branches.withIndex()) {
			menu.menu.add(Menu.NONE, Menu.NONE, i, branch ?: getString(R.string.unknown))
		}
		menu.setOnMenuItemClickListener {
			viewModel.setSelectedBranch(branches.getOrNull(it.order))
			true
		}
		menu.show()
	}

	private fun showFirstChaptersCountSelection(v: View) {
		val option = viewModel.chaptersSelectOptions.value.firstChapters ?: return
		val menu = PopupMenu(v.context, v)
		chaptersCount(option.maxAvailableCount).forEach { i ->
			menu.menu.add(i.format())
		}
		menu.setOnMenuItemClickListener {
			viewModel.setFirstChaptersCount(
				it.title?.toString()?.toIntOrNull() ?: return@setOnMenuItemClickListener false,
			)
			true
		}
		menu.show()
	}

	private fun showUnreadChaptersCountSelection(v: View) {
		val option = viewModel.chaptersSelectOptions.value.unreadChapters ?: return
		val menu = PopupMenu(v.context, v)
		chaptersCount(option.maxAvailableCount).forEach { i ->
			menu.menu.add(i.format())
		}
		menu.menu.add(getString(R.string.chapters_all))
		menu.setOnMenuItemClickListener {
			viewModel.setUnreadChaptersCount(it.title?.toString()?.toIntOrNull() ?: Int.MAX_VALUE)
			true
		}
		menu.show()
	}

	private fun chaptersCount(max: Int) = sequence {
		yield(1)
		var seed = 5
		var step = 5
		while (seed + step <= max) {
			yield(seed)
			step = when {
				seed < 20 -> 5
				seed < 60 -> 10
				else -> 20
			}
			seed += step
		}
		if (seed < max) {
			yield(max)
		}
	}

	private class SnackbarResultListener(
		private val host: View,
	) : FragmentResultListener {

		override fun onFragmentResult(requestKey: String, result: Bundle) {
			val isStarted = result.getBoolean(ARG_STARTED, true)
			val snackbar = Snackbar.make(
				host,
				if (isStarted) R.string.download_started else R.string.download_added,
				Snackbar.LENGTH_LONG,
			)
			(host.context.findActivity() as? BottomNavOwner)?.let {
				snackbar.anchorView = it.bottomNav
			}
			val router = AppRouter.from(host)
			if (router != null) {
				snackbar.setAction(R.string.details) { router.openDownloads() }
			}
			snackbar.show()
		}
	}

	companion object {

		private const val RESULT_KEY = "DOWNLOAD_STARTED"
		private const val ARG_STARTED = "started"
		private const val KEY_CHECKED_OPTION = "checked_opt"

		fun registerCallback(
			fm: FragmentManager,
			lifecycleOwner: LifecycleOwner,
			snackbarHost: View
		) = fm.setFragmentResultListener(RESULT_KEY, lifecycleOwner, SnackbarResultListener(snackbarHost))

		fun unregisterCallback(fm: FragmentManager) = fm.clearFragmentResultListener(RESULT_KEY)
	}
}
